package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.COUNTRY_DATA
import ro.bankar.DEV_MODE
import ro.bankar.api.SmsService
import ro.bankar.database.User
import ro.bankar.generateNumeric
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SFinalLoginData
import ro.bankar.model.SInitialLoginData
import ro.bankar.model.SNewUser
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.LoginSession
import kotlin.time.Duration.Companion.minutes

private fun generateCode() = if (DEV_MODE) "123456" else generateNumeric(6)

fun Route.configureUsers() {
    // Check if tag is taken (or if it's invalid)
    get("check_tag") {
        val tag = call.request.queryParameters["q"]
        if (tag.isNullOrEmpty() || !SNewUser.tagRegex.matches(tag)) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_tag"))
        else if (newSuspendedTransaction { User.isTagTaken(tag) }) call.respond(HttpStatusCode.Conflict, StatusResponse("exists"))
        else call.respond(HttpStatusCode.OK, StatusResponse("valid"))
    }

    route("login") {
        // First login step
        post("initial") {
            newSuspendedTransaction t@{
                // Verify login information
                val login = call.receive<SInitialLoginData>()
                val user = User.findByAnything(login.id)
                if (user == null || !user.verifyPassword(login.password)) {
                    call.respond(HttpStatusCode.Unauthorized, StatusResponse("invalid_username_or_password")); return@t
                } else if (user.disabled) {
                    call.respond(HttpStatusCode.Forbidden, StatusResponse("account_disabled"))
                }
                // Send SMS code and save user&code to session
                val code = generateCode()
                SmsService.sendCode(user.phone, code)
                call.sessions.set(LoginSession(user.id.value, code, Clock.System.now() + 30.minutes))

                call.respond(HttpStatusCode.OK, StatusResponse.Success)
                // Client should now call /login/final with the SMS code
            }
        }

        // Second login step
        post("final") {
            newSuspendedTransaction t@{
                // Verify SMS message
                val login = call.receive<SFinalLoginData>()
                val session = call.sessions.get<LoginSession>()
                when {
                    session == null -> {
                        call.respond(HttpStatusCode.Unauthorized, StatusResponse("invalid_session"))
                        return@t
                    }

                    session.expiration < Clock.System.now() -> {
                        call.respond(HttpStatusCode.Forbidden, StatusResponse("session_expired"))
                        return@t
                    }

                    session.correctCode != login.smsCode -> {
                        call.respond(HttpStatusCode.Forbidden, StatusResponse("invalid_code"))
                        return@t
                    }
                }

                // Complete authentication
                val user = User.findById(session!!.userID) ?: run {
                    call.respond(HttpStatusCode.InternalServerError); return@t
                }
                // No longer needed
                call.sessions.clear<LoginSession>()

                // Return auth token
                val token = user.createSessionToken()
                call.response.headers.append("Authorization", "Bearer $token")
                call.respond(HttpStatusCode.OK, StatusResponse.Success)
            }
        }
    }

    post("signup") {
        newSuspendedTransaction t@{
            // Receive signup data
            val data = call.receive<SNewUser>()
            // Validate data
            data.validate(COUNTRY_DATA)?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@t
            }

            // Check that a user with the same tag, e-mail or phone isn't already registered
            User.checkRegistered(data)?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse("already_exists", it))
            }

            // Send SMS first to ensure phone number is good
            val code = generateCode()
            if (!SmsService.sendCode(data.phone, code)) {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = "phone")); return@t
            }

            // Create user
            val user = User.createUser(data)

            // Save login session (do first login step)
            call.sessions.set(LoginSession(user.id.value, code, Clock.System.now() + 30.minutes))
            call.respond(HttpStatusCode.OK, StatusResponse.Success)
            // Client should now call /login/final with the SMS code
        }
    }
}