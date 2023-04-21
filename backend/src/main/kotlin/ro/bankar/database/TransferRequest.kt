package ro.bankar.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import ro.bankar.amount
import ro.bankar.currency

@Serializable
data class STransferRequest(
    val direction: Direction,
    val firstName: String,
    val middleName: String?,
    val lastName: String,
    val amount: Double,
    val currency: String,
    val partyID: Int?,
) {
    @Serializable
    enum class Direction { SENT, RECEIVED }
}

class TransferRequest(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TransferRequest>(TransferRequests)

    var sourceUser by User referencedOn TransferRequests.sourceUser
    var targetUser by User referencedOn TransferRequests.targetUser

    private val partyID by TransferRequests.party
    var party by Party optionalReferencedOn TransferRequests.party
    var amount by TransferRequests.amount
    var currency by TransferRequests.currency

    /**
     * Converts this TransferRequest to a serializable object.
     * @param direction Whether this transfer request was sent/received by the user
     */
    fun serializable(direction: STransferRequest.Direction) = when (direction) {
        STransferRequest.Direction.SENT -> targetUser
        STransferRequest.Direction.RECEIVED -> sourceUser
    }.let { STransferRequest(direction, it.firstName, it.middleName, it.lastName, amount.toDouble(), currency, partyID?.value) }
}

internal object TransferRequests : IntIdTable(columnName = "transfer_req_id") {
    val sourceUser = reference("source_user_id", Users)
    val targetUser = reference("target_user_id", Users)

    val party = reference("party", Parties).nullable()
    val amount = amount("amount")
    val currency = currency("currency")
}