package ro.bankar.database

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.date

@Serializable
data class SBankAccount(
    val iban: String,
    val type: BankAccount.Type,
    val balance: Double,
    val currency: String,
    val name: String,
    val color: Int,
    val interest: Double,
    val interestDate: LocalDate
)

class BankAccount(id: EntityID<Int>) : IntEntity(id) {
    @Serializable
    enum class Type { DEBIT, SAVINGS, CREDIT }

    companion object : IntEntityClass<BankAccount>(BankAccounts)

    var user by User referencedOn BankAccounts.userID
    var iban by BankAccounts.iban
    var type by BankAccounts.type

    var balance by BankAccounts.balance
    var currency by BankAccounts.currency

    var name by BankAccounts.name
    var color by BankAccounts.color

    var interest by BankAccounts.interest
    var interestDate by BankAccounts.interestDate

    val cards by BankCard referrersOn BankCards.bankAccount

    /**
     * Returns a serializable object containg the data for this bank account.
     */
    fun serializable() = SBankAccount(iban, type, balance.toDouble(), currency, name, color, interest, interestDate)
}

internal object BankAccounts : IntIdTable(columnName = "bank_account_id") {
    val iban = varchar("iban", 34).uniqueIndex()
    val userID = reference("user_id", Users.id)
    val type = enumeration<BankAccount.Type>("type")
    val balance = decimal("balance", 20, 2)
    val currency = varchar("currency", 7)

    val name = varchar("name", 30)
    val color = integer("color")
    val interest = double("interest").default(0.0)
    val interestDate = date("interest_date").defaultExpression(CurrentDate)
}