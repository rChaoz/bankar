package ro.bankar.model

import kotlinx.serialization.Serializable

@Serializable
class SCreateParty(
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