package ro.bankar.model

import kotlinx.serialization.Serializable
import ro.bankar.banking.Currency

@Serializable
data class SCreateParty(
    val accountID: Int,
    val note: String,
    val amounts: List<Pair<String, Double>>
) {
    fun validate() = when {
        amounts.any { it.second <= 0.0 } -> "amounts"
        note.isBlank() || note.length > SSendRequestMoney.maxNoteLength -> "note"
        else -> null
    }
}

@Serializable
data class SPartyInformation(
    /**
     * Null if the user receiving the information is the host
     */
    val host: SPublicUser?,
    val total: Double,
    val currency: Currency,
    val note: String,
    val members: List<SPartyMember>
)

@Serializable
data class SPartyMember(
    val profile: SPublicUser,
    val amount: Double,
    val status: Status
) {
    enum class Status {
        Pending, Declined, Accepted
    }
}