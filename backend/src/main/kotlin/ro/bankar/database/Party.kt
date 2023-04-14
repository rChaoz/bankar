package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

class Party(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Party>(Parties)

    val host by User referencedOn Parties.host
    val total by Parties.total
    val currency by Parties.currency
    val members by PartyMember referrersOn PartyMembers.party
}

class PartyMember(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PartyMember>(PartyMembers)

    val party by Party referencedOn PartyMembers.party
    val user by User referencedOn PartyMembers.userID
    val amount by PartyMembers.amount
    val transfer by BankTransfer optionalReferencedOn PartyMembers.transfer
}

internal object Parties : IntIdTable() {
    val host = reference("host", Users)
    val total = amount("total")
    val currency = currency("currency")
}

internal object PartyMembers : IntIdTable() {
    val party = reference("party", Parties)
    val userID = reference("user_id", Users)
    val amount = amount("user_amount")
    val transfer = reference("transfer", BankTransfers).nullable()
}