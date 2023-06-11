package ro.bankar.routing

import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.api.NotificationService
import ro.bankar.database.User
import ro.bankar.database.serializable
import ro.bankar.model.SMessagingToken
import ro.bankar.model.SSendMessage
import ro.bankar.model.SSocketNotification
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondInvalidParam
import ro.bankar.respondNotFound
import ro.bankar.respondSuccess
import ro.bankar.respondValue

fun Route.configureUserMessaging() {
    route("messaging") {
        post("send") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val request = call.receive<SSendMessage>()
            request.validate()?.let {
                call.respondInvalidParam(it); return@post
            }

            newSuspendedTransaction {
                val recipient = User.findByTag(request.recipientTag)
                if (recipient == null || recipient !in user.friends)
                    call.respondNotFound("user")
                else {
                    user.updateLastOpenedConversationWith(recipient)
                    user.sendMessage(recipient, request.message.trim())
                    // Notify recipient if he is connected
                    if (!sendNotificationToUser(recipient.id, SSocketNotification.SMessageNotification(user.tag)))
                        // Otherwise, send a push notification
                        NotificationService.sendMessageNotification(user, recipient, request.message)
                    call.respondSuccess()
                }
            }
        }

        post("register") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val token = call.receive<SMessagingToken>().token
            if (token.isBlank()) {
                call.respondInvalidParam("token"); return@post
            }
            newSuspendedTransaction { user.notificationToken = token }
            call.respondSuccess()
        }

        get("conversation/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val tag = call.parameters["id"]!!

            newSuspendedTransaction {
                val otherUser = User.findByTag(tag)
                if (otherUser == null || otherUser !in user.friends)
                    call.respondNotFound("user")
                else {
                    user.updateLastOpenedConversationWith(otherUser)
                    call.respondValue(user.getConversationWith(otherUser).serializable(user))
                }
            }
        }
    }
}