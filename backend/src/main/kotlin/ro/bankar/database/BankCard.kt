package ro.bankar.database

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.date
import ro.bankar.amount

@Serializable
data class SBankCard(
    val id: Int,
    val number: String?,
    val lastFour: String,
    val pin: String?,
    val expirationMonth: Int?,
    val expirationYear: Int?,
    val cvv: String?,
    val limit: Double,
    val limitCurrent: Double,
    val limitReset: LocalDate,
    val transaction: List<SCardTransaction>,
)

class BankCard(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankCard>(BankCards) {
        fun find(id: Int, accountID: Int)  = find { (BankCards.id eq id) and (BankCards.bankAccount eq accountID) }.firstOrNull()
    }

    var bankAccount by BankAccount referencedOn BankCards.bankAccount

    var cardNumber by BankCards.cardNumber
    var pin by BankCards.pin
    var expiration by BankCards.expiration
    var cvv by BankCards.cvv
    var limit by BankCards.limit
    var limitCurrent by BankCards.limitCurrent
    var limitReset by BankCards.limitReset

    val transactions by CardTransaction referrersOn CardTransactions.card

    /**
     * Returns a serializable object containg the data for this bank account.
     * @param includeSensitive Whether to include sensitive information, such as card number, PIN, expiration date or CVV
     */
    fun serializable(includeSensitive: Boolean = false) = if (includeSensitive) SBankCard(
        id.value,
        cardNumber.toString(),
        cardNumber.toString().takeLast(4),
        pin.toString(),
        expiration.monthNumber,
        expiration.year,
        cvv.toString(),
        limit.toDouble(),
        limitCurrent.toDouble(),
        limitReset,
        transactions.serializable(),
    ) else SBankCard(
        id.value,
        null,
        cardNumber.toString().takeLast(4),
        null,
         null,
        null,
        null,
        limit.toDouble(),
        limitCurrent.toDouble(),
        limitReset,
        transactions.serializable(),
    )
}

internal object BankCards : IntIdTable(columnName = "card_id") {
    val bankAccount = reference("bank_account_id", BankAccounts)
    val cardNumber = decimal("card_number", 16, 0).uniqueIndex()
    val pin = decimal("pin", 4, 0)
    val expiration = date("expiration")
    val cvv = decimal("cvv", 3, 0)
    val limit = amount("limit")
    val limitCurrent = amount("limit_current")
    val limitReset = date("limit_reset")
}