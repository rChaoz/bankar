package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankAccount
import ro.bankar.database.BankCard
import ro.bankar.database.CREDIT_DATA
import ro.bankar.database.Parties
import ro.bankar.database.Party
import ro.bankar.database.TransferRequest
import ro.bankar.database.TransferRequests
import ro.bankar.database.serializable
import ro.bankar.model.SCustomiseBankAccount
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewBankCard
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondError
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
                    if (account.user.id != user.id || account.closed) return@t null
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
                    val account = user.bankAccounts.find { !it.closed && it.id.value == accountID }
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
                    val account =
                        call.parameters["id"]?.toIntOrNull()?.let { BankAccount.findById(it) }?.takeIf { it.user.id == user.id && !it.closed }
                            ?: run { call.respondNotFound("bank_account"); return@t }
                    val newCard = BankCard.create(newCardData, account)
                    call.respondValue(newCard.id.value)
                }
            }
            // Card by ID
            route("{cardID}") {
                get {
                    val user = call.authentication.principal<UserPrincipal>()!!.user

                    val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("account_id"); return@get
                    }
                    val cardID = call.parameters["cardID"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("card_id"); return@get
                    }
                    val card = newSuspendedTransaction {
                        BankCard.find(cardID, accountID)?.takeIf { it.bankAccount.user.id == user.id }?.serializable(true)
                    } ?: run {
                        call.respondNotFound("card"); return@get
                    }
                    call.respondValue(card)
                }

                post {
                    val user = call.authentication.principal<UserPrincipal>()!!.user

                    val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("account_id"); return@post
                    }
                    val cardID = call.parameters["cardID"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("card_id"); return@post
                    }

                    // Get updated card data
                    val newCardData = call.receive<SNewBankCard>()
                    newCardData.validate(updating = true)?.let {
                        call.respondInvalidParam(it); return@post
                    }

                    newSuspendedTransaction {
                        val card = BankCard.find(cardID, accountID)?.takeIf { it.bankAccount.user.id == user.id } ?: run {
                            call.respondNotFound("card"); return@newSuspendedTransaction
                        }
                        if (newCardData.name.isNotEmpty()) card.name = newCardData.name.trim()
                        if (newCardData.limit != -1.0) card.limit = newCardData.limit.toBigDecimal()
                        call.respondSuccess()
                    }
                }

                post("reset_limit") {
                    val user = call.authentication.principal<UserPrincipal>()!!.user

                    val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("account_id"); return@post
                    }
                    val cardID = call.parameters["cardID"]?.toIntOrNull() ?: run {
                        call.respondInvalidParam("card_id"); return@post
                    }

                    newSuspendedTransaction {
                        val card = BankCard.find(cardID, accountID)?.takeIf { it.bankAccount.user.id == user.id } ?: run {
                            call.respondNotFound("card"); return@newSuspendedTransaction
                        }
                        card.limitCurrent = 0.toBigDecimal()
                        call.respondSuccess()
                    }
                }
            }
            // Close bank account
            delete {
                val user = call.authentication.principal<UserPrincipal>()!!.user

                val accountID = call.parameters["id"]?.toIntOrNull() ?: run {
                    call.respondInvalidParam("account_id"); return@delete
                }
                newSuspendedTransaction {
                    val account = BankAccount.findById(accountID)
                    if (account == null || account.user.id != user.id || account.closed)
                        call.respondNotFound("account")
                    else if (!account.cards.empty())
                        call.respondError("account_has_cards")
                    else if (!TransferRequest.find { TransferRequests.sourceAccount eq account.id }.empty())
                        call.respondError("pending_transfer_requests")
                    else if (!Party.find { not(Parties.completed) and (Parties.hostAccount eq account.id) }.empty())
                        call.respondError("pending_parties")
                    else {
                        account.closed = true
                        call.respondSuccess()
                    }
                }
            }
        }
    }
}