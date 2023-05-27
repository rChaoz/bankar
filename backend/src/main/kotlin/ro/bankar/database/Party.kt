package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import ro.bankar.amount
import java.math.BigDecimal

class Party(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Party>(Parties) {
        fun create(note: String, targetAccount: BankAccount, data: List<Pair<User, BigDecimal>>) {
            // Create party
            val total = data.sumOf { it.second }
            val party = new {
                this.total = total
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
    }

    var hostAccount by BankAccount referencedOn Parties.hostAccount
    var total by Parties.total
    val members by PartyMember referrersOn PartyMembers.party
}

internal object Parties : IntIdTable() {
    val hostAccount = reference("host", BankAccounts)
    val total = amount("total")
    val note = varchar("note", 100)
}
