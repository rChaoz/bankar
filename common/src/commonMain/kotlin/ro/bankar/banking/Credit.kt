package ro.bankar.banking

import kotlinx.serialization.Serializable

@Serializable
data class SCreditData(
    val currency: Currency,
    val interest: Double,
    val minAmount: Double,
    val maxAmount: Double,
)