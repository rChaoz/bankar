package ro.bankar.app.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

private object EmptyDataStore : DataStore<Preferences> {
    private val emptyPreferences = emptyPreferences()

    override val data: Flow<Preferences> = flowOf(emptyPreferences)
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = emptyPreferences

}

val LocalDataStore = staticCompositionLocalOf<DataStore<Preferences>> { EmptyDataStore }