package ro.bankar.routing

import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankTransfer
import ro.bankar.database.CardTransaction
import ro.bankar.database.Party
import ro.bankar.database.TransferRequest
import ro.bankar.database.previewSerializable
import ro.bankar.database.serializable
import ro.bankar.model.SRecentActivity
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondValue

fun Route.configureRecentActivity() {
    // Get recent activity & notifications
    route("recentActivity") {
        // Get recent 3 transfers/transactions and all notifications
        get("short") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respondValue(newSuspendedTransaction {
                // Check if user has any created parties
                val parties = Party.byUserPending(user) + Party.byUserCompleted(user).limit(3)
                // Get the 3 most recent transfers of these accounts
                val recentTransfers = BankTransfer.findRecent(user, 3)
                // As well as the most recent transactions
                val allCards = user.bankAccounts.flatMap { it.cards }
                val recentTransactions = CardTransaction.findRecent(allCards, 3)
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
        // Get more recent activity, doesn't include notifications
        get("long") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            // Get completed parties, transfers & transactions
            call.respondValue(newSuspendedTransaction {
                val parties = Party.byUserCompleted(user)
                val transfers = BankTransfer.findRecent(user, 100)
                val allCards = user.bankAccounts.flatMap { it.cards }
                val recentTransactions = CardTransaction.findRecent(allCards, 100)
                // Serialize all data
                SRecentActivity(
                    transfers.serializable(user),
                    recentTransactions.serializable(),
                    parties.previewSerializable(),
                    emptyList()
                )
            })
        }
    }
}