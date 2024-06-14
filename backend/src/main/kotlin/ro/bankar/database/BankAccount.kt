package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.not
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.banking.SCreditData
import ro.bankar.banking.calcIBANCheckDigits
import ro.bankar.currency
import ro.bankar.generateNumeric
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount
import java.math.BigDecimal

class BankAccount(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankAccount>(BankAccounts) {
        fun create(user: User, data: SNewBankAccount, creditData: SCreditData?): BankAccount {
            if (data.type == SBankAccountType.Credit && creditData == null)
                throw IllegalArgumentException("creditData cannot be null if account type is Credit")
            return BankAccount.new {
                this.user = user
                type = data.type
                currency = data.currency
                name = data.name.trim()
                color = data.color
                if (data.type != SBankAccountType.Credit) return@new
                interest = creditData!!.interest
                limit = data.creditAmount.toBigDecimal()
            }
        }
    }

    var user by User referencedOn BankAccounts.userID
    var iban by BankAccounts.iban
    var type by BankAccounts.type

    var balance by BankAccounts.balance
    val spendable get() = limit + balance
    var limit by BankAccounts.limit
    var currency by BankAccounts.currency
    var closed by BankAccounts.closed

    var name by BankAccounts.name
    var color by BankAccounts.color

    var interest by BankAccounts.interest

    val cards by BankCard referrersOn BankCards.bankAccount
    val transfers get() = BankTransfer.find { (BankTransfers.sender eq id) or ((BankTransfers.recipient eq id)) and (BankTransfers.party eq null) }
    val parties by Party referrersOn Parties.hostAccount

    /**
     * Returns a serializable object containing the data for this bank account
     */
    fun serializable(user: User) = SBankAccountData(
        cards.filter { !it.closed }.map(BankCard::serializable),
        transfers.orderBy(BankTransfers.timestamp to SortOrder.DESC).serializable(user),
        cards.flatMap { it.transactions.serializable() },
        parties.orderBy(Parties.timestamp to SortOrder.DESC).filter { it.completed }.previewSerializable(),
    )
}

/**
 * Converts a list of BankAccounts to a list of serializable objects
 */
fun SizedIterable<BankAccount>.serializable() = orderBy(BankAccounts.id to SortOrder.ASC).filter { !it.closed }.map {
    SBankAccount(it.id.value, it.iban, it.type, it.balance.toDouble(), it.limit.toDouble(), it.currency, it.name, it.color, it.interest)
}

internal object BankAccounts : IntIdTable(columnName = "bank_account_id") {
    val iban = varchar("iban", 34).uniqueIndex().clientDefault {
        val accountNumber = generateNumeric(16)
        val bankCode = "RBNK"
        val countryCode = "RO"
        val checkDigits = calcIBANCheckDigits(countryCode, bankCode, accountNumber)
        "$countryCode$checkDigits$bankCode$accountNumber"
    }
    val userID = reference("user_id", Users)
    val type = enumeration<SBankAccountType>("type")
    val balance = amount("balance").default(BigDecimal.ZERO)
    val limit = amount("limit").default(BigDecimal.ZERO)
    val currency = currency("currency")
    val closed = bool("closed").default(false)

    val name = varchar("name", 30)
    val color = integer("color")
    val interest = double("interest").default(0.0)

    init {
        check("closed_must_be_empty") { not(closed) or (balance eq BigDecimal.ZERO) }
    }
}
