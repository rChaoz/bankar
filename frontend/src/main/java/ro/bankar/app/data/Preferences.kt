package ro.bankar.app.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
// LocalDataStore defined in debug/release source sets

val KeyUserSession = stringPreferencesKey("userSession")

val KeyLanguage = stringPreferencesKey("language")
val KeyTheme = intPreferencesKey("theme")
val KeyPreferredCurrency = intPreferencesKey("preferredCurrency")

val KeyAuthenticationPin = stringPreferencesKey("authenticationPin")
val KeyFingerprintEnabled = booleanPreferencesKey("fingerprintEnabled")

@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(key: Preferences.Key<out T>, initial: T) =
    remember { data.map { it[key] } }.collectAsState(initial, Dispatchers.IO)

@Composable
inline fun <T, P> DataStore<Preferences>.mapCollectPreferenceAsState(key: Preferences.Key<out T>, initial: P, crossinline mapFunc: (T?) -> P) =
    remember { data.map { it[key].let(mapFunc) } }.collectAsState(initial, Dispatchers.IO)

suspend fun <T> DataStore<Preferences>.setPreference(key: Preferences.Key<T>, value: T) = withContext(Dispatchers.IO) { edit { it[key] = value } }
suspend fun <T> DataStore<Preferences>.removePreference(key: Preferences.Key<T>) = withContext(Dispatchers.IO) { edit { it -= key } }