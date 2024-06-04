package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import ro.bankar.amount
import ro.bankar.model.SPartyInformation
import ro.bankar.model.SPartyMember
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

        fun byUserCompleted(user: User) = find { (Parties.hostAccount inList user.bankAccountIds) and (Parties.completed eq true) }
    }

    var hostAccount by BankAccount referencedOn Parties.hostAccount
    var total by Parties.total
    var note by Parties.note
    val members by PartyMember referrersOn PartyMembers.party
    var completed by Parties.completed
    var timestamp by Parties.timestamp

    /**
     * Cancel the party and delete all pending requests. If there are no accepted requests, the party will be deleted.
     * @return true if the party was cancelled (wasn't already completed)
     */
    fun cancel(): Boolean {
        if (completed) return false
        var hasAccepted = false
        for (member in members) {
            member.request?.decline()
            if (member.transfer != null) hasAccepted = true
        }
        // This will cascade delete all party members
        if (!hasAccepted) delete()
        else completed = true
        return true
    }

    fun serializable(user: User): SPartyInformation {
        val (selfList, others) = members.partition { it.user.id == user.id }
        val self = selfList.firstOrNull()
        val host = hostAccount.user
        return SPartyInformation(
            host.publicSerializable(host.hasFriend(user)), total.toDouble(), hostAccount.currency, note,
            self?.serializable(user, false) ?: SPartyMember(
                host.publicSerializable(false),
                0.0, SPartyMember.Status.Host, null
            ), others.serializable(user, user.id == host.id), self?.request?.id?.value
        )
    }

    fun previewSerializable() = SPartyPreview(
        id.value, completed, timestamp, total.toDouble(), members.filter { it.transfer != null }.sumOf { it.amount }.toDouble(), hostAccount.currency, note
    )
}

fun SizedIterable<Party>.previewSerializable() = map(Party::previewSerializable)

internal object Parties : IntIdTable() {
    val hostAccount = reference("host", BankAccounts)
    val total = amount("total")
    val note = varchar("note", 100)

    // If true, the party is completed and there are no more pending requests
    // "Completed" doesn't mean that all requests were accepted. A completed party will appear in the host's history.
    val completed = bool("completed").default(false)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
}
