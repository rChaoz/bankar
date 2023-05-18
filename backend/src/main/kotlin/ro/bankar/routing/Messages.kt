package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.User
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SSendMessage
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.UserPrincipal

fun Route.configureUserMessaging() {
    route("messaging") {
        post("send") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val request = call.receive<SSendMessage>()
            request.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
            }

            newSuspendedTransaction {
                val recipient = User.findByTag(request.recipientTag)
                if (recipient == null || recipient !in user.friends)
                    call.respond(HttpStatusCode.BadRequest, StatusResponse("user_not_found"))
                else {
                    user.sendMessage(recipient, request.message.trim())
                    call.respond(HttpStatusCode.OK, StatusResponse.Success)
                }
            }
        }

        get("conversation/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val tag = call.parameters["id"]!!

            newSuspendedTransaction {
                val otherUser = User.findByTag(tag)
                if (otherUser == null || otherUser !in user.friends)
                    call.respond(HttpStatusCode.BadRequest, StatusResponse("user_not_found"))
                else call.respond(HttpStatusCode.OK, user.getSerializableConversationWith(otherUser))
            }
        }
    }
}