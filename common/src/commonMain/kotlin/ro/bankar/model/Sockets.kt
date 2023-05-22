package ro.bankar.model

import kotlinx.serialization.Serializable

@Serializable
sealed class SSocketNotification {
    @Serializable
    class SMessageNotification(val fromTag: String): SSocketNotification()

    @Serializable
    object STransferNotification: SSocketNotification()

    @Serializable
    object SFriendNotification: SSocketNotification()

    @Serializable
    object SRecentActivityNotification: SSocketNotification()
}