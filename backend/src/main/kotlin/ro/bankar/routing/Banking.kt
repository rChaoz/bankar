package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankAccount
import ro.bankar.database.BankCard
import ro.bankar.database.SNewAccount
import ro.bankar.database.serializable
import ro.bankar.plugins.UserPrincipal

fun Route.configureBanking() {
    route("accounts") {
        // Get all accounts
        get {
            val user = call.authentication.principal<UserPrincipal>()?.user
            if (user == null) {
                call.respond(HttpStatusCode.InternalServerError); return@get
            }

            val accounts = newSuspendedTransaction { user.bankAccounts.serializable() }
            call.respond(HttpStatusCode.OK, accounts)
        }

        // Open bank account
        post("new") {
            val user = call.authentication.principal<UserPrincipal>()?.user
            if (user == null) {
                call.respond(HttpStatusCode.InternalServerError); return@post
            }

            val newAccountData = call.receive<SNewAccount>()
            newAccountData.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, "invalid $it")
                return@post
            }
            newSuspendedTransaction { BankAccount.create(user, newAccountData) }
            call.respond(HttpStatusCode.OK)
        }

        // Bank account by ID
        route("{id}") {
            get {
                val user = call.authentication.principal<UserPrincipal>()?.user
                if (user == null) {
                    call.respond(HttpStatusCode.InternalServerError); return@get
                }

                val accountID = call.parameters["id"]?.toIntOrNull()
                if (accountID == null) {
                    call.respond(HttpStatusCode.BadRequest, "invalid bank account ID"); return@get
                }
                val account = newSuspendedTransaction { BankAccount.findById(accountID) }
                if (account == null || account.user != user) {
                    call.respond(HttpStatusCode.BadRequest, "invalid bank account ID"); return@get
                }
                call.respond(HttpStatusCode.OK, account.serializable())
            }
            // Get credit card data
            get("{cardID}") {
                val user = call.authentication.principal<UserPrincipal>()?.user
                if (user == null) {
                    call.respond(HttpStatusCode.InternalServerError); return@get
                }

                val accountID = call.parameters["id"]?.toIntOrNull()
                val cardID = call.parameters["cardID"]?.toIntOrNull()
                val sensitive = call.request.queryParameters["sensitive"] == "true"
                if (accountID == null || cardID == null) {
                    call.respond(HttpStatusCode.BadRequest, "invalid card ID"); return@get
                }
                val card = newSuspendedTransaction { BankCard.find(cardID, accountID) }
                if (card == null || card.bankAccount.user != user) {
                    call.respond(HttpStatusCode.BadRequest, "invalid account or card ID"); return@get
                }
                call.respond(HttpStatusCode.OK, card.serializable(sensitive))
            }
        }
    }
}