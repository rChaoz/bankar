package ro.bankar.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ro.bankar.banking.Currency

@Serializable
enum class SDirection { Sent, Received }

@Serializable
data class SBankTransfer(
    /**
     * If null, this is a transfer between an user's own accounts
     */
    val direction: SDirection?,
    val accountID: Int,
    /**
     * Non-null if [direction] is null.
     */
    val sourceAccountID: Int?,

    val user: SPublicUser?,
    val fullName: String,
    val iban: String,
    /**
     * If this transfer is not associated with a party, this is null.
     */
    val partyID: Int?,

    val amount: Double,
    /**
     * If this transfer was exchanged on arrival, this represent the amount that was received by the recipient,
     * and [amount] represents the amount that was sent.
     */
    val exchangedAmount: Double?,
    val currency: Currency,
    /**
     * If the transfer was exchanged on arrival ([exchangedAmount]` != null`), this will be the currency of the destination account.
     * Otherwise, this will be the same as [currency].
     */
    val exchangedCurrency: Currency,
    val note: String,

    override val timestamp: Instant,
) : STimestamped {
    /**
     * Amount that is relevant to the user: sent amount/[amount] if direction is `Sent`; received amount/[exchangedAmount] if direction is `Received`.
     * This will be the exchanged amount if this is a self-transfer (exchange operation).
     */
    @Transient val relevantAmount = if (direction == SDirection.Sent) amount else (exchangedAmount ?: amount)

    /**
     * Currency associated with [relevantAmount]
     */
    @Transient val relevantCurrency = if (direction == SDirection.Sent) currency else exchangedCurrency
}

@Serializable
data class STransferRequest(
    val id: Int,
    val direction: SDirection,

    val user: SPublicUser,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val partyID: Int?,
    override val timestamp: Instant,
) : STimestamped

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