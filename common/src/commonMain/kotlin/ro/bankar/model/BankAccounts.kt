package ro.bankar.model

import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency
import ro.bankar.banking.SCreditData

@Serializable
enum class SBankAccountType { Debit, Savings, Credit }

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
    val creditAmount: Double,
) {
    companion object {
        val nameLengthRange = 2..30
    }

    fun validate(creditData: List<SCreditData>): String? {
        val data = creditData.find { it.currency == currency }
        return when {
            name.trim().length !in nameLengthRange -> "name"
            type != SBankAccountType.Credit -> null
            data == null -> "credit"
            creditAmount !in data.amountRange -> "credit-amount"
            else -> null
        }
    }
}
