package ro.bankar.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import ro.bankar.banking.Currency
import ro.bankar.database.BankCard
import kotlin.random.Random

fun Routing.configurePayment() {
    staticResources("download", null, "BanKAR.apk") {
        modify { _, call -> call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"BanKAR.apk\"") }
    }
    post("payment") {
        val data = call.receiveParameters()
        val cardNumber = data["cardNumber"]
            ?.filter { it.isDigit() }
            ?.takeIf { it matches Regex("\\d{14,16}") }
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Bad card number")
        val expirationDate = data["expirationDate"]
            ?.takeIf { it matches Regex("(0[1-9]|1[0-2])/\\d{2}") }
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Bad expiration date")
        val securityCode = data["securityCode"]
            ?.takeIf { it matches Regex("\\d{3,4}") }
            ?: return@post call.respond(HttpStatusCode.BadRequest, "Bad security code")
        newSuspendedTransaction {
            val card = BankCard.findByPaymentInfo(cardNumber, expirationDate, securityCode)
                ?: return@newSuspendedTransaction call.respondRedirect("/payment/fail.html?reason=${"Error: invalid card info".encodeURLParameter()}")
            // Attempt a random payment
            val amount = Random.nextDouble(1.0, 100.0).toBigDecimal()
            val currency = Currency.entries.random()
            val title = listOf(
                "Taco Bell",
                "Emag",
                "PC garage",
                "Fancy Restaurant",
                "Riot Games",
                "nothing",
                "McDonald's, sadly :(",
                "Movie (WALL-E)",
                "Pharmacy",
                "A lot of cheese",
                "Random place",
                "Mobile game",
                "Random microtransactions",
                "Microsoft",
                "Kotlin stuff"
            ).random()
            if (card.pay(amount, currency, title)) call.respondRedirect("/payment/success.html")
            else call.respondRedirect("/payment/fail.html?reason=${"Error: not enough funds.".encodeURLParameter()}")
        }
    }
}