package ro.bankar.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ro.bankar.banking.Currency
import ro.bankar.banking.SCreditData

@Serializable
enum class SBankAccountType(val title: String) {
    Debit("Debit"), Savings("Savings"), Credit("Credit");
}

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
) {
    // Because balance goes negative, but limit is positive, e.g. limit 500, balance -200 -> remaining 300
    /**
     * If credit account, this is the remaining credit. Otherwise, this is equal to balance.
     *
     * This represents the total remaining spendable amount.
     */
    @Transient val spendable = limit + balance
}

@Serializable
data class SBankAccountData(
    val cards: List<SBankCard>,
    val transfers: List<SBankTransfer>,
    val transactions: List<SCardTransaction>,
    val parties: List<SPartyPreview>
)

sealed class SBankAccountBase {
    abstract val name: String
    abstract val color: Int

    fun validate() = when {
        name.trim().length !in SNewBankAccount.nameLengthRange -> "name"
        color < 0 -> "color"
        else -> null
    }
}

@Serializable
data class SNewBankAccount(
    val type: SBankAccountType,
    override val name: String,
    override val color: Int,
    val currency: Currency,
    val creditAmount: Double,
): SBankAccountBase() {
    companion object {
        val nameLengthRange = 2..30
    }

    fun validate(creditData: List<SCreditData>) = super.validate() ?: run {
        val data = creditData.find { it.currency == currency }
        when {
            type != SBankAccountType.Credit -> null
            data == null -> "credit"
            creditAmount !in data.amountRange -> "credit-amount"
            else -> null
        }
    }
}

@Serializable
data class SCustomiseBankAccount(
    override val name: String,
    override val color: Int,
): SBankAccountBase()

@Serializable
data class SDefaultBankAccount(val id: Int?, val alwaysUse: Boolean)