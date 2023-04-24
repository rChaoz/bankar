package ro.bankar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ro.bankar.app.ui.LoginScreen
import ro.bankar.app.ui.theme.AppTheme

data class ThemeMode(val isDarkMode: Boolean, val toggleThemeMode: () -> Unit)

val LocalThemeMode = compositionLocalOf { ThemeMode(false) {} }
val LocalDataStore = compositionLocalOf<DataStore<Preferences>?> { null }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get initial preferences TODO check if user is logged in etc.
        val initialPrefs = runBlocking { dataStore.data.first() }
        setContent {
            val ioScope = rememberCoroutineScope { Dispatchers.IO }

            val isSystemDarkMode = isSystemInDarkTheme()
            val isDarkTheme by remember { dataStore.data.map { it[IS_DARK_MODE] ?: isSystemDarkMode } }.collectAsState(isSystemDarkMode, Dispatchers.IO)

            AppTheme(useDarkTheme = isDarkTheme) {
                CompositionLocalProvider(
                    LocalThemeMode provides ThemeMode(isDarkTheme) {
                        ioScope.launch {
                            dataStore.edit { it[IS_DARK_MODE] = !isDarkTheme }
                        }
                    },
                    LocalDataStore provides dataStore
                ) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Surface {
                            LoginScreen()
                        }
                    }
                }
            }
        }
    }
}