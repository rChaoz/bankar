package ro.bankar.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
// LocalDataStore defined in debug/release source sets

val IS_DARK_MODE = booleanPreferencesKey("isDarkMode")
val USER_SESSION = stringPreferencesKey("userSession")
val PREFERRED_CURRENCY = stringPreferencesKey("preferredCurrency")

@Composable
fun <T> DataStore<Preferences>.collectPreferenceAsState(key: Preferences.Key<out T>, defaultValue: T) =
    remember { data.map { it[key] ?: defaultValue } }.collectAsState(defaultValue, Dispatchers.IO)

suspend fun <T> DataStore<Preferences>.setPreference(key: Preferences.Key<T>, value: T) = withContext(Dispatchers.IO) { edit { it[key] = value } }
suspend fun <T> DataStore<Preferences>.removePreference(key: Preferences.Key<T>) = withContext(Dispatchers.IO) { edit { it -= key } }