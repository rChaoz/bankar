package ro.bankar.model

import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
enum class SBankAccountType { DEBIT, SAVINGS, CREDIT }

@Serializable
data class SBankAccount(
    val id: Int,
    val iban: String,
    val type: SBankAccountType,
    val balance: Double,
    val limit: Double,
    val currency: Currency,
    val name: String,
    val color: Int,
    val interest: Double,
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
    val currency: Currency,
) {
    companion object {
        val nameLengthRange = 2..30
    }

    fun validate() = if (name.trim().length !in nameLengthRange) "name" else null
}