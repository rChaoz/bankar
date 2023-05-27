package ro.bankar.model

import kotlinx.serialization.Serializable

@Serializable
data class SRecentActivity(
    val transfers: List<SBankTransfer>,
    val transactions: List<SCardTransaction>,
    val parties: List<SPartyPreview>,
    val transferRequests: List<STransferRequest>,
) {
    fun isEmpty() = transfers.isEmpty() && transactions.isEmpty() && transferRequests.isEmpty()
}