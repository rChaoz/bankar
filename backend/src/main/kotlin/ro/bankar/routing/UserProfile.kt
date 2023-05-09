package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.SizedCollection
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
                newSuspendedTransaction {
                    val otherUser = User.findByAnything(id)
                    if (otherUser == null) call.respond(HttpStatusCode.NotFound, StatusResponse("user_not_found"))
                    else if (user.friends.any { it.id == otherUser.id }) call.respond(HttpStatusCode.Conflict, StatusResponse("user_is_friend"))
                    else if (otherUser.id == user.id) call.respond(HttpStatusCode.BadRequest, StatusResponse("cant_friend_self"))
                    else {
                        // Send friend request if it doesn't already exist
                        if (otherUser.friendRequests.none { it.id == user.id }) {
                            otherUser.addFriendRequest(user)
                            call.respond(HttpStatusCode.OK, StatusResponse.Success)
                        } else {
                            // Else, notify the user
                            call.respond(HttpStatusCode.Conflict, StatusResponse("exists"))
                        }
                    }
                }
            }
        }

        route("friend_requests") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.friendRequests.serializable() })
            }

            get("{action}/{tag}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val action = call.parameters["action"]!!
                if (action != "accept" && action != "decline") {
                    call.respond(HttpStatusCode.NotFound, StatusResponse("action_not_found")); return@get
                }

                val tag = call.parameters["tag"]!!

                newSuspendedTransaction t@{
                    val otherUser = user.friendRequests.find { it.tag == tag }
                    if (otherUser == null) {
                        call.respond(HttpStatusCode.BadRequest, StatusResponse("no_request_from_tag")); return@t
                    }
                    // If user wants to accept request, for each user add the other as friend
                    if (action == "accept") {
                        user.addFriend(otherUser)
                        otherUser.addFriend(user)
                    }
                    // Delete friend request
                    user.friendRequests = SizedCollection(user.friendRequests.filter { it.id != otherUser.id })

                    call.respond(HttpStatusCode.OK, StatusResponse.Success)
                }
            }
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