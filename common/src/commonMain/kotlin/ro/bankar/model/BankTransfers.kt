package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
enum class SDirection { Sent, Received }

@Serializable
data class SBankTransfer(
    val direction: SDirection,

    val fullName: String,
    val iban: String,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val dateTime: LocalDateTime,
)

@Serializable
data class STransferRequest(
    val id: Int,
    val direction: SDirection,

    val firstName: String,
    val middleName: String?,
    val lastName: String,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val partyID: Int?,
    val dateTime: LocalDateTime,
)

/**
 * Send money to another user, by tag
 */
@Serializable
data class SSendMoney(
    val targetTag: String,
    val sourceAccountID: Int,

    val amount: Double,
    // For verification - as source account already has a currency
    val currency: Currency,

    val note: String
) {
    fun validate() = when {
        amount < 0 -> "amount"
        note.length > 200 -> "note"
        else -> null
    }
}

/**
 * Accept the money received into the specified account
 */
@Serializable
data class SAcceptMoney(
    val transferRequestID: Int,
    val accountID: Int
)