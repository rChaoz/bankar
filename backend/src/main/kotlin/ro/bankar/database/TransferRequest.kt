package ro.bankar.database

import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.model.SDirection
import ro.bankar.model.STransferRequest
import ro.bankar.util.nowUTC
import java.math.BigDecimal

class TransferRequest(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TransferRequest>(TransferRequests) {
        fun findRecent(user: User) =
            find { (TransferRequests.sourceUser eq user.id) or (TransferRequests.targetUser eq user.id) }
                .orderBy(TransferRequests.dateTime to SortOrder.DESC)

        fun create(sourceAccount: BankAccount, target: User, amount: BigDecimal, note: String, party: Party? = null): Boolean {
            if (sourceAccount.spendable < amount) return false
            // Lock funds
            if (amount > BigDecimal.ZERO) sourceAccount.balance -= amount
            new {
                this.sourceAccount = sourceAccount
                sourceUser = sourceAccount.user
                targetUser = target
                this.party = party
                this.amount = amount
                this.note = note.trim()
            }
            return true
        }
    }

    var sourceUser by User referencedOn TransferRequests.sourceUser
    var sourceAccount by BankAccount referencedOn TransferRequests.sourceAccount
    var targetUser by User referencedOn TransferRequests.targetUser

    private val partyID by TransferRequests.party
    var party by Party optionalReferencedOn TransferRequests.party
    var amount by TransferRequests.amount
    var dateTime by TransferRequests.dateTime
    var note by TransferRequests.note

    /**
     * Accepts this transfer request, transferring the amount into/from the given account
     */
    fun accept(account: BankAccount) = BankTransfer.transfer(this, account).also { if (it) delete() }

    /**
     * Accepts this transfer request, transferring the amount from source account, exchanging, and into target account
     */
    fun acceptExchanging(account: BankAccount) = BankTransfer.transferExchanging(this, account).also { if (it) delete() }

    /**
     * Declines this transfer request, released the locked funds if any
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
        val user = if (direction == SDirection.Sent) targetUser else sourceUser
        val otherUser = if (direction == SDirection.Sent) sourceUser else targetUser
        return STransferRequest(id.value, direction, otherUser.publicSerializable(user.hasFriend(otherUser)),
            amount.toDouble(), sourceAccount.currency, note, partyID?.value, dateTime)
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
    val party = reference("party", Parties).nullable().default(null)
    val amount = amount("amount").check("amount_check") { it greater BigDecimal.ZERO }
    val dateTime = datetime("datetime").clientDefault { Clock.System.nowUTC() }
}