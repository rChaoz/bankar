package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.InvalidParamResponse
import ro.bankar.NotFoundResponse
import ro.bankar.StatusResponse
import ro.bankar.database.*
import ro.bankar.plugins.UserPrincipal

fun Route.configureBanking() {
    route("accounts") {
        // Get all accounts
        get {
            val user = call.authentication.principal<UserPrincipal>()?.user ?: run {
                call.respond(HttpStatusCode.InternalServerError); return@get
            }

            val accounts = newSuspendedTransaction { user.bankAccounts.serializable() }
            call.respond(HttpStatusCode.OK, accounts)
        }

        // Open bank account
        post("new") {
            val user = call.authentication.principal<UserPrincipal>()?.user ?: run {
                call.respond(HttpStatusCode.InternalServerError); return@post
            }

            val newAccountData = call.receive<SNewAccount>()
            newAccountData.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it))
                return@post
            }
            newSuspendedTransaction { BankAccount.create(user, newAccountData) }
            call.respond(HttpStatusCode.Created, StatusResponse.Success)
        }

        // Bank account by ID
        route("{id}") {
            get {
                val user = call.authentication.principal<UserPrincipal>()?.user ?: run {
                    call.respond(HttpStatusCode.InternalServerError); return@get
                }

                val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "account_id")); return@get
                }
                val accountData = newSuspendedTransaction t@{
                    val account = BankAccount.findById(accountID) ?: return@t null
                    if (account.user.id != user.id) return@t null
                    account.serializable()
                }
                if (accountData == null) call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "account_id"))
                else call.respond(HttpStatusCode.OK, accountData)
            }
            // Create new bank card
            post("new") {
                val user = call.authentication.principal<UserPrincipal>()?.user ?: run {
                    call.respond(HttpStatusCode.InternalServerError); return@post
                }

                // Get new card data
                val newCardData = call.receive<SNewCard>()
                newCardData.validate()?.let {
                    call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
                }

                // Create new card
                newSuspendedTransaction t@{
                    val account = call.parameters["id"]?.toIntOrNull()?.let { BankAccount.findById(it) }?.takeIf { it.user.id == user.id } ?: run {
                        call.respond(HttpStatusCode.NotFound, InvalidParamResponse(param = "account_id")); return@t
                    }
                    BankCard.create(newCardData, account)
                }
                call.respond(HttpStatusCode.Created, StatusResponse.Success)
            }
            // Get bank card data
            get("{cardID}") {
                val user = call.authentication.principal<UserPrincipal>()?.user ?: run {
                    call.respond(HttpStatusCode.InternalServerError); return@get
                }

                val accountID = call.parameters["id"]?.toIntOrNull()
                val cardID = call.parameters["cardID"]?.toIntOrNull()
                val sensitive = call.request.queryParameters["sensitive"].toBoolean()
                if (accountID == null || cardID == null) {
                    call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = "account_or_card_id")); return@get
                }
                val card = newSuspendedTransaction { BankCard.find(cardID, accountID)?.takeIf { it.bankAccount.user.id == user.id }?.serializable(sensitive) } ?: run {
                    call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = "account_or_card_id")); return@get
                }
                call.respond(HttpStatusCode.OK, card)
            }
        }
    }
}