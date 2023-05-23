package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
enum class SDirection { Sent, Received }

@Serializable
data class SBankTransfer(
    val direction: SDirection,
    val accountID: Int,

    val user: SPublicUser?,
    val fullName: String,
    val iban: String,

    val amount: Double,
    /**
     * If this transfer was exchanged on arrival, this represent the amount that was received by the recipient, and [amount] represents the amount that was sent.
     */
    val exchangedAmount: Double?,
    val currency: Currency,
    /**
     * If the transfer was exchanged on arrival ([exchangedAmount]` != null`), this will be the currency of the destination account.
     * Otherwise, this will be the same as [currency].
     */
    val exchangedCurrency: Currency,
    val note: String,

    val dateTime: LocalDateTime,
)

@Serializable
data class STransferRequest(
    val id: Int,
    val direction: SDirection,

    val user: SPublicUser,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val partyID: Int?,
    val dateTime: LocalDateTime,
)

/**
 * Send or request money to/from another user, by tag
 */
@Serializable
data class SSendRequestMoney(
    val targetTag: String,
    /**
     * For requesting money, this is the requesting user's account that money will go into
     */
    val sourceAccountID: Int,

    val amount: Double,
    // For verification - as source account already has a currency
    val currency: Currency,

    val note: String
) {
    companion object {
        const val maxNoteLength = 200
    }

    fun validate() = when {
        amount <= 0 -> "amount"
        note.length > maxNoteLength -> "note"
        else -> null
    }
}