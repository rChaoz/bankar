package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankTransfer
import ro.bankar.database.CardTransaction
import ro.bankar.database.TransferRequest
import ro.bankar.database.serializable
import ro.bankar.model.SRecentActivity
import ro.bankar.plugins.UserPrincipal

fun Route.configureRecentActivity() {
    // Get recent activity
    get("recent") {
        val user = call.authentication.principal<UserPrincipal>()!!.user
        val count = call.request.queryParameters["count"]?.toIntOrNull() ?: 3
        call.respond(HttpStatusCode.OK, newSuspendedTransaction {
            // Get the 3 most recent transfers of these accounts
            val recentTransfers = BankTransfer.findRecent(user.bankAccounts, count)
            // As well as the most recent transactions
            val allCards = user.bankAccounts.flatMap { it.cards }
            val recentTransactions = CardTransaction.findRecent(allCards, count)
            // Finally get 3 most recent transfer requests
            val recentRequests = TransferRequest.findRecent(user, count)
            // Serialize all data
            SRecentActivity(recentTransfers.serializable(user), recentTransactions.serializable(), recentRequests.serializable(user))
        })
    }
}