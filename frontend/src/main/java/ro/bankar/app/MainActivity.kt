package ro.bankar.app

import android.content.Context
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
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import ro.bankar.app.data.Cache
import ro.bankar.app.data.EmptyRepository
import ro.bankar.app.data.KeyLanguage
import ro.bankar.app.data.KeyTheme
import ro.bankar.app.data.KeyUserSession
import ro.bankar.app.data.LocalDataStore
import ro.bankar.app.data.LocalRepository
import ro.bankar.app.data.cache
import ro.bankar.app.data.collectPreferenceAsState
import ro.bankar.app.data.dataStore
import ro.bankar.app.data.mapCollectPreferenceAsState
import ro.bankar.app.data.removePreference
import ro.bankar.app.data.repository
import ro.bankar.app.data.setPreference
import ro.bankar.app.ui.LockScreen
import ro.bankar.app.ui.main.MainNav
import ro.bankar.app.ui.main.mainNavigation
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

        // Re-create activity if language changes
        val currentLanguage = runBlocking { dataStore.data.first()[KeyLanguage] }
        lifecycleScope.launch {
            dataStore.data.map { it[KeyLanguage] }.filter { it != currentLanguage }.collect { recreate() }
        }

        setContent {
            Main(dataStore, cache, lifecycleScope)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        var context = newBase
        val language = runBlocking { context.dataStore.data.first()[KeyLanguage] }

        // Apply language changes
        if (language != null) {
            val locale = Locale(language)
            context = context.createConfigurationContext(Configuration().apply { setLocale(locale) })
            Locale.setDefault(locale)
        }

        super.attachBaseContext(context)
    }
}

enum class Nav(val route: String) {
    Lock("lock"),

    NewUser(NewUserNav.route), Main(MainNav.route), Verification("verification");
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun Main(dataStore: DataStore<Preferences>, cache: DataStore<Cache>, lifecycleScope: CoroutineScope) {
    val scope = rememberCoroutineScope()
    val initialPrefs = remember { runBlocking { dataStore.data.first() } }

    // This is to prevent theme flickering on startup
    val systemDark = isSystemInDarkTheme()
    val initialDarkMode = initialPrefs[KeyTheme]?.let { it == Theme.Dark.ordinal } ?: systemDark
    val darkMode by dataStore.mapCollectPreferenceAsState(key = KeyTheme, initial = initialDarkMode) {
        when (it) {
            Theme.Light.ordinal -> false
            Theme.Dark.ordinal -> true
            else -> systemDark
        }
    }

    AppTheme(useDarkTheme = darkMode) {
        CompositionLocalProvider(
            LocalThemeMode provides ThemeMode(darkMode) {
                scope.launch { dataStore.setPreference(KeyTheme, if (darkMode) Theme.Light.ordinal else Theme.Dark.ordinal) }
            },
            LocalDataStore provides dataStore,
        ) {
            // Setup navigation
            val controller = rememberAnimatedNavController()

            // Server data repository
            val sessionToken by dataStore.collectPreferenceAsState(KeyUserSession, initial = initialPrefs[KeyUserSession])

            // Create repository object, which should be cancelled when the session token changes (e.g. is deleted - logout)
            class RepositoryRememberObserver(sessionToken: String?) : RememberObserver {
                private val repositoryScope = lifecycleScope.coroutineContext.let { CoroutineScope(it + Job(it.job)) }
                val repository = if (sessionToken != null) repository(repositoryScope, sessionToken, cache) {
                    // On auto logout (session expired)
                    // Erase session token
                    lifecycleScope.launch { dataStore.removePreference(KeyUserSession) }
                    // Ensure that, if multiple calls attempt to navigate to NewUser simultaneously, we only navigate once
                    val current = controller.currentBackStackEntry?.destination?.route ?: return@repository
                    if (current !in listOf(NewUserNav.route, NewUserNav.Welcome.route, NewUserNav.SignIn.route, NewUserNav.SignUp.route))
                        controller.navigate(NewUserNav.route) {
                            popUpTo(Nav.Main.route) { inclusive = true }
                        }
                } else EmptyRepository

                override fun onRemembered() {}

                override fun onAbandoned() {
                    repositoryScope.cancel()
                }

                override fun onForgotten() {
                    repositoryScope.cancel()
                }
            }

            val repository = remember(sessionToken) { RepositoryRememberObserver(sessionToken) }.repository

            // App should start in locked state. However, if the activity is re-created, we should not lock it again
            var firstStart by rememberSaveable { mutableStateOf(true) }
            LaunchedEffect(true) {
                if (sessionToken != null && firstStart) {
                    controller.navigate(Nav.Lock.route)
                    firstStart = false
                }
            }

            // Open web socket
            if (sessionToken != null) LaunchedEffect(repository) {
                repository.openAndMaintainSocket()
            }

            // Track user inactivity
            var lastActiveAt by rememberSaveable(saver = Saver(
                save = { it.value.toEpochMilliseconds() },
                restore = { mutableStateOf(Instant.fromEpochMilliseconds(it)) }
            )) { mutableStateOf(Clock.System.now()) }
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