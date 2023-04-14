package ro.bankar.database

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@Serializable
data class SCardTransaction(
    val referenceID: Long,
    val amount: Double,
    val currency: String,
    val dateTime: LocalDateTime,
    val details: String,
)

class CardTransaction(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardTransaction>(CardTransactions)

    var referenceID by CardTransactions.referenceID
    var card by BankCard referencedOn CardTransactions.cardID
    var amount by CardTransactions.amount
    var currency by CardTransactions.currency
    var dateTime by CardTransactions.dateTime
    var details by CardTransactions.details

    fun serializable() = SCardTransaction(referenceID, amount.toDouble(), currency, dateTime, details)
}

internal object CardTransactions : IntIdTable(columnName = "transaction_id") {
    val referenceID = long("reference_id").uniqueIndex()
    val cardID = reference("card_id", BankCards)
    val amount = amount("amount")
    val currency = currency("currency")
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
    val details = varchar("details", 500)
}