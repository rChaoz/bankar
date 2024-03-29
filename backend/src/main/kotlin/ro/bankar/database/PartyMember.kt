package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import ro.bankar.amount
import ro.bankar.model.SPartyMember

class PartyMember(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PartyMember>(PartyMembers)

    var party by Party referencedOn PartyMembers.party
    var user by User referencedOn PartyMembers.user
    var amount by PartyMembers.amount
    var transfer by BankTransfer optionalReferencedOn PartyMembers.transfer
    var request by TransferRequest optionalReferencedOn PartyMembers.request

    fun serializable(user: User, includeTransfer: Boolean) = SPartyMember(
        this.user.publicSerializable(user.hasFriend(this.user)), amount.toDouble(), when {
            transfer != null -> SPartyMember.Status.Accepted
            request != null -> SPartyMember.Status.Pending
            else -> SPartyMember.Status.Cancelled
        }, if (!includeTransfer) null else transfer?.serializable(user)
    )
}

fun Iterable<PartyMember>.serializable(user: User, includeTransfers: Boolean) = map { it.serializable(user, includeTransfers) }

internal object PartyMembers : IntIdTable() {
    val party = reference("party", Parties, onDelete = ReferenceOption.CASCADE)
    val user = reference("user_id", Users)
    val amount = amount("user_amount")
    val request = reference("request", TransferRequests, onDelete = ReferenceOption.SET_NULL).nullable()
    val transfer = reference("transfer", BankTransfers).nullable()
}