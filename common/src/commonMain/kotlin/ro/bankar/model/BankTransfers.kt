package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
data class SBankTransfer(
    val senderName: String,
    val senderIban: String,
    val recipientName: String,
    val recipientIban: String,
    val amount: Double,
    val currency: Currency,
    val date: LocalDateTime,
)