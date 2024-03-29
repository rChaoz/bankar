package ro.bankar.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable


@Serializable
data class SUserMessage(
    val direction: SDirection,
    val message: String,
    val timestamp: Instant,
)

typealias SConversation = List<SUserMessage>

@Serializable
data class SMessagingToken(val token: String)

@Serializable
data class SSendMessage(
    val message: String,
    val recipientTag: String
) {
    companion object {
        const val maxLength = 500
    }

    fun validate() = if (message.length > maxLength || message.isBlank()) "message" else null
}