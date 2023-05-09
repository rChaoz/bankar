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
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
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

        get("friends") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.friends.serializable() })
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