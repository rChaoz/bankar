package ro.bankar.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.pipeline.PipelineContext
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankAccount
import ro.bankar.database.BankTransfer
import ro.bankar.database.TransferRequest
import ro.bankar.database.User
import ro.bankar.database.serializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SSendRequestMoney
import ro.bankar.model.SSocketNotification
import ro.bankar.model.StatusResponse
import ro.bankar.plugins.UserPrincipal

fun Route.configureBankTransfers() {
    route("transfer") {
        get("list/{tag}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val tag = call.parameters["tag"]!!

            newSuspendedTransaction {
                val targetUser = User.findByTag(tag)

                if (targetUser == null || targetUser !in user.friends) call.respond(HttpStatusCode.BadRequest, StatusResponse("user_not_found"))
                else call.respond(HttpStatusCode.OK, BankTransfer.findBetween(user, targetUser).serializable(user))
            }
        }

        suspend fun PipelineContext<Unit, ApplicationCall>.sendRequestMoneyBase(
            block: suspend PipelineContext<Unit, ApplicationCall>.(sourceAccount: BankAccount, targetUser: User, data: SSendRequestMoney) -> Unit
        ) {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SSendRequestMoney>()
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return
            }

            newSuspendedTransaction {
                val targetUser = User.findByTag(data.targetTag)
                val sourceAccount = user.bankAccounts.find { it.id.value == data.sourceAccountID }

                if (targetUser == null) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_user"))
                else if (sourceAccount == null) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_account"))
                else if (sourceAccount.currency != data.currency) call.respond(HttpStatusCode.BadRequest, StatusResponse("incorrect_currency"))
                else block(sourceAccount, targetUser, data)
            }
        }

        post("send") {
            sendRequestMoneyBase { sourceAccount, targetUser, data ->
                val targetAccount = targetUser.bankAccounts.find { it.currency == data.currency }
                // If user is in friend list and has an open debit/savings account for the currency, instantly transfer
                // TODO Add "default" account setting to users to specify what account money should go into
                if (targetUser in sourceAccount.user.friends && targetAccount != null) {
                    if (!BankTransfer.transfer(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                        call.respond(HttpStatusCode.Conflict, StatusResponse("balance_low"))
                    else {
                        sendNotificationToUser(targetUser.id, SSocketNotification.STransferNotification)
                        call.respond(HttpStatusCode.OK, StatusResponse("sent"))
                    }
                }
                else {
                    // Create a transfer request
                    if (!TransferRequest.create(sourceAccount, targetUser, data.amount.toBigDecimal(), data.note))
                        call.respond(HttpStatusCode.Conflict, StatusResponse("balance_low"))
                    else {
                        sendNotificationToUser(targetUser.id, SSocketNotification.SRecentActivityNotification)
                        call.respond(HttpStatusCode.OK, StatusResponse("sent_request"))
                    }
                }
            }
        }

        post("request") {
            sendRequestMoneyBase { sourceAccount, targetUser, data ->
                // Create a transfer request
                TransferRequest.create(sourceAccount, targetUser, (-data.amount).toBigDecimal(), data.note)
                sendNotificationToUser(targetUser.id, SSocketNotification.STransferNotification)
                call.respond(HttpStatusCode.OK, StatusResponse("sent_request"))
            }
        }

        get("cancel/{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user

            newSuspendedTransaction {
                val request = call.parameters["id"]?.toIntOrNull()?.let { id -> user.sentTransferRequests.find { it.id.value == id } } ?: run {
                    call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_id")); return@newSuspendedTransaction
                }
                val userID = request.targetUser.id
                request.delete()
                sendNotificationToUser(userID, SSocketNotification.SRecentActivityNotification)
                call.respond(HttpStatusCode.OK, StatusResponse.Success)
            }
        }
    }
}