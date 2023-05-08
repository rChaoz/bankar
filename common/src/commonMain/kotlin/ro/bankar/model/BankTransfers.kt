package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
enum class TransferDirection { Sent, Received }

@Serializable
data class SBankTransfer(
    val direction: TransferDirection,

    val fullName: String,
    val iban: String,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val dateTime: LocalDateTime,
)

@Serializable
data class STransferRequest(
    val direction: TransferDirection,

    val firstName: String,
    val middleName: String?,
    val lastName: String,

    val amount: Double,
    val currency: Currency,
    val note: String,

    val partyID: Int?,
    val dateTime: LocalDateTime,
)