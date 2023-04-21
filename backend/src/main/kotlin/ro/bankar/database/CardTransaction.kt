package ro.bankar.database

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import ro.bankar.amount
import ro.bankar.currency

@Serializable
data class SCardTransaction(
    val reference: Long,
    val cardID: Int,
    val cardLastFour: String,
    val amount: Double,
    val currency: String,
    val dateTime: LocalDateTime,
    val details: String,
)

class CardTransaction(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardTransaction>(CardTransactions)

    var reference by CardTransactions.reference
    var card by BankCard referencedOn CardTransactions.card
    var amount by CardTransactions.amount
    var currency by CardTransactions.currency
    var dateTime by CardTransactions.dateTime
    var details by CardTransactions.details

    fun serializable() = SCardTransaction(reference, card.id.value, card.cardNumber.toString().takeLast(4), amount.toDouble(), currency, dateTime, details)
}

fun SizedIterable<CardTransaction>.serializable() = map(CardTransaction::serializable)

internal object CardTransactions : IntIdTable(columnName = "transaction_id") {
    val reference = long("reference_id").uniqueIndex()
    val card = reference("card_id", BankCards)
    val amount = amount("amount")
    val currency = currency("currency")
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
    val details = varchar("details", 500)
}