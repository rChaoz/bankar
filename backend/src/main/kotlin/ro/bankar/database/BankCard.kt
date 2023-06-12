package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.date
import ro.bankar.amount
import ro.bankar.generateNumeric
import ro.bankar.model.SBankCard
import ro.bankar.model.SNewBankCard

class BankCard(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankCard>(BankCards) {
        fun find(id: Int, accountID: Int) = find { (BankCards.id eq id) and (BankCards.bankAccount eq accountID) }.firstOrNull()

        fun create(data: SNewBankCard, account: BankAccount) = BankCard.new {
            name = data.name
            bankAccount = account
        }
    }

    var bankAccount by BankAccount referencedOn BankCards.bankAccount

    var name by BankCards.name
    var cardNumber by BankCards.cardNumber
    var pin by BankCards.pin
    var expiration by BankCards.expiration
    var cvv by BankCards.cvv
    var limit by BankCards.limit
    var limitCurrent by BankCards.limitCurrent

    val transactions by CardTransaction referrersOn CardTransactions.card

    /**
     * Returns a serializable object containg the data for this bank account.
     * @param includeSensitive Whether to include sensitive information, such as card number, PIN, expiration date or CVV
     */
    fun serializable(includeSensitive: Boolean = false) = if (includeSensitive) SBankCard(
        id.value,
        name,
        cardNumber.toString(),
        cardNumber.toString().takeLast(4),
        pin.toString(),
        expiration.month,
        expiration.year,
        cvv.toString(),
        limit.toDouble(),
        limitCurrent.toDouble(),
        transactions.serializable(),
    ) else SBankCard(
        id.value,
        name,
        null,
        cardNumber.toString().takeLast(4),
        null,
        null,
        null,
        null,
        limit.toDouble(),
        limitCurrent.toDouble(),
        transactions.serializable(),
    )
}

internal object BankCards : IntIdTable(columnName = "card_id") {
    val bankAccount = reference("bank_account_id", BankAccounts)
    val name = varchar("name", 30)
    val cardNumber = decimal("card_number", 16, 0).uniqueIndex().clientDefault { generateNumeric(16).toBigDecimal() }
    val pin = decimal("pin", 4, 0).clientDefault { generateNumeric(4).toBigDecimal() }
    val expiration = date("expiration").clientDefault { Clock.System.todayIn(TimeZone.UTC) + DatePeriod(years = 5) }
    val cvv = decimal("cvv", 3, 0).clientDefault { generateNumeric(3).toBigDecimal() }
    val limit = amount("limit").default(0.toBigDecimal())
    val limitCurrent = amount("limit_current").default(0.toBigDecimal())
}