package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.*
import ro.bankar.database.FriendRequests
import ro.bankar.database.User
import ro.bankar.database.friendsSerializable
import ro.bankar.model.SDirection
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.plugins.UserPrincipal

fun Route.configureUserProfiles() {
    route("profile") {
        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respondValue(newSuspendedTransaction { user.serializable() })
        }

        route("friends") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                call.respondValue(newSuspendedTransaction { user.friends.friendsSerializable(user) })
            }

            get("add/{id}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val id = call.parameters["id"]!!
                newSuspendedTransaction {
                    val otherUser = User.findByAnything(id)
                    if (otherUser == null) call.respondNotFound("user")
                    else if (user.hasFriend(otherUser)) call.respondError("user_is_friend", HttpStatusCode.Conflict)
                    else if (otherUser.id == user.id) call.respondError("cant_friend_self", HttpStatusCode.Conflict)
                    else if (otherUser.friendRequests.any { it.id == user.id } || user.friendRequests.any { it.id == otherUser.id })
                        call.respondError("exists", HttpStatusCode.Conflict)
                    else {
                        // Send the friend request
                        otherUser.addFriendRequest(user)
                        sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                        call.respondSuccess(HttpStatusCode.Created)
                    }
                }
            }

            get("remove/{tag}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val tag = call.parameters["tag"]!!
                newSuspendedTransaction {
                    val otherUser = User.findByTag(tag)
                    if (otherUser == null || otherUser !in user.friends) call.respondNotFound("user")
                    else {
                        user.removeFriend(otherUser)
                        otherUser.removeFriend(user)
                        sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                        call.respondSuccess()
                    }
                }
            }
        }

        route("friend_requests") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val requests = newSuspendedTransaction {
                    with(FriendRequests) {
                        FriendRequests.select(sourceUser, targetUser).where { (sourceUser eq user.id) or (targetUser eq user.id) }.map {
                            val isSent = it[sourceUser] == user.id
                            val otherUser = User.findById(if (isSent) it[targetUser] else it[sourceUser])!!
                            otherUser.friendRequestSerializable(if (isSent) SDirection.Sent else SDirection.Received)
                        }
                    }
                }
                call.respondValue(requests)
            }

            get("{action}/{tag}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val action = call.parameters["action"]!!
                if (action != "accept" && action != "decline" && action != "cancel") {
                    call.respondNotFound("action"); return@get
                }

                val tag = call.parameters["tag"]!!

                newSuspendedTransaction t@{
                    if (action == "cancel") {
                        val otherUser = User.findByAnything(tag)
                        if (otherUser == null) call.respondNotFound("friend_request")
                        else {
                            otherUser.friendRequests = SizedCollection(otherUser.friendRequests.filter { it.id != user.id })
                            sendNotificationToUser(otherUser.id, SSocketNotification.SFriendNotification)
                            call.respondSuccess()
                        }
                        return@t
                    }

                    val otherUser = user.friendRequests.find { it.tag == tag }
                    if (otherUser == null) {
                        call.respondError("no_request_from_tag"); return@t
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

                    call.respondSuccess()
                }
            }
        }

        // Update about and/or profile picture
        put("update") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            // Get & validate data
            val data = call.receive<SUserProfileUpdate>()
            data.validate()?.let {
                call.respondInvalidParam(it); return@put
            }
            // Update user profile
            newSuspendedTransaction {
                data.about?.let { user.about = it }
                data.avatar?.let {
                    user.avatar = it
                }
            }
            call.respondSuccess()
        }
    }
}