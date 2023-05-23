package ro.bankar.routing

import io.ktor.server.auth.authentication
import io.ktor.server.routing.Route
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.send
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import ro.bankar.DEV_MODE
import ro.bankar.model.SSocketNotification
import ro.bankar.plugins.UserPrincipal
import java.util.Collections
import java.util.concurrent.CancellationException

val activeSockets: MutableMap<Int, DefaultWebSocketServerSession> = Collections.synchronizedMap(HashMap())

suspend fun sendNotificationToUser(userID: EntityID<Int>, notification: SSocketNotification) {
    // Encode to string is needed because we want to use SSocketNotification serializer (which is extracted from type, as
    // type is reified for Json.encodeToString) instead of the specific class serializer (e.g. SMessageNotification), to correctly
    // serialize the polymorphic classes
    activeSockets[userID.value]?.send(Json.encodeToString(notification))
}

fun Route.configureSockets() {
    webSocket("socket", if (DEV_MODE) null else "wss") {
        val user = call.authentication.principal<UserPrincipal>()!!.user
        activeSockets.compute(user.id.value) { _, oldValue ->
            if (oldValue != null) throw RuntimeException("Can't open multiple sockets for the same user: ${user.id}")
            else this@webSocket
        }
        try {
            while (true) incoming.receive()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            else if (e !is ClosedReceiveChannelException) e.printStackTrace()
        } finally {
            activeSockets.remove(user.id.value)
        }
    }
}