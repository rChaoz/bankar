package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.database.Statement
import ro.bankar.database.serializable
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SStatementRequest
import ro.bankar.plugins.UserPrincipal

fun Route.configureStatements() {
    route("statements") {
        post("request") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val data = call.receive<SStatementRequest>()
            data.validate()?.let {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = it)); return@post
            }

            newSuspendedTransaction {
                val account = user.bankAccounts.find { it.id.value == data.accountID } ?: run {
                    call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "bank_account")); return@newSuspendedTransaction
                }
                val statement = Statement.generate(data.name, account, data.startDate..data.endDate)
                call.respond(HttpStatusCode.Created, statement.serializable())
            }
        }

        get {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            call.respond(HttpStatusCode.OK, newSuspendedTransaction { Statement.findByUser(user).serializable() })
        }

        get("{id}") {
            val user = call.authentication.principal<UserPrincipal>()!!.user
            val id = call.parameters["id"]?.toIntOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, InvalidParamResponse(param = "id")); return@get
            }

            newSuspendedTransaction {
                val statement = Statement.findById(id)?.takeIf { it.bankAccount.id in user.bankAccountIds } ?: run {
                    call.respond(HttpStatusCode.NotFound, NotFoundResponse(resource = "statement")); return@newSuspendedTransaction
                }
                call.respondBytes(statement.statement.inputStream.readBytes(), ContentType.Application.Pdf)
            }
        }
    }
}