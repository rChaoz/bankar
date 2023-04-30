package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ro.bankar.model.SNewUser

@Serializable
data class LoginSession(val userID: Int, val correctCode: String, val expiration: Instant)

@Serializable
data class SignupSession(val user: SNewUser, val correctCode: String, val expiration: Instant)

fun Application.configureSessions() {
    val memoryStorage = SessionStorageMemory()
    install(Sessions) {
        header<LoginSession>("LoginSession", memoryStorage) {
            serializer = KotlinxSessionSerializer(Json)
        }
        header<SignupSession>("SignupSession", memoryStorage) {
            serializer = KotlinxSessionSerializer(Json)
        }
    }
}