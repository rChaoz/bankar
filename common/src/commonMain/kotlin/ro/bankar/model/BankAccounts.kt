package ro.bankar.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currencies

@Serializable
enum class SBankAccountType { DEBIT, SAVINGS, CREDIT }

@Serializable
data class SBankAccount(
    val id: Int,
    val iban: String,
    val type: SBankAccountType,
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
data class SNewBankAccount(
    val type: SBankAccountType,
    val name: String,
    val color: Int,
    val currency: String,
) {
    fun validate() = when {
        name.length !in 2..30 -> "name"
        Currencies.values().none { it.code == currency } -> "currency"
        else -> null
    }
}