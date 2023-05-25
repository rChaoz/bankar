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
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.FriendRequests
import ro.bankar.database.User
import ro.bankar.database.friendsSerializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SDirection
import ro.bankar.model.SSocketNotification
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
                call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.friends.friendsSerializable(user) })
            }

            get("add/{id}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val id = call.parameters["id"]!!
                newSuspendedTransaction {
                    val otherUser = User.findByAnything(id)
                    if (otherUser == null) call.respond(HttpStatusCode.NotFound, StatusResponse("user_not_found"))
                    else if (user.hasFriend(otherUser)) call.respond(HttpStatusCode.Conflict, StatusResponse("user_is_friend"))
                    else if (otherUser.id == user.id) call.respond(HttpStatusCode.BadRequest, StatusResponse("cant_friend_self"))
                    else if (otherUser.friendRequests.any { it.id == user.id } || user.friendRequests.any { it.id == otherUser.id })
                        call.respond(HttpStatusCode.Conflict, StatusResponse("exists"))
                    else {
                        // Send the friend request
                        otherUser.addFriendRequest(user)
                        sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                        call.respond(HttpStatusCode.OK, StatusResponse.Success)
                    }
                }
            }

            get("remove/{tag}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val tag = call.parameters["tag"]!!
                newSuspendedTransaction {
                    val otherUser = User.findByTag(tag)
                    if (otherUser == null || otherUser !in user.friends) call.respond(HttpStatusCode.NotFound, StatusResponse("user_not_found"))
                    else {
                        user.removeFriend(otherUser)
                        otherUser.removeFriend(user)
                        sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                        call.respond(HttpStatusCode.OK, StatusResponse.Success)
                    }
                }
            }
        }

        route("friend_requests") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val requests = newSuspendedTransaction {
                    with(FriendRequests) {
                        FriendRequests.select { (sourceUser eq user.id) or (targetUser eq user.id) }.map {
                            val isSent = it[sourceUser] == user.id
                            val otherUser = User.findById(if (isSent) it[targetUser] else it[sourceUser])!!
                            otherUser.friendRequestSerializable(if (isSent) SDirection.Sent else SDirection.Received)
                        }
                    }
                }
                call.respond(HttpStatusCode.OK, requests)
            }

            get("{action}/{tag}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val action = call.parameters["action"]!!
                if (action != "accept" && action != "decline" && action != "cancel") {
                    call.respond(HttpStatusCode.NotFound, StatusResponse("action_not_found")); return@get
                }

                val tag = call.parameters["tag"]!!

                newSuspendedTransaction t@{
                    if (action == "cancel") {
                        val otherUser = User.findByAnything(tag)
                        if (otherUser == null) call.respond(HttpStatusCode.NotFound, StatusResponse("request_not_found"))
                        else {
                            otherUser.friendRequests = SizedCollection(otherUser.friendRequests.filter { it.id != user.id })
                            sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                            call.respond(HttpStatusCode.OK, StatusResponse.Success)
                        }
                        return@t
                    }

                    val otherUser = user.friendRequests.find { it.tag == tag }
                    if (otherUser == null) {
                        call.respond(HttpStatusCode.BadRequest, StatusResponse("no_request_from_tag")); return@t
                    }
                    // Delete this friend request
                    user.friendRequests = SizedCollection(user.friendRequests.filter { it.id != otherUser.id })
                    sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)

                    // If user wants to accept request, for each user add the other as friend
                    if (action == "accept") {
                        user.addFriend(otherUser)
                        otherUser.addFriend(user)
                        // Also, delete opposing-direction request (if it exists)
                        otherUser.friendRequests = SizedCollection(otherUser.friendRequests.filter { it.id != user.id })
                    }

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