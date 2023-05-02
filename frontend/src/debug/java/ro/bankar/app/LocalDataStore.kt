package ro.bankar.app

import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private object EmptyDataStore : DataStore<Preferences> {
    val emptyPreferences = emptyPreferences()

    override val data: Flow<Preferences> = emptyFlow()
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = emptyPreferences

}

val LocalDataStore = compositionLocalOf<DataStore<Preferences>> { EmptyDataStore }