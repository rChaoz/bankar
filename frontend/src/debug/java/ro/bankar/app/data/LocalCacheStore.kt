package ro.bankar.app.data

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.flowOf

private object EmptyCacheStore : DataStore<Cache> {
    private val emptyCache = Cache()

    override val data = flowOf(emptyCache)

    override suspend fun updateData(transform: suspend (t: Cache) -> Cache) = emptyCache
}

val LocalCacheStore = staticCompositionLocalOf<DataStore<Cache>> { EmptyCacheStore }