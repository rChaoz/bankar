package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.BankTransfer
import ro.bankar.database.TransferRequest
import ro.bankar.database.User
import ro.bankar.database.serializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SSendMoney
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

        post("send") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SSendMoney>()
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
            }

            newSuspendedTransaction {
                val targetUser = User.findByTag(data.targetTag)
                val sourceAccount = user.bankAccounts.find { it.id.value == data.sourceAccountID }

                if (targetUser == null) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_user"))
                else if (sourceAccount == null) call.respond(HttpStatusCode.BadRequest, StatusResponse("invalid_account"))
                else if (sourceAccount.currency != data.currency) call.respond(HttpStatusCode.BadRequest, StatusResponse("incorrect_currency"))
                else {
                    val targetAccount = targetUser.bankAccounts.find { it.currency == data.currency }
                    // If user is in friend list and has an open debit/savings account for the currency, instantly transfer
                    // TODO Add "default" account setting to users to specify what account money should go into
                    if (targetUser in user.friends && targetAccount != null) {
                        if (!BankTransfer.transfer(sourceAccount, targetAccount, data.amount.toBigDecimal(), data.note))
                            call.respond(HttpStatusCode.Conflict, StatusResponse("balance_low"))
                        else
                            call.respond(HttpStatusCode.OK, StatusResponse("sent"))
                    }
                    else {
                        // Create a transfer request
                        TransferRequest.create(sourceAccount, targetUser, data.amount.toBigDecimal(), data.note)
                        call.respond(HttpStatusCode.OK, StatusResponse("sent_request"))
                    }
                }
            }
        }
    }
}