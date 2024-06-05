package ro.bankar.model

import kotlinx.serialization.Serializable

@Serializable
sealed class SSocketNotification {
    @Serializable
    data class SMessageNotification(val fromTag: String): SSocketNotification()

    @Serializable
    data object STransferNotification: SSocketNotification()

    @Serializable
    data object SFriendNotification: SSocketNotification()

    @Serializable
    data object SRecentActivityNotification: SSocketNotification()
}