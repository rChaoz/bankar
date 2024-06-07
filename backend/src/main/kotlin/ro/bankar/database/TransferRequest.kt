package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import ro.bankar.amount
import ro.bankar.model.SDirection
import ro.bankar.model.STransferRequest
import java.math.BigDecimal

class TransferRequest(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TransferRequest>(TransferRequests) {
        fun findRecent(user: User) = with(TransferRequests) {
            find {
                ((sourceUser eq user.id) and partyMember.isNull()) or (targetUser eq user.id)
            }.orderBy(timestamp to SortOrder.DESC)
        }

        fun create(sourceAccount: BankAccount, target: User, amount: BigDecimal, note: String, partyMember: PartyMember? = null): TransferRequest? {
            if (sourceAccount.spendable < amount) return null
            // Lock funds
            if (amount > BigDecimal.ZERO) sourceAccount.balance -= amount
            return new {
                this.sourceAccount = sourceAccount
                sourceUser = sourceAccount.user
                targetUser = target
                this.partyMember = partyMember
                this.amount = amount
                this.note = note.trim()
            }
        }
    }

    var sourceUser by User referencedOn TransferRequests.sourceUser
    var sourceAccount by BankAccount referencedOn TransferRequests.sourceAccount
    var targetUser by User referencedOn TransferRequests.targetUser

    var partyMember by PartyMember optionalReferencedOn TransferRequests.partyMember
    var amount by TransferRequests.amount
    var timestamp by TransferRequests.timestamp
    var note by TransferRequests.note

    /**
     * Accepts this transfer request, transferring the amount into/from the given account
     */
    fun accept(account: BankAccount) = BankTransfer.transfer(this, account)

    /**
     * Accepts this transfer request, transferring the amount from source account, exchanging, and into target account
     */
    fun acceptExchanging(account: BankAccount) = BankTransfer.transferExchanging(this, account)

    /**
     * Declines this transfer request, releasing the locked funds if any
     */
    fun decline() {
        if (amount > BigDecimal.ZERO) sourceAccount.balance += amount
        delete()
    }

    /**
     * Converts this TransferRequest to a serializable object.
     * @param direction Whether this transfer request was sent/received by the user
     */
    fun serializable(direction: SDirection): STransferRequest {
        val user = if (direction == SDirection.Sent) sourceUser else targetUser
        val otherUser = if (direction == SDirection.Sent) targetUser else sourceUser
        return STransferRequest(id.value, direction, otherUser.publicSerializable(user.hasFriend(otherUser)),
            amount.toDouble(), sourceAccount.currency, note, partyMember?.party?.id?.value, timestamp)
    }

    /**
     * Converts this TransferRequest to a serializable object.
     * @param user The user this transfer data will be given to, used to deduce whether this is sent/received transfer
     */
    fun serializable(user: User) =
        serializable(if (targetUser.id == user.id) SDirection.Received else SDirection.Sent)
}

fun SizedIterable<TransferRequest>.serializable(user: User) = map { it.serializable(user) }

internal object TransferRequests : IntIdTable(columnName = "transfer_req_id") {
    val sourceUser = reference("source_user_id", Users)
    val sourceAccount = reference("source_account_id", BankAccounts)
    val targetUser = reference("target_user_id", Users)

    val note = varchar("note", 100)
    val partyMember = reference("party_member", PartyMembers, onDelete = ReferenceOption.CASCADE).nullable()
    val amount = amount("amount")
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
}