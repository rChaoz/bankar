package ro.bankar.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable


@Serializable
data class SUserMessage(
    val direction: SDirection,
    val message: String,
    val dateTime: LocalDateTime
)

@Serializable
data class SConversation(
    val user: SPublicUser,
    val messages: List<SUserMessage>
)

@Serializable
data class SSendMessage(
    val message: String,
    val recipientTag: String
) {
    fun validate() = if (message.trim().length > 500) "message" else null
}