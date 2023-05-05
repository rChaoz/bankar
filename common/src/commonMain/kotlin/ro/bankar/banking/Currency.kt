package ro.bankar.banking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Currency(val code: String) {
    @SerialName("EUR") EURO("EUR"),
    @SerialName("USD") US_DOLLAR("USD"),
    @SerialName("RON") ROMANIAN_LEU("RON");

    companion object {
        fun from(code: String) = values().first { it.code == code }
    }
}