package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import ro.bankar.amount
import ro.bankar.model.SPartyInformation
import ro.bankar.model.SPartyPreview
import java.math.BigDecimal

class Party(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Party>(Parties) {
        fun create(note: String, targetAccount: BankAccount, data: List<Pair<User, BigDecimal>>) {
            // Create party
            val total = data.sumOf { it.second }
            val party = new {
                this.total = total
                this.note = note
                hostAccount = targetAccount
            }
            // Create transfer requests & party member information
            for (pair in data) {
                val member = PartyMember.new {
                    this.party = party
                    this.user = pair.first
                    this.amount = pair.second
                }
                val transfer = TransferRequest.create(targetAccount, pair.first, -pair.second, note, member)!!
                member.request = transfer
            }
        }

        fun byUser(user: User) = find { Parties.hostAccount inList user.bankAccountIds }
    }

    var hostAccount by BankAccount referencedOn Parties.hostAccount
    var total by Parties.total
    var note by Parties.note
    val members by PartyMember referrersOn PartyMembers.party

    fun serializable(user: User) = SPartyInformation(
        hostAccount.user.takeIf { it.id != user.id }?.publicSerializable(true),
        total.toDouble(), hostAccount.currency, note, members.serializable(user)
    )

    fun previewSerializable() = SPartyPreview(
        id.value, total.toDouble(), members.filter { it.transfer != null }.sumOf { it.amount }.toDouble(), hostAccount.currency, note
    )
}

fun SizedIterable<Party>.previewSerializable() = map(Party::previewSerializable)

internal object Parties : IntIdTable() {
    val hostAccount = reference("host", BankAccounts)
    val total = amount("total")
    val note = varchar("note", 100)
}
