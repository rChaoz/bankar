package ro.bankar.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SCountry(
    val country: String,
    @SerialName("dial_code") val dialCode: String,
    val code: String,
    val states: List<String>
)

typealias SCountries = List<SCountry>