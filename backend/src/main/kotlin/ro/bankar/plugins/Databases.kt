package ro.bankar.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.database.*
import kotlin.time.Duration.Companion.minutes

fun Database.Companion.connect() = this.connect(
    url = "jdbc:h2:file:./build/test",
    user = "root",
    driver = "org.h2.Driver",
    password = ""
)

val Database.Companion.tables
    get() = listOf(
        Users, FriendRequests, FriendPairs,
        BankAccounts, BankTransfers, BankCards, CardTransactions,
        Parties, PartyMembers, TransferRequests,
        AssetAccounts,
    )

fun Application.configureDatabase() {
    Database.connect()
    // Configure routing
    routing {
        // Users
        route("login") {
            // First login step
            post("initial") {
                @Serializable
                class InitialLoginData(
                    val id: String,
                    val password: String,
                )

                // Verify login information
                val login = call.receive<InitialLoginData>()
                val user = transaction { User.findByAnything(login.id) }
                if (user == null || !user.verifyPassword(login.password)) {
                    call.respond(HttpStatusCode.Unauthorized, "invalid username or password")
                    return@post
                }
                // Save user to session
                // TODO Call API to send SMS message
                call.sessions.set(LoginSession(user.id.value, "1234", Clock.System.now() + 30.minutes))
                call.respond(HttpStatusCode.OK)
                // Client should call /login/final with the SMS code
            }

            // Second login step
            post("final") {
                @Serializable
                data class FinalLoginData(
                    val smsCode: String
                )

                // Verify SMS message
                val login = call.receive<FinalLoginData>()
                val session = call.sessions.get<LoginSession>()
                when {
                    session == null -> {
                        call.respond(HttpStatusCode.Unauthorized, "invalid session")
                        return@post
                    }
                    session.expiration < Clock.System.now() -> {
                        call.respond(HttpStatusCode.Unauthorized, "session expired")
                        return@post
                    }
                    session.correctCode != login.smsCode -> {
                        call.respond(HttpStatusCode.Unauthorized, "invalid SMS code")
                        return@post
                    }
                }

                // Complete authentication
                val user = transaction { User.findById(session!!.userID) }
                if (user == null) {
                    call.respond(HttpStatusCode.InternalServerError)
                    return@post
                }
                call.sessions.clear<LoginSession>()

                // Return auth token
                val token = transaction { user.createSessionToken() }
                call.response.headers.append("Authorization", "Bearer $token")
                call.respond(HttpStatusCode.OK)
            }
        }

        post("signup") {
            // Receive signup data
            val data = call.receive<SSignupData>()
            // Validate data
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, "invalid $it")
                return@post
            }
            // Create user
            val user = try {
                transaction { User.createUser(data) }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, "e-mail, phone number or tag already exists")
                return@post
            }

            // Save login session
            call.sessions.set(LoginSession(user.id.value, "1234", Clock.System.now() + 30.minutes))
            call.respond(HttpStatusCode.OK)
            // Client should call /login/final with the SMS code
        }
    }
}
