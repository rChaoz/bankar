package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.banking.Currency
import ro.bankar.currency
import ro.bankar.model.SDirection
import ro.bankar.model.STransferRequest

class TransferRequest(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TransferRequest>(TransferRequests) {
        fun findRecent(user: User, count: Int) =
            find { (TransferRequests.sourceUser eq user.id) or (TransferRequests.targetUser eq user.id) }
                .orderBy(TransferRequests.dateTime to SortOrder.DESC).limit(count)
    }

    var sourceUser by User referencedOn TransferRequests.sourceUser
    var targetUser by User referencedOn TransferRequests.targetUser

    var note by TransferRequests.note
    private val partyID by TransferRequests.party
    var party by Party optionalReferencedOn TransferRequests.party
    var amount by TransferRequests.amount
    private var currencyString by TransferRequests.currency
    var currency: Currency
        get() = Currency.from(currencyString)
        set(value) {
            currencyString = value.code
        }
    var dateTime by TransferRequests.dateTime

    /**
     * Converts this TransferRequest to a serializable object.
     * @param direction Whether this transfer request was sent/received by the user
     */
    fun serializable(direction: SDirection) = when (direction) {
        SDirection.Sent -> targetUser
        SDirection.Received -> sourceUser
    }.let { STransferRequest(direction, it.firstName, it.middleName, it.lastName, amount.toDouble(), currency, note, partyID?.value, dateTime) }

    /**
     * Converts this TransferRequest to a serializable object.
     * @param user The user this transfer data will be given to, used to deduce whether this is sent/received transfer
     */
    fun serializable(user: User) =
        serializable(if (sourceUser.id == user.id) SDirection.Received else  SDirection.Sent)
}

fun SizedIterable<TransferRequest>.serializable(user: User) = map { it.serializable(user) }

internal object TransferRequests : IntIdTable(columnName = "transfer_req_id") {
    val sourceUser = reference("source_user_id", Users)
    val targetUser = reference("target_user_id", Users)

    val note = varchar("note", 100)
    val party = reference("party", Parties).nullable()
    val amount = amount("amount")
    val currency = currency("currency")
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
}