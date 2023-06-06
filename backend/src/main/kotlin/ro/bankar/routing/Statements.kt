package ro.bankar.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.Statement
import ro.bankar.database.serializable
import ro.bankar.model.SStatementRequest
import ro.bankar.plugins.UserPrincipal
import ro.bankar.respondInvalidParam
import ro.bankar.respondNotFound
import ro.bankar.respondValue

fun Route.configureStatements() {
    route("statements") {
        post("request") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SStatementRequest>()
            data.validate()?.let {
                call.respondInvalidParam(it); return@post
            }

            newSuspendedTransaction {
                val account = user.bankAccounts.find { it.id.value == data.accountID } ?: run {
                    call.respondNotFound("bank_account"); return@newSuspendedTransaction
                }
                val statement = Statement.generate(data.name, account, data.startDate..data.endDate)
                call.respondValue(statement.serializable(), HttpStatusCode.Created)
            }
        }

        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respondValue(newSuspendedTransaction { Statement.findByUser(user).serializable() })
        }

        get("{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respondInvalidParam("id"); return@get
            }

            newSuspendedTransaction {
                val statement = Statement.findById(id)?.takeIf { it.bankAccount.id in user.bankAccountIds } ?: run {
                    call.respondNotFound("statement"); return@newSuspendedTransaction
                }
                call.respondBytes(statement.statement.inputStream.readBytes(), ContentType.Application.Pdf)
            }
        }
    }
}