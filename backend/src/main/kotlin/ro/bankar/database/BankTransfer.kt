package ro.bankar.database

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import ro.bankar.amount
import ro.bankar.banking.exchange
import ro.bankar.banking.reverseExchange
import ro.bankar.currency
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SDirection
import java.math.BigDecimal

class BankTransfer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankTransfer>(BankTransfers) {
        fun findRecent(user: User, count: Int) = user.bankAccountIds.let { ids ->
            // Ignore party transfers to user (they are shown separately)
            find { (BankTransfers.sender inList ids) or ((BankTransfers.recipient inList ids) and BankTransfers.party.isNull()) }
                .orderBy(BankTransfers.timestamp to SortOrder.DESC)
                .limit(count)
        }

        fun findBetween(user: User, otherUser: User) = with(BankTransfers) {
            find {
                ((sender inList user.bankAccountIds) and (recipient inList otherUser.bankAccountIds)) or ((sender eq otherUser.id) and (recipient eq user.id))
            }.orderBy(timestamp to SortOrder.DESC)
        }

        fun findInPeriod(account: BankAccount, range: ClosedRange<Instant>) = with(BankTransfers) {
            find {
                ((sender eq account.id) or (recipient eq account.id)) and (timestamp greaterEq range.start) and (timestamp lessEq range.endInclusive)
            }.orderBy(timestamp to SortOrder.DESC)
        }

        fun transfer(sourceAccount: BankAccount, targetAccount: BankAccount, amount: BigDecimal, note: String,
                     partyMember: PartyMember? = null, removeFunds: Boolean = true): Boolean {
            if (sourceAccount.currency != targetAccount.currency) throw RuntimeException("transfer: accounts have different currencies")
            if (amount <= BigDecimal.ZERO) throw RuntimeException("transfer: invalid amount: $amount")

            if (removeFunds && sourceAccount.spendable < amount) return false
            if (removeFunds) sourceAccount.balance -= amount
            targetAccount.balance += amount

            val transfer = new {
                sender = sourceAccount
                senderName = sourceAccount.user.fullName
                senderIban = sourceAccount.iban
                recipient = targetAccount
                recipientName = targetAccount.user.fullName
                recipientIban = targetAccount.iban
                this.amount = amount
                currency = sourceAccount.currency
                this.note = note.trim()
                this.party = partyMember?.party
            }
            partyMember?.transfer = transfer
            return true
        }

        fun transfer(request: TransferRequest, targetAccount: BankAccount): Boolean {
            if (request.amount > BigDecimal.ZERO) {
                // Funds are already locked (removed from sourceAccounts), no need to check balance
                transfer(request.sourceAccount, targetAccount, request.amount, request.note,
                    partyMember = request.partyMember, removeFunds = false)
            } else {
                // Funds are not already locked
                if (!transfer(targetAccount, request.sourceAccount, -request.amount, request.note,
                        partyMember = request.partyMember, removeFunds = true)) return false
            }
            request.delete()
            return true
        }

        fun transferExchanging(sourceAccount: BankAccount, targetAccount: BankAccount, amount: BigDecimal, note: String,
                               originalAmount: BigDecimal? = null, partyMember: PartyMember? = null, removeFunds: Boolean = true): Boolean {
            if (sourceAccount.currency == targetAccount.currency) throw RuntimeException("transfer: accounts have same currency")
            if (amount <= BigDecimal.ZERO) throw RuntimeException("transfer: invalid amount: $amount")

            val exchanged = originalAmount ?: EXCHANGE_DATA.exchange(sourceAccount.currency, targetAccount.currency, amount) ?: return false
            if (removeFunds && sourceAccount.spendable < amount) return false
            if (removeFunds) sourceAccount.balance -= amount
            targetAccount.balance += exchanged

            val transfer = new {
                sender = sourceAccount
                senderName = sourceAccount.user.fullName
                senderIban = sourceAccount.iban
                recipient = targetAccount
                recipientName = targetAccount.user.fullName
                recipientIban = targetAccount.iban
                this.amount = amount
                exchangedAmount = exchanged
                currency = sourceAccount.currency
                this.note = note.trim()
                party = partyMember?.party
            }
            partyMember?.transfer = transfer
            return true
        }

