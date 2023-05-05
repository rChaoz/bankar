package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.currency
import ro.bankar.generateNumeric
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount

class BankAccount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankAccount>(BankAccounts) {
        fun create(user: User, data: SNewBankAccount) = BankAccount.new {
            this.user = user
            type = data.type
            currency = data.currency
            name = data.name.trim()
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

    val cards by BankCard referrersOn BankCards.bankAccount
    val transfers get() = BankTransfer.find { (BankTransfers.sender eq id) or (BankTransfers.recipient eq id) }

    /**
     * Returns a serializable object containing the data for this bank account
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
    SBankAccount(it.id.value, it.iban, it.type, it.balance.toDouble(), it.limit.toDouble(), it.currency, it.name, it.color, it.interest)
}

internal object BankAccounts : IntIdTable(columnName = "bank_account_id") {
    val iban = varchar("iban", 34).uniqueIndex().clientDefault {
        val accountNumber = generateNumeric(16)
        val bankCode = "RBNK"
        val countryCode = "RO"
        val checkDigits = 98 - ("${bankCode.toInt(36)}$accountNumber${countryCode.toInt(36)}00".toBigInteger() % 97.toBigInteger()).toInt()
        "$countryCode$checkDigits$bankCode$accountNumber"
    }
    val userID = reference("user_id", Users)
    val type = enumeration<SBankAccountType>("type")
    val balance = amount("balance").default(0.0.toBigDecimal())
    val limit = amount("limit").default(0.0.toBigDecimal())
    val currency = currency("currency")

    val name = varchar("name", 30)
    val color = integer("color")
    val interest = double("interest").default(0.0)
}
