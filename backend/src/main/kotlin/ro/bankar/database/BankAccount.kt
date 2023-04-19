package ro.bankar.database

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.banking.Currencies
import ro.bankar.currency
import ro.bankar.generateNumeric

@Serializable
data class SBankAccount(
    val id: Int,
    val iban: String,
    val type: BankAccount.Type,
    val balance: Double,
    val limit: Double,
    val currency: String,
    val name: String,
    val color: Int,
    val interest: Double,
    val interestDate: LocalDate,
)

@Serializable
data class SBankAccountData(
    val cards: List<SBankCard>,
    val transfers: List<SBankTransfer>,
    val transactions: List<SCardTransaction>,
)

@Serializable
data class SNewAccount(
    val type: BankAccount.Type,
    val name: String,
    val color: Int,
    val currency: String,
) {
    fun validate() = when {
        name.length < 2 || name.length > 30 -> "name"
        Currencies.values().none { it.code == currency } -> "currency"
        else -> null
    }
}

class BankAccount(id: EntityID<Int>) : IntEntity(id) {
    @Serializable
    enum class Type { DEBIT, SAVINGS, CREDIT }

    companion object : IntEntityClass<BankAccount>(BankAccounts) {
        fun create(user: User, data: SNewAccount) = BankAccount.new {
            this.user = user
            type = data.type
            currency = data.currency
            name = data.name
            color = data.color
            // TODO interest for credit accounts
        }
    }

    var user by User referencedOn BankAccounts.userID
    var iban by BankAccounts.iban
    var type by BankAccounts.type

    var balance by BankAccounts.balance
    var currency by BankAccounts.currency
    var limit by BankAccounts.limit

    var name by BankAccounts.name
    var color by BankAccounts.color

    var interest by BankAccounts.interest
    var interestDate by BankAccounts.interestDate

    val cards by BankCard referrersOn BankCards.bankAccount
    val transfers get() = BankTransfer.find { (BankTransfers.sender eq id) or (BankTransfers.recipient eq id) }

    /**
     * Returns a serializable object containg the data for this bank account
     */
    fun serializable() = SBankAccountData(
        cards.map(BankCard::serializable),
        transfers.map(BankTransfer::serializable),
        cards.flatMap { it.transactions.serializable() }
    )
}

/**
 * Converts a list of BankAccounts to a list of serializable objects
 */
fun SizedIterable<BankAccount>.serializable() = map {
    SBankAccount(it.id.value, it.iban, it.type, it.balance.toDouble(), it.limit.toDouble(), it.currency, it.name, it.color, it.interest, it.interestDate)
}

internal object BankAccounts : IntIdTable(columnName = "bank_account_id") {
    val iban = varchar("iban", 34).uniqueIndex().clientDefault {
        val accountNumber = generateNumeric(10)
        val bankCode = "192153"
        val countryCode = "RO"
        val checkDigits = 98 - ("$bankCode$accountNumber${countryCode.toInt(36)}00".toBigInteger() % 97.toBigInteger()).toInt()
        "$countryCode$checkDigits$bankCode$accountNumber"
    }
    val userID = reference("user_id", Users)
    val type = enumeration<BankAccount.Type>("type")
    val balance = amount("balance").default(0.0.toBigDecimal())
    val limit = amount("limit").default(0.0.toBigDecimal())
    val currency = currency("currency")

    val name = varchar("name", 30)
    val color = integer("color")
    val interest = double("interest").default(0.0)
    val interestDate = date("interest_date").defaultExpression(CurrentDate)
}
