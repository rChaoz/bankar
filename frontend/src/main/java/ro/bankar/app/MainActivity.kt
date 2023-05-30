package ro.bankar.app

import android.content.res.Configuration
import android.os.Bundle
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.FragmentActivity
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import ro.bankar.app.data.EmptyRepository
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.repository
import ro.bankar.app.ui.LockScreen
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.mainNavigation
import ro.bankar.app.ui.main.settings.Language
import ro.bankar.app.ui.main.settings.Theme
import ro.bankar.app.ui.newuser.NewUserNav
import ro.bankar.app.ui.newuser.newUserNavigation
import ro.bankar.app.ui.theme.AppTheme
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

// For logging
const val TAG = "BanKAR"

data class ThemeMode(val isDarkMode: Boolean, val toggleThemeMode: () -> Unit)

val LocalThemeMode = compositionLocalOf { ThemeMode(false) {} }

class MainActivity : FragmentActivity() {
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
    val systemDark = isSystemInDarkTheme()
    val initialDarkMode = initialPrefs[KeyTheme]?.let { it == Theme.Dark.ordinal } ?: systemDark
    val darkMode by dataStore.mapCollectPreferenceAsState(key = KeyTheme, defaultValue = initialDarkMode) {
        when (it) {
            Theme.Light.ordinal -> false
            Theme.Dark.ordinal -> true
            else -> systemDark
        }
    }

    // Allow programmatically changing language
    val languages = remember { Language.values() }
    val language by remember { dataStore.data.map {
            data -> data[KeyLanguage]?.let { languages[it.coerceIn(languages.indices)] } ?: Language.SystemDefault
    } }.collectAsState(initial = initialPrefs[KeyLanguage]?.let { languages[it.coerceIn(languages.indices)] } ?: Language.SystemDefault)

    val context = LocalContext.current
    val languageContext by remember {
        derivedStateOf {
            if (language == Language.SystemDefault) context
            else context.createConfigurationContext(Configuration().apply {
                setLocale(Locale(language.code!!))
            })
        }
    }

    AppTheme(useDarkTheme = darkMode) {
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode(darkMode) {
                scope.launch { dataStore.setPreference(KeyTheme, if (darkMode) Theme.Light.ordinal else Theme.Dark.ordinal) }
            },
            LocalDataStore provides dataStore,
            LocalContext provides languageContext
        ) {
            // Setup navigation
            val controller = rememberAnimatedNavController()

            // Server data repository
            val sessionToken by dataStore.collectPreferenceAsState(KeyUserSession, defaultValue = initialPrefs[KeyUserSession])
            val repository = remember(sessionToken) {
                sessionToken?.let {
                    repository(lifecycleScope, it) {
                        // Erase session token
                        lifecycleScope.launch { dataStore.removePreference(KeyUserSession) }
                        // Ensure that, if multiple calls attempt to navigate to NewUser simultaneously, we only navigate once
                        val current = controller.currentBackStackEntry?.destination?.route ?: return@repository
                        if (current in listOf(NewUserNav.route, NewUserNav.Welcome.route, NewUserNav.SignIn.route, NewUserNav.SignUp.route))
                            controller.navigate(NewUserNav.route) {
                                popUpTo(Nav.Main.route) { inclusive = true }
                            }
                    }
                } ?: EmptyRepository
            }
            // App should start in locked state
            LaunchedEffect(true) {
                if (sessionToken != null) controller.navigate(Nav.Lock.route)
            }

            // Open web socket
            if (sessionToken != null) LaunchedEffect(repository) {
                repository.openAndMaintainSocket()
            }

            // Track user inactivity
            var lastActiveAt by remember { mutableStateOf(Clock.System.now()) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) lastActiveAt = Clock.System.now()
                    else if (event == Lifecycle.Event.ON_START && (Clock.System.now() - lastActiveAt) > 1.minutes) {
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
                    startDestination = if (initialPrefs[KeyUserSession] == null) Nav.NewUser.route else Nav.Main.route,
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