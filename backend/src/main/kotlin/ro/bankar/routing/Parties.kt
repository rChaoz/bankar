package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.Party
import ro.bankar.model.SCreateParty
import ro.bankar.model.SSocketNotification
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondError
import ro.bankar.respondInvalidParam
import ro.bankar.respondNotFound
import ro.bankar.respondSuccess
import ro.bankar.respondValue

fun Route.configureParties() {
    route("party") {
        post("create") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SCreateParty>()

            // Verify the party data is valid
            data.validate()?.let {
                call.respondInvalidParam(it); return@post
            }
            newSuspendedTransaction {
                val account = user.bankAccounts.find { it.id.value == data.accountID } ?: run {
                    call.respondNotFound("bank_account"); return@newSuspendedTransaction
                }
                val party = data.amounts.map { pair ->
                    val u = user.friends.find { it.tag == pair.first }
                    if (u == null) {
                        call.respondNotFound("user:${pair.first}"); return@newSuspendedTransaction
                    }
                    u to pair.second.toBigDecimal()
                }
                Party.create(data.note, account, party)
                for (pair in party) sendNotificationToUser(pair.first.id, SSocketNotification.SRecentActivityNotification)
                call.respondSuccess(HttpStatusCode.Created)
            }
        }

        get("{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            newSuspendedTransaction {
                val party = call.parameters["id"]?.toIntOrNull()?.let { Party.findById(it) }?.takeIf {
                    it.hostAccount.user.id == user.id || it.members.any { member -> member.user.id == user.id }
                } ?: run {
                    call.respondNotFound("party"); return@newSuspendedTransaction
                }
                call.respondValue(party.serializable(user))
            }
        }

        get("cancel/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            newSuspendedTransaction {
                val party = call.parameters["id"]?.toIntOrNull()?.let { Party.findById(it) }
                if (party == null || party.hostAccount.user.id != user.id) {
                    call.respondNotFound("party"); return@newSuspendedTransaction
                }
                for (member in party.members) sendNotificationToUser(member.user.id, SSocketNotification.SRecentActivityNotification)
                if (party.cancel()) call.respondSuccess()
                else call.respondError("already_cancelled")
            }
        }
    }
}