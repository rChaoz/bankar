package ro.bankar.app.data

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import ro.bankar.model.SConversation
import ro.bankar.model.SFriend
import ro.bankar.model.SUser
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val Context.cache: DataStore<Cache> by CacheDatastoreDelegate("cache.bin", OkioSerializerWrapper(CacheSerializer))

@Serializable
data class Cache(
    val profile: SUser? = null,
    val friends: List<SFriend> = emptyList(),
    val conversations: Map<String, SConversation> = emptyMap(),
)

@Suppress("UnnecessaryOptInAnnotation")
@OptIn(ExperimentalSerializationApi::class)
private object CacheSerializer : Serializer<Cache> {
    override val defaultValue = Cache()

    override suspend fun readFrom(input: InputStream) = Json.decodeFromStream<Cache>(input)

    override suspend fun writeTo(t: Cache, output: OutputStream) {
        Json.encodeToStream(t, output)
    }
}

private class OkioSerializerWrapper<T>(private val serializer: Serializer<T>) : OkioSerializer<T> {
    override val defaultValue = serializer.defaultValue

    override suspend fun readFrom(source: BufferedSource) = serializer.readFrom(source.inputStream())

    override suspend fun writeTo(t: T, sink: BufferedSink) {
        serializer.writeTo(t, sink.outputStream())
    }
}

private class CacheDatastoreDelegate<T>(
    private val fileName: String,
    private val serializer: OkioSerializer<T>
) : ReadOnlyProperty<Context, DataStore<T>> {

    private val lock = Any()

    @GuardedBy("lock")
    @Volatile
    private var store: DataStore<T>? = null

    override fun getValue(thisRef: Context, property: KProperty<*>) =
        store ?: synchronized(lock) {
            if (store == null)
                store = DataStoreFactory.create(
                    OkioStorage(FileSystem.SYSTEM, serializer) { File(thisRef.applicationContext.cacheDir, fileName).absolutePath.toPath() }
                )
            store!!
        }
}