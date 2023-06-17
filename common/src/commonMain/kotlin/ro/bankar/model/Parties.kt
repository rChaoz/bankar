package ro.bankar.model

import kotlinx.datetime.Instant
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
    val host: SPublicUser,
    val total: Double,
    val currency: Currency,
    val note: String,
    val self: SPartyMember,
    val members: List<SPartyMember>,
    /**
     * If the user receiving the information is a member with pending status, this will be the request ID.
     */
    val requestID: Int?,
)

@Serializable
data class SPartyMember(
    val profile: SPublicUser,
    val amount: Double,
    val status: Status,
    /**
     * The transfer for this party member. Only present if the user receiving the information is the host.
     */
    val transfer: SBankTransfer?
) {
    enum class Status {
        /**
         * Host is a separate status as the host is not an actual member of the party, so they don't have a status.
         */
        Host, Pending, Cancelled, Accepted
    }
}

@Serializable
data class SPartyPreview(
    val id: Int,
    val completed: Boolean,
    override val timestamp: Instant,
    val total: Double,
    val collected: Double,
    val currency: Currency,
    val note: String
) : STimestamped