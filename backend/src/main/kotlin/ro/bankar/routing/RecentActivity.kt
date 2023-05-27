package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankTransfer
import ro.bankar.database.CardTransaction
import ro.bankar.database.Party
import ro.bankar.database.TransferRequest
import ro.bankar.database.previewSerializable
import ro.bankar.database.serializable
import ro.bankar.model.SRecentActivity
import ro.bankar.plugins.UserPrincipal

fun Route.configureRecentActivity() {
    // Get recent activity
    get("recent") {
        val user = call.authentication.principal<UserPrincipal>()!!.user
        val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 3
        call.respond(HttpStatusCode.OK, newSuspendedTransaction {
            // Check if user has any created parties
            val parties = Party.byUser(user)
            // Get the 3 most recent transfers of these accounts
            val recentTransfers = BankTransfer.findRecent(user.bankAccounts, count)
            // As well as the most recent transactions
            val allCards = user.bankAccounts.flatMap { it.cards }
            val recentTransactions = CardTransaction.findRecent(allCards, count)
            // Finally get all pending transfer requests
            val recentRequests = TransferRequest.findRecent(user)
            // Serialize all data
            SRecentActivity(
                recentTransfers.serializable(user),
                recentTransactions.serializable(),
                parties.previewSerializable(),
                recentRequests.serializable(user)
            )
        })
    }
}