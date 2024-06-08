package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankAccount
import ro.bankar.database.BankAccounts
import ro.bankar.database.BankTransfer
import ro.bankar.database.TransferRequest
import ro.bankar.database.User
import ro.bankar.database.serializable
import ro.bankar.model.SExternalTransfer
import ro.bankar.model.SOwnTransfer
import ro.bankar.model.SSendRequestMoney
import ro.bankar.model.SSocketNotification
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondError
import ro.bankar.respondInvalidParam
import ro.bankar.respondNotFound
import ro.bankar.respondSuccess
import ro.bankar.respondValue

fun Route.configureBankTransfers() {
    route("transfer") {
        get("list/{tag}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val tag = call.parameters["tag"]!!

            newSuspendedTransaction {
                val targetUser = User.findByTag(tag)

                if (targetUser == null || targetUser !in user.friends) call.respondNotFound("user")
                else call.respondValue(BankTransfer.findBetween(user, targetUser).serializable(user))
            }
        }

        suspend fun PipelineContext<Unit, ApplicationCall>.sendRequestMoneyBase(
            block: suspend PipelineContext<Unit, ApplicationCall>.(sourceAccount: BankAccount, targetUser: User, data: SSendRequestMoney) -> Unit
        ) {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SSendRequestMoney>()
            data.validate()?.let {
                call.respondInvalidParam(it); return
            }

            newSuspendedTransaction {
                val targetUser = User.findByTag(data.targetTag)
                val sourceAccount = user.bankAccounts.find { it.id.value == data.sourceAccountID }

                if (targetUser == null) call.respondNotFound("user")
                else if (sourceAccount == null) call.respondNotFound("bank_account")
                else if (sourceAccount.currency != data.currency) call.respondError("incorrect_currency")
                else block(sourceAccount, targetUser, data)
            }
        }

        post("send") {
            sendRequestMoneyBase { sourceAccount, targetUser, data ->
                val defaultAccount = targetUser.defaultAccount?.takeIf { targetUser.alwaysUseDefaultAccount || it.currency == sourceAccount.currency }
                // If user is in friend list and has a default account instantly transfer
                if (targetUser in sourceAccount.user.friends && defaultAccount != null) {
                    if (!BankTransfer.transfer(sourceAccount, defaultAccount, data.amount.toBigDecimal(), data.note))
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else {
                        sendNotificationToUser(targetUser.id, SSocketNotification.STransferNotification)
                        call.respondValue("sent")
                    }
                }
                else {
                    // Create a transfer request
                    if (TransferRequest.create(sourceAccount, targetUser, data.amount.toBigDecimal(), data.note) == null)
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else {
                        sendNotificationToUser(targetUser.id, SSocketNotification.SRecentActivityNotification)
                        call.respondValue("sent_request")
                    }
                }
            }
        }

        post("request") {
            sendRequestMoneyBase { sourceAccount, targetUser, data ->
                // Create a transfer request
                TransferRequest.create(sourceAccount, targetUser, (-data.amount).toBigDecimal(), data.note)
                sendNotificationToUser(targetUser.id, SSocketNotification.STransferNotification)
                call.respondValue("sent_request")
            }
        }

        post("own") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SOwnTransfer>()
            data.validate()?.let {
                call.respondInvalidParam(it); return@post
            }

            newSuspendedTransaction {
                val sourceAccount = user.bankAccounts.find { it.id.value == data.sourceAccountID }
                val targetAccount = user.bankAccounts.find { it.id.value == data.targetAccountID }
                if (sourceAccount == null || targetAccount == null) {
                    call.respondNotFound("bank_account"); return@newSuspendedTransaction
                }
                if (data.exchanging && (sourceAccount.currency == targetAccount.currency)) {
                    call.respondError("incorrect_exchanging"); return@newSuspendedTransaction
                } else if (!data.exchanging && (targetAccount.currency != sourceAccount.currency)) {
                    call.respondError("incorrect_exchanging"); return@newSuspendedTransaction
                }
                if (data.exchanging) {
                    if (!BankTransfer.transferExchanging(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else call.respondSuccess()
                } else {
                    if (!BankTransfer.transfer(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else call.respondSuccess()
                }
            }
        }

        post("external") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SExternalTransfer>()
            data.validate()?.let {
                call.respondInvalidParam(it); return@post
            }

            newSuspendedTransaction {
                val sourceAccount = user.bankAccounts.find { it.id.value == data.sourceAccountID }
                val targetAccount = BankAccount.find { BankAccounts.iban eq data.targetIBAN }.firstOrNull()
                if (sourceAccount == null || targetAccount == null) {
                    call.respondNotFound("bank_account"); return@newSuspendedTransaction
                }
                if (sourceAccount.currency != targetAccount.currency) {
                    if (!BankTransfer.transferExchanging(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else {
                        sendNotificationToUser(targetAccount.user.id, SSocketNotification.STransferNotification)
                        call.respondSuccess()
                    }
                } else {
                    if (!BankTransfer.transfer(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                    else {
                        sendNotificationToUser(targetAccount.user.id, SSocketNotification.STransferNotification)
                        call.respondSuccess()
                    }
                }
            }
        }

        get("respond/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val accept = when (call.request.queryParameters["action"]) {
                "accept" -> true
                "decline" -> false
                else -> {
                    call.respondInvalidParam("action")
                    return@get
                }
            }
            val accountID = call.request.queryParameters["accountID"]?.toIntOrNull()

            val party = newSuspendedTransaction {
                val request = call.parameters["id"]?.toIntOrNull()?.let { id -> user.receivedTransferRequests.find { it.id.value == id } } ?: run {
                    call.respondInvalidParam("id"); return@newSuspendedTransaction null
                }
                val party = request.partyMember?.party
                val otherAccount = user.bankAccounts.find { it.id.value == accountID }
                if (accept && otherAccount == null) {
                    call.respondNotFound("bank_account"); return@newSuspendedTransaction null
                }
                // Handle the request
                val sourceUserID = request.sourceUser.id
                if (accept) {
                    val result =
                        if (otherAccount!!.currency != request.sourceAccount.currency) request.acceptExchanging(otherAccount)
                        else request.accept(otherAccount)
                    if (!result) {
                        call.respondError("balance_low", HttpStatusCode.Conflict)
                        return@newSuspendedTransaction null
                    }
                } else request.decline()
                sendNotificationToUser(sourceUserID, SSocketNotification.STransferNotification)

                call.respondSuccess()
                party
            } ?: return@get

            // If every invitee has responded, mark the party as completed
            newSuspendedTransaction { if (party.members.all { it.request == null }) party.completed = true }
        }

        get("cancel/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user

            newSuspendedTransaction {
                val request = call.parameters["id"]?.toIntOrNull()?.let { id -> user.sentTransferRequests.find { it.id.value == id } } ?: run {
                    call.respondNotFound("transfer_request"); return@newSuspendedTransaction
                }
                val userID = request.targetUser.id
                request.decline()
                sendNotificationToUser(userID, SSocketNotification.SRecentActivityNotification)
                call.respondSuccess()
            }
        }
    }
}