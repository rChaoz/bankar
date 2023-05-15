package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import ro.bankar.amount
import ro.bankar.currency
import ro.bankar.model.SCardTransaction

class CardTransaction(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardTransaction>(CardTransactions) {
        fun findRecent(cards: Iterable<BankCard>, count: Int) = cards.map { it.id }.let { ids ->
            find { CardTransactions.card inList ids }.orderBy(CardTransactions.dateTime to SortOrder.DESC).limit(count)
        }
    }

    var reference by CardTransactions.reference
    var card by BankCard referencedOn CardTransactions.card
    var amount by CardTransactions.amount
    var currency by CardTransactions.currency
    var dateTime by CardTransactions.dateTime
    var details by CardTransactions.details
    var title by CardTransactions.title

    fun serializable() = SCardTransaction(reference, card.id.value, card.cardNumber.toString().takeLast(4), amount.toDouble(), currency, dateTime, details, title)
}

fun SizedIterable<CardTransaction>.serializable() = map(CardTransaction::serializable)

internal object CardTransactions : IntIdTable(columnName = "transaction_id") {
    val reference = long("reference_id").uniqueIndex()
    val card = reference("card_id", BankCards)
    val amount = amount("amount")
    val currency = currency("currency")
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
    val details = varchar("details", 500)
    val title = varchar("title", 50)
}