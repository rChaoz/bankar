package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class SCardTransaction(
    val reference: Long,
    val cardID: Int,
    val cardLastFour: String,
    val amount: Double,
    val currency: String,
    val dateTime: LocalDateTime,
    val details: String,
)