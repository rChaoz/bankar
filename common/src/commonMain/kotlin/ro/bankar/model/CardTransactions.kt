package ro.bankar.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
data class SCardTransaction(
    val reference: Long,
    val cardID: Int,
    val accountID: Int,
    val cardLastFour: String,

    val amount: Double,
    val currency: Currency,
    override val timestamp: Instant,

    val title: String,
    val details: String,
) : STimestamped