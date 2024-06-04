package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore

val LocalCacheStore = compositionLocalOf<DataStore<Cache>> { throw RuntimeException("LocalCacheStore provider not found") }