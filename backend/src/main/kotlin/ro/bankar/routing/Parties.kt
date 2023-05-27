package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.Party
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SCreateParty
import ro.bankar.model.SSocketNotification
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.UserPrincipal

fun Route.configureParties() {
    route("party") {
        post("create") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SCreateParty>()

            // Verify the party data is valid
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
            }
            newSuspendedTransaction {
                val account = user.bankAccounts.find { it.id.value == data.accountID } ?: run {
                    call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "bank_account")); return@newSuspendedTransaction
                }
                val party = data.amounts.map { pair ->
                    val u = user.friends.find { it.tag == pair.first }
                    if (u == null) {
                        call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "user:${pair.first}")); return@newSuspendedTransaction
                    }
                    u to pair.second.toBigDecimal()
                }
                Party.create(data.note, account, party)
                for (pair in party) sendNotificationToUser(pair.first.id, SSocketNotification.SRecentActivityNotification)
                call.respond(HttpStatusCode.Created, StatusResponse.Success)
            }
        }
    }
}