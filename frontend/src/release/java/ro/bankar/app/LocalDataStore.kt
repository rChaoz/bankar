package ro.bankar.app

import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

val LocalDataStore = compositionLocalOf<DataStore<Preferences>> { throw RuntimeException("LocalDataStore provider not found") }