        fun transferExchanging(request: TransferRequest, targetAccount: BankAccount): Boolean {
            if (request.amount > BigDecimal.ZERO) {
                // Funds are already locked (removed from sourceAccounts), no need to check balance
                transferExchanging(request.sourceAccount, targetAccount, request.amount, request.note,
                    partyMember = request.partyMember, removeFunds = false)
            } else {
                // Funds are not already locked
                val amount = -request.amount
                val exchanged = EXCHANGE_DATA.reverseExchange(targetAccount.currency, request.sourceAccount.currency, amount) ?: return false
                if (!transferExchanging(targetAccount, request.sourceAccount, exchanged, request.note,
                        originalAmount = amount, partyMember = request.partyMember, removeFunds = true)) return false
            }
            request.delete()
            return true
        }
    }

    var sender by BankAccount optionalReferencedOn BankTransfers.sender
    var senderName by BankTransfers.senderName
    var senderIban by BankTransfers.senderIban

    var recipient by BankAccount optionalReferencedOn BankTransfers.recipient
    var recipientName by BankTransfers.recipientName
    var recipientIban by BankTransfers.recipientIban

    var amount by BankTransfers.amount
    var exchangedAmount by BankTransfers.exchangedAmount
    var currency by BankTransfers.currency
    var note by BankTransfers.note
    var timestamp by BankTransfers.timestamp
    private val partyID by BankTransfers.party
    var party by Party optionalReferencedOn BankTransfers.party

    fun serializable(direction: SDirection?): SBankTransfer {
        val bankAccount = if (direction == SDirection.Sent) sender!! else recipient!!
        val sourceAccount = if (direction == SDirection.Sent) recipient else sender
        return SBankTransfer(
            direction, bankAccount.id.value, if (direction != null) null else sourceAccount!!.id.value,
            sourceAccount?.user?.publicSerializable(sourceAccount.user.hasFriend(bankAccount.user)),
            if (direction == SDirection.Sent) recipientName else senderName,
            if (direction == SDirection.Sent) recipientIban else senderIban,
            partyID?.value, amount.toDouble(), exchangedAmount?.toDouble(), currency, bankAccount.currency, note, timestamp
        )
    }

    fun serializable(user: User) =
        serializable(
            when (sender?.user?.id) {
                recipient?.user?.id -> null
                user.id -> SDirection.Sent
                else -> SDirection.Received
            }
        )
}

fun SizedIterable<BankTransfer>.serializable(user: User) = map { it.serializable(user) }

internal object BankTransfers : IntIdTable(columnName = "transfer_id") {
    val sender = reference("sender_account", BankAccounts, onDelete = ReferenceOption.SET_NULL).nullable()
    val senderName = varchar("sender_name", 50)
    val senderIban = varchar("sender_iban", 34)

    val recipient = reference("recipient_account", BankAccounts, onDelete = ReferenceOption.SET_NULL).nullable()
    val recipientName = varchar("recipient_name", 50)
    val recipientIban = varchar("recipient_iban", 34)

    val amount = amount("amount").check("amount_positive") { it greater BigDecimal.ZERO }
    val exchangedAmount = amount("exchanged_amount").check("exchanged_amount_positive") { it greater BigDecimal.ZERO }.nullable()
    val currency = currency("currency")
    val note = varchar("note", 200)
    val timestamp = timestamp("timestamp").defaultExpression(CurrentTimestamp)
    val party = reference("party", Parties).nullable()

    init {
        check("sender_or_recipient_not_null") { sender.isNotNull() or recipient.isNotNull() }
    }
}