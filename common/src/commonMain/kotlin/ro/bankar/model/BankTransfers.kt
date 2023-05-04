package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SBankTransfer(
    val senderName: String,
    val senderIban: String,
    val recipientName: String,
    val recipientIban: String,
    val amount: Double,
    val currency: String,
    val date: LocalDateTime,
)