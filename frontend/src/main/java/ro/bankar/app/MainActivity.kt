package ro.bankar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import ro.bankar.app.data.EmptyRepository
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.repository
import ro.bankar.app.ui.LockScreen
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.mainNavigation
import ro.bankar.app.ui.newuser.NewUserNav
import ro.bankar.app.ui.newuser.newUserNavigation
import ro.bankar.app.ui.theme.AppTheme
import kotlin.time.Duration.Companion.seconds

// For logging
const val TAG = "BanKAR"

data class ThemeMode(val isDarkMode: Boolean, val toggleThemeMode: () -> Unit)

val LocalThemeMode = compositionLocalOf { ThemeMode(false) {} }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Main(dataStore, lifecycleScope) }
    }
}

enum class Nav(val route: String) {
    Lock("lock"),

    NewUser(NewUserNav.route), Main(MainNav.route), Verification("verification");
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Main(dataStore: DataStore<Preferences>, lifecycleScope: CoroutineScope) {
    val scope = rememberCoroutineScope()
    val initialPrefs = remember { runBlocking { dataStore.data.first() } }

    // This is to prevent theme flickering on startup
    val initialDarkMode = initialPrefs[IS_DARK_MODE] ?: isSystemInDarkTheme()
    val isDarkTheme by dataStore.collectPreferenceAsState(IS_DARK_MODE, initialDarkMode)

    AppTheme(useDarkTheme = isDarkTheme) {
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode(isDarkTheme) {
                scope.launch { dataStore.setPreference(IS_DARK_MODE, !isDarkTheme) }
            },
            LocalDataStore provides dataStore,
        ) {
            // Setup navigation
            val controller = rememberAnimatedNavController()

            // Server data repository
            val sessionToken by dataStore.collectPreferenceAsState(USER_SESSION, defaultValue = initialPrefs[USER_SESSION])
            val repository = remember(sessionToken) {
                sessionToken?.let {
                    repository(lifecycleScope, it) {
                        val stack = controller.currentBackStack.value
                        // Ensure that, if multiple calls attempt to navigate to NewUser simultaneously, we only navigate once
                        if (stack.size < 2 || stack[1].destination.route == Nav.Main.route)
                            controller.navigate(NewUserNav.route) {
                                popUpTo(Nav.Main.route) { inclusive = true }
                            }
                    }
                } ?: EmptyRepository
            }

            // Open web socket
            LaunchedEffect(true) {
                lifecycleScope.launch {
                    repository.openAndMaintainSocket()
                }
            }

            // Track user inactivity
            var lastActiveAt by remember { mutableStateOf(Clock.System.now()) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) lastActiveAt = Clock.System.now()
                    // TODO Increase timer from 10 seconds to 1-2 minutes
                    else if (event == Lifecycle.Event.ON_START && (Clock.System.now() - lastActiveAt) > 10.seconds) {
                        if (sessionToken != null) controller.navigate(Nav.Lock.route)
                    }
                }
                val lifecycle = lifecycleOwner.lifecycle
                lifecycle.addObserver(observer)
                onDispose {
                    lifecycle.removeObserver(observer)
                }
            }

            CompositionLocalProvider(LocalRepository provides repository) {
                AnimatedNavHost(
                    controller,
                    startDestination = if (initialPrefs[USER_SESSION] == null) Nav.NewUser.route else Nav.Main.route,
                    enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) { it / 2 } + fadeIn(spring()) },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) { it / 2 } + fadeOut(spring()) },
                ) {
                    navigation(controller)
                }
            }
        }
    }
}


@OptIn(ExperimentalAnimationApi::class)
private fun NavGraphBuilder.navigation(controller: NavHostController) {
    composable(Nav.Lock.route) {
        LockScreen(onUnlock = { controller.popBackStack() })
    }
    newUserNavigation(controller, onSuccess = {
        controller.navigate(Nav.Main.route) {
            popUpTo(Nav.NewUser.route) { inclusive = true }
        }
    })
    mainNavigation(controller)
}