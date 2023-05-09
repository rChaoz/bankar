package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.User
import ro.bankar.database.serializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.UserPrincipal

fun Route.configureUserProfiles() {
    route("profile") {
        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.serializable() })
        }

        route("friends") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.friends.serializable() })
            }

            get("add/{id}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val id = call.parameters["id"] ?: run {
                    call.respond(HttpStatusCode.BadRequest, StatusResponse("no_id_provided")); return@get
                }
                val otherUser = newSuspendedTransaction { User.findByAnything(id) }
                if (otherUser == null) call.respond(HttpStatusCode.NotFound, StatusResponse("user_not_found"))
                else if (otherUser.id == user.id) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_id"))
                else newSuspendedTransaction {
                    // Send friend request if it doesn't already exist
                    if (otherUser.friendRequests.none { it.id == user.id }) otherUser.addFriendRequest(user)
                    call.respond(HttpStatusCode.OK, StatusResponse.Success)
                }
            }
        }

        get("friend_requests") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.friendRequests.serializable() })
        }
        // Update about and/or profile picture
        put("update") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            // Get & validate data
            val data = call.receive<SUserProfileUpdate>()
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@put
            }
            // Update user profile
            newSuspendedTransaction {
                data.about?.let { user.about = it }
                data.avatar?.let {
                    user.avatar = null
                    user.avatar = ExposedBlob(it)
                }
            }
            call.respond(HttpStatusCode.OK, StatusResponse.Success)
        }
    }
}