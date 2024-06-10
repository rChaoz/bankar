package ro.bankar.database

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import ro.bankar.amount
import ro.bankar.currency
import ro.bankar.model.SCardTransaction

class CardTransaction(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CardTransaction>(CardTransactions) {
        fun findRecent(cards: Iterable<BankCard>, count: Int) = cards.map { it.id }.let { ids ->
            find { CardTransactions.card inList ids }.orderBy(CardTransactions.timestamp to SortOrder.DESC).limit(count)
        }

        fun findInPeriod(account: BankAccount, range: ClosedRange<Instant>) = with(CardTransactions) {
            find {
                (card inList account.cards.map(BankCard::id)) and (timestamp greaterEq range.start) and (timestamp lessEq range.endInclusive)
            }.orderBy(timestamp to SortOrder.DESC)
        }
    }

    var reference by CardTransactions.reference
    var card by BankCard referencedOn CardTransactions.card
    var amount by CardTransactions.amount
    var currency by CardTransactions.currency
    var timestamp by CardTransactions.timestamp
    var details by CardTransactions.details
    var title by CardTransactions.title

    fun serializable() = SCardTransaction(
        reference, card.bankAccountId.value, card.id.value, amount.toDouble(), currency, timestamp, title, details
    )
}

fun SizedIterable<CardTransaction>.serializable() = map(CardTransaction::serializable)

internal object CardTransactions : IntIdTable(columnName = "transaction_id") {
    val reference = long("reference_id").uniqueIndex()
    val card = reference("card_id", BankCards)
    val amount = amount("amount")
    val currency = currency("currency")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val details = varchar("details", 500)
    val title = varchar("title", 50)
}