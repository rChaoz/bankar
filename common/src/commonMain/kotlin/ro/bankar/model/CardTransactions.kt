package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
data class SCardTransaction(
    val reference: Long,
    val cardID: Int,
    val cardLastFour: String,

    val amount: Double,
    val currency: Currency,
    val dateTime: LocalDateTime,

    val title: String,
    val details: String,
)