package ro.bankar.database

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.date

@Serializable
data class SBankCard(
    val number: String?,
    val lastFour: String,
    val pin: String?,
    val expirationMonth: Int?,
    val expirationYear: Int?,
    val cvv: String?,
    val limit: Double,
    val limitCurrent: Double,
    val limitReset: LocalDate,
)

class BankCard(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankCard>(BankCards)

    var bankAccount by BankAccount referencedOn BankCards.bankAccount
    var cardNumber by BankCards.cardNumber
    var pin by BankCards.pin
    var expiration by BankCards.expiration
    var cvv by BankCards.cvv
    var limit by BankCards.limit
    var limitCurrent by BankCards.limitCurrent
    var limitReset by BankCards.limitReset

    /**
     * Returns a serializable object containg the data for this bank account.
     * @param includeSensitive Whether to include sensitive information, such as card number, PIN, expiration date or CVV
     */
    fun serializable(includeSensitive: Boolean = false) = if (includeSensitive) SBankCard(
        cardNumber.toString(),
        cardNumber.toString().takeLast(4),
        pin.toString(),
        expiration.monthNumber,
        expiration.year,
        cvv.toString(),
        limit.toDouble(),
        limitCurrent.toDouble(),
        limitReset
    ) else SBankCard(
        null,
        cardNumber.toString().takeLast(4),
        null,
         null,
        null,
        null,
        limit.toDouble(),
        limitCurrent.toDouble(),
        limitReset
    )
}

object BankCards : IntIdTable(columnName = "card_id") {
    val bankAccount = reference("bank_account_id", BankAccounts.id)
    val cardNumber = decimal("card_number", 16, 0).uniqueIndex()
    val pin = decimal("pin", 4, 0)
    val expiration = date("expiration")
    val cvv = decimal("cvv", 3, 0)
    val limit = decimal("limit", 20, 2)
    val limitCurrent = decimal("limit_current", 20, 2)
    val limitReset = date("limit_reset")
}