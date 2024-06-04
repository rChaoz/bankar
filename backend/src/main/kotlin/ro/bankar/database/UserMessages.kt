package ro.bankar.database

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.or
import ro.bankar.model.SDirection
import ro.bankar.model.SUserMessage

class UserMessage(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserMessage>(UserMessages) {
        fun create(from: User, to: User, message: String) = new {
            sourceUser = from
            targetUser = to
            this.message = message.trim()
        }

        fun getConversationBetween(user: User, otherUser: User) = with(UserMessages) {
            find {
                ((sourceUser eq user.id) and (targetUser eq otherUser.id)) or ((targetUser eq user.id) and (sourceUser eq otherUser.id))
            }.orderBy(timestamp to SortOrder.DESC)
        }

        fun getConversationBetweenSince(user: User, otherUser: User, since: Instant) = with(UserMessages) {
            find {
                (((sourceUser eq user.id) and (targetUser eq otherUser.id)) or ((targetUser eq user
                    .id) and (sourceUser eq otherUser.id))) and (timestamp greater since)
            }.orderBy(timestamp to SortOrder.DESC)
        }
    }

    var sourceUser by User referencedOn UserMessages.sourceUser
    var targetUser by User referencedOn UserMessages.targetUser

    var message by UserMessages.message
    var timestamp by UserMessages.timestamp

    fun serializable(direction: SDirection) = SUserMessage(direction, message, timestamp)

    fun serializable(user: User) = serializable(if (sourceUser.id == user.id) SDirection.Sent else SDirection.Received)
}

fun SizedIterable<UserMessage>.serializable(user: User) = map { it.serializable(user) }

internal object UserMessages : IntIdTable() {
    val sourceUser = reference("source_user_id", Users.id)
    val targetUser = reference("target_user_id", Users.id)

    val message = varchar("message", 500)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
}