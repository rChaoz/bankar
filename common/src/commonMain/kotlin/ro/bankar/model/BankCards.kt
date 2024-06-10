package ro.bankar.model

import kotlinx.datetime.Month
import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
data class SBankCard(
    val id: Int,
    val name: String,
    val number: String?,
    val lastFour: String,
    val pin: String?,
    val expirationMonth: Month?,
    val expirationYear: Int?,
    val cvv: String?,
    val limit: Double,
    val limitCurrent: Double,
    val currency: Currency,
    val transactions: List<SCardTransaction>,
)

@Serializable
data class SNewBankCard(val name: String, val limit: Double) {
    fun validate(updating: Boolean = false) = when {
        !(name.length in 2..30 || (updating && name.isEmpty())) -> "name"
        !(limit in 0.0..1e15 || (updating && limit == -1.0)) -> "limit"
        else -> null
    }
}