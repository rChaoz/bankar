package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankAccount
import ro.bankar.database.BankCard
import ro.bankar.database.CREDIT_DATA
import ro.bankar.database.serializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewBankCard
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.UserPrincipal

fun Route.configureBankAccounts() {
    route("accounts") {
        // Get all accounts
        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respond(HttpStatusCode.OK, newSuspendedTransaction { user.bankAccounts.serializable() })
        }

        // Open bank account
        post("new") {
            val user = call.authentication.principal<UserPrincipal>()!!.user

            val newAccountData = call.receive<SNewBankAccount>()
            newAccountData.validate(CREDIT_DATA)?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it))
                return@post
            }
            newSuspendedTransaction { BankAccount.create(user, newAccountData, CREDIT_DATA.find { it.currency == newAccountData.currency }) }
            call.respond(HttpStatusCode.Created, StatusResponse.Success)
        }

        // Bank account by ID
        route("{id}") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "account_id")); return@get
                }
                val accountData = newSuspendedTransaction t@{
                    val account = BankAccount.findById(accountID) ?: return@t null
                    if (account.user.id != user.id) return@t null
                    account.serializable(user)
                }
                if (accountData == null) call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "account_id"))
                else call.respond(HttpStatusCode.OK, accountData)
            }
            post("customise") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val accountID = call.parameters["id"]?.toIntOrNull()
                val data = call.receive<SNewBankAccount>()

                data.validate(CREDIT_DATA)?.takeIf { it == "color" || it == "name" }?.let {
                    call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
                }
                newSuspendedTransaction {
                    val account = user.bankAccounts.find { it.id.value == accountID }
                    if (account == null) call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "account_id"))
                    else {
                        account.name = data.name
                        account.color = data.color
                        call.respond(HttpStatusCode.OK, StatusResponse.Success)
                    }
                }
            }
            // Create new bank card
            post("new") {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                // Get new card data
                val newCardData = call.receive<SNewBankCard>()
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
                val user = call.authentication.principal<UserPrincipal>()!!.user

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