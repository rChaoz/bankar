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
import ro.bankar.database.BankAccount
import ro.bankar.database.BankCard
import ro.bankar.database.CREDIT_DATA
import ro.bankar.database.serializable
import ro.bankar.model.SCustomiseBankAccount
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewBankCard
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondInvalidParam
import ro.bankar.respondNotFound
import ro.bankar.respondSuccess
import ro.bankar.respondValue

fun Route.configureBankAccounts() {
    route("accounts") {
        // Get all accounts
        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respondValue(newSuspendedTransaction { user.bankAccounts.serializable() })
        }

        // Open bank account
        post("new") {
            val user = call.authentication.principal<UserPrincipal>()!!.user

            val newAccountData = call.receive<SNewBankAccount>()
            newAccountData.validate(CREDIT_DATA)?.let {
                call.respondInvalidParam(it); return@post
            }
            newSuspendedTransaction { BankAccount.create(user, newAccountData, CREDIT_DATA.find { it.currency == newAccountData.currency }) }
            call.respondSuccess(HttpStatusCode.Created)
        }

        // Bank account by ID
        route("{id}") {
            get {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respondInvalidParam("id"); return@get
                }
                val accountData = newSuspendedTransaction t@{
                    val account = BankAccount.findById(accountID) ?: return@t null
                    if (account.user.id != user.id) return@t null
                    account.serializable(user)
                }
                if (accountData == null) call.respondNotFound("bank_account")
                else call.respondValue(accountData)
            }
            post("customise") {
                val user = call.authentication.principal<UserPrincipal>()!!.user
                val accountID = call.parameters["id"]?.toIntOrNull()
                val data = call.receive<SCustomiseBankAccount>()

                data.validate()?.let {
                    call.respondInvalidParam(it); return@post
                }
                newSuspendedTransaction {
                    val account = user.bankAccounts.find { it.id.value == accountID }
                    if (account == null) call.respondNotFound("bank_account")
                    else {
                        account.name = data.name
                        account.color = data.color
                        call.respondSuccess()
                    }
                }
            }
            // Create new bank card
            post("new") {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                // Get new card data
                val newCardData = call.receive<SNewBankCard>()
                newCardData.validate()?.let {
                    call.respondInvalidParam(it); return@post
                }

                // Create new card
                newSuspendedTransaction t@{
                    val account = call.parameters["id"]?.toIntOrNull()?.let { BankAccount.findById(it) }?.takeIf { it.user.id == user.id } ?: run {
                        call.respondNotFound("bank_account"); return@t
                    }
                    BankCard.create(newCardData, account)
                }
                call.respondSuccess(HttpStatusCode.Created)
            }
            // Get bank card data
            get("{cardID}") {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respondInvalidParam("account_id"); return@get
                }
                val cardID = call.parameters["cardID"]?.toIntOrNull() ?: run {
                    call.respondInvalidParam("card_id"); return@get
                }
                val sensitive = call.request.queryParameters["sensitive"].toBoolean()
                val card = newSuspendedTransaction {
                    BankCard.find(cardID, accountID)?.takeIf { it.bankAccount.user.id == user.id }?.serializable(sensitive)
                } ?: run {
                    call.respondNotFound("card"); return@get
                }
                call.respondValue(card)
            }
        }
    }
}