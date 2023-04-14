package ro.bankar.database

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

@Serializable
data class SUserMessage(
    val message: String,
    val dateTime: LocalDateTime,
    val transferID: Int?,
)

class UserMessage(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserMessage>(UserMessages)

    var sourceUser by User referencedOn UserMessages.sourceUser
    var targetUser by User referencedOn UserMessages.targetUser

    var message by UserMessages.message
    var dateTime by UserMessages.dateTime
    private val transferID by UserMessages.transfer
    var transfer by BankTransfer optionalReferencedOn UserMessages.transfer

    fun serializable() = SUserMessage(message, dateTime, transferID?.value)
}

internal object UserMessages : IntIdTable() {
    val sourceUser = reference("source_user_id", Users.id)
    val targetUser = reference("target_user_id", Users.id)

    val message = varchar("message", 200)
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
    val transfer = reference("transfer", BankTransfers).nullable()
}