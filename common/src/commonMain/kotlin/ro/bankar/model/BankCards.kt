package ro.bankar.model

import kotlinx.datetime.Month
import kotlinx.serialization.Serializable

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
    val transactions: List<SCardTransaction>,
)

@Serializable
data class SNewBankCard(val name: String) {
    fun validate() = if (name.length in 2..30) null else "name"
}