package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import ro.bankar.amount

class PartyMember(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PartyMember>(PartyMembers)

    val party by Party referencedOn PartyMembers.party
    val user by User referencedOn PartyMembers.user
    val amount by PartyMembers.amount
    val transfer by BankTransfer optionalReferencedOn PartyMembers.transfer
}

internal object PartyMembers : IntIdTable() {
    val party = reference("party", Parties)
    val user = reference("user_id", Users)
    val amount = amount("user_amount")
    val transfer = reference("transfer", BankTransfers).nullable()
}