package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.date
import ro.bankar.amount
import ro.bankar.banking.Currency
import ro.bankar.banking.reverseExchange
import ro.bankar.generateNumeric
import ro.bankar.model.SBankCard
import ro.bankar.model.SNewBankCard
import ro.bankar.secureRandom
import ro.bankar.util.nowHere
import java.math.BigDecimal

class BankCard(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankCard>(BankCards) {
        fun find(id: Int, accountID: Int) =
            find { (BankCards.id eq id) and (BankCards.bankAccount eq accountID) and (BankCards.closed eq false) }.firstOrNull()

        fun create(data: SNewBankCard, account: BankAccount) = BankCard.new {
            name = data.name
            bankAccount = account
        }

        /**
         * Finds a card by payment information.
         * @param cardNumber 14-16 digits, no spaces
         * @param expirationDate format MM/YY
         * @param securityCode 3-4 digits
         */
        fun findByPaymentInfo(cardNumber: String, expirationDate: String, securityCode: String): BankCard? {
            val card = find { (BankCards.cardNumber eq cardNumber) and (BankCards.closed eq false) }.firstOrNull() ?: return null
            if (card.cvv != securityCode) return null
            var (month, year) = expirationDate.split('/')
            year = if (year.toInt() > 50) "19$year" else "20$year"
            if (card.expiration <= Clock.System.todayIn(TimeZone.UTC)) return null
            if (card.expiration.monthNumber != month.toInt() || card.expiration.year != year.toInt()) return null
            return card
        }

        private val detailsDateTimeFormat = LocalDateTime.Format {
            dayOfMonth()
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            year()
            chars(", ")
            hour()
            char(':')
            minute()
        }
    }

    var bankAccountId by BankCards.bankAccount
    var bankAccount by BankAccount referencedOn BankCards.bankAccount

    var name by BankCards.name
    var cardNumber by BankCards.cardNumber
    var pin by BankCards.pin
    var expiration by BankCards.expiration
    var cvv by BankCards.cvv
    var limit by BankCards.limit
    var limitCurrent by BankCards.limitCurrent
    var closed by BankCards.closed

    /**
     * Only makes sense if [limit] is non-zero.
     */
    val remainingLimit get() = limit - limitCurrent

    val transactions by CardTransaction referrersOn CardTransactions.card

    /**
     * Tries to make a new payment, declining if not enough funds
     */
    fun pay(amount: BigDecimal, currency: Currency, title: String): Boolean {
        val realAmount =
            if (currency != bankAccount.currency) EXCHANGE_DATA.reverseExchange(bankAccount.currency, currency, amount) ?: return false
            else amount
        // Check balance
        if (realAmount > bankAccount.spendable) return false
        // Check limit
        if (limit.compareTo(BigDecimal.ZERO) != 0 && realAmount > remainingLimit) return false
        bankAccount.balance -= realAmount
        limitCurrent += realAmount
        CardTransaction.new {
            reference = secureRandom.nextLong().let { if (it < 0L) -(it + 1) else it }
            card = this@BankCard
            this.amount = realAmount
            this.currency = bankAccount.currency
            this.title = title
            details = "Payment on ${Clock.System.nowHere().format(detailsDateTimeFormat)} at $title"
        }
        return true
    }

    /**
     * Returns a serializable object containing the data for this bank account.
     * @param includeSensitive Whether to include sensitive information, such as card number, PIN, expiration date or CVV
     */
    fun serializable(includeSensitive: Boolean = false) = if (includeSensitive) SBankCard(
        id.value,
        name,
        cardNumber,
        cardNumber.takeLast(4),
        pin,
        expiration.month,
        expiration.year,
        cvv,
        limit.toDouble(),
        limitCurrent.toDouble(),
        bankAccount.currency,
        transactions.serializable(),
    ) else SBankCard(
        id.value,
        name,
        null,
        cardNumber.takeLast(4),
        null,
        null,
        null,
        null,
        limit.toDouble(),
        limitCurrent.toDouble(),
        bankAccount.currency,
        transactions.serializable(),
    )
}

internal object BankCards : IntIdTable(columnName = "card_id") {
    val bankAccount = reference("bank_account_id", BankAccounts)
    val name = varchar("name", 30)
    val cardNumber = varchar("card_number", 20).uniqueIndex().clientDefault { generateNumeric(16) }
    val pin = varchar("pin", 4).clientDefault { generateNumeric(4) }
    val expiration = date("expiration").clientDefault { Clock.System.todayIn(TimeZone.UTC) + DatePeriod(years = 5) }
    val cvv = varchar("cvv", 3).clientDefault { generateNumeric(3) }
    val limit = amount("limit").default(0.toBigDecimal())
    val limitCurrent = amount("limit_current").default(0.toBigDecimal())
    val closed = bool("is_closed").default(false)
}