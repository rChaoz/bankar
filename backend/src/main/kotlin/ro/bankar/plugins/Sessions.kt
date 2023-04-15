package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.server.sessions.serialization.*
import java.io.File
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ro.bankar.database.User

@Serializable
data class LoginSession(val userID: Int, val correctCode: String, val expiration: Instant)

fun Application.configureSessions() {
    install(Sessions) {
        header<LoginSession>("LoginSession", SessionStorageMemory()) {
            serializer = KotlinxSessionSerializer(Json)
        }
    }
}