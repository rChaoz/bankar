package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
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
                (sourceUser eq user.id) and (targetUser eq otherUser.id) or (targetUser eq user.id) and (sourceUser eq otherUser.id)
            }
        }
    }

    var sourceUser by User referencedOn UserMessages.sourceUser
    var targetUser by User referencedOn UserMessages.targetUser

    var message by UserMessages.message
    var dateTime by UserMessages.dateTime

    fun serializable(direction: SDirection) = SUserMessage(direction, message, dateTime)
}

fun SizedIterable<UserMessage>.serializable(user: User) = map { it.serializable(if (it.sourceUser == user) SDirection.Sent else SDirection.Received) }

internal object UserMessages : IntIdTable() {
    val sourceUser = reference("source_user_id", Users.id)
    val targetUser = reference("target_user_id", Users.id)

    val message = varchar("message", 500)
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
}