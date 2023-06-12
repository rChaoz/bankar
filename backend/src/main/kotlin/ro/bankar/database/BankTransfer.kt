package ro.bankar.database

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.or
import ro.bankar.amount
import ro.bankar.banking.exchange
import ro.bankar.banking.reverseExchange
import ro.bankar.currency
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SDirection
import ro.bankar.util.nowUTC
import java.math.BigDecimal

class BankTransfer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankTransfer>(BankTransfers) {
        fun findRecent(user: User, count: Int) = user.bankAccountIds.let { ids ->
            // Ignore party transfers to user (they are shown separately)
            find { (BankTransfers.sender inList ids) or ((BankTransfers.recipient inList ids) and BankTransfers.party.isNull()) }
                .orderBy(BankTransfers.dateTime to SortOrder.DESC)
                .limit(count)
        }

        fun findBetween(user: User, otherUser: User) = with(BankTransfers) {
            find {
                ((sender inList user.bankAccountIds) and (recipient inList otherUser.bankAccountIds)) or ((sender eq otherUser.id) and (recipient eq user.id))
            }.orderBy(dateTime to SortOrder.DESC)
        }

        fun findInPeriod(account: BankAccount, range: ClosedRange<LocalDate>) = with(BankTransfers) {
            val start = LocalDateTime(range.start, LocalTime(0, 0, 0))
            val end = LocalDateTime(range.endInclusive, LocalTime(23, 59, 59, 999_999_999))
            find {
                ((sender eq account.id) or (recipient eq account.id)) and (dateTime greaterEq start) and (dateTime lessEq end)
            }.orderBy(dateTime to SortOrder.DESC)
        }

        fun transfer(sourceAccount: BankAccount, targetAccount: BankAccount, amount: BigDecimal, note: String): Boolean {
            if (sourceAccount.currency != targetAccount.currency) throw RuntimeException("transfer: accounts have different currencies")
            if (sourceAccount.spendable < amount) return false
            sourceAccount.balance -= amount
            targetAccount.balance += amount
            new {
                sender = sourceAccount
                senderName = sourceAccount.user.fullName
                senderIban = sourceAccount.iban
                recipient = targetAccount
                recipientName = targetAccount.user.fullName
                recipientIban = targetAccount.iban
                this.amount = amount
                currency = sourceAccount.currency
                this.note = note.trim()
            }
            return true
        }

        fun transfer(request: TransferRequest, otherAccount: BankAccount): Boolean {
            if (request.sourceAccount.currency != otherAccount.currency) throw RuntimeException("transfer: accounts have different currencies")
            return transferExchanging(request, otherAccount)
        }

        fun transferExchanging(request: TransferRequest, otherAccount: BankAccount): Boolean {
            fun saveTransfer(senderAcc: BankAccount, recipientAcc: BankAccount, amount: BigDecimal, exchanged: BigDecimal?) {
                val transfer = new {
                    sender = senderAcc
                    senderName = senderAcc.user.fullName
                    senderIban = senderAcc.iban
                    recipient = recipientAcc
                    recipientName = recipientAcc.user.fullName
                    recipientIban = recipientAcc.iban
                    this.amount = amount
                    exchangedAmount = exchanged
                    currency = senderAcc.currency
                    note = request.note
                    party = request.partyMember?.party
                }
                request.partyMember?.transfer = transfer
            }

            // Normal transfer (request.sourceAccount -> otherAccount)
            if (request.amount > BigDecimal.ZERO) {
                // Funds are already locked (removed from request.sourceAccount), no need to do that again
                if (request.sourceAccount.currency == otherAccount.currency) {
                    otherAccount.balance += request.amount
                    saveTransfer(request.sourceAccount, otherAccount, request.amount, null)
                } else {
                    val exchanged = EXCHANGE_DATA.exchange(request.sourceAccount.currency, otherAccount.currency, request.amount) ?: return false
                    otherAccount.balance += exchanged
                    saveTransfer(request.sourceAccount, otherAccount, request.amount, exchanged)
                }

                // Reverse transfer (otherAccount -> request.sourceAccount)
            } else {
                if (request.sourceAccount.currency == otherAccount.currency) {
                    val amount = -request.amount
                    if (otherAccount.spendable < amount) return false
                    otherAccount.balance -= amount
                    request.sourceAccount.balance += amount
                    saveTransfer(otherAccount, request.sourceAccount, amount, null)
                } else {
                    // Calculate amount that is needed to be exchanged in order to obtain requested amount
                    val exchanged = EXCHANGE_DATA.reverseExchange(otherAccount.currency, request.sourceAccount.currency, -request.amount) ?: return false
                    if (otherAccount.spendable < exchanged) return false
                    otherAccount.balance -= exchanged
                    request.sourceAccount.balance += -request.amount
                    saveTransfer(otherAccount, request.sourceAccount, exchanged, -request.amount)
                }
            }
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
    var dateTime by BankTransfers.dateTime
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
            partyID?.value, amount.toDouble(), exchangedAmount?.toDouble(), currency, bankAccount.currency, note, dateTime
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
    val dateTime = datetime("datetime").clientDefault { Clock.System.nowUTC() }
    val party = reference("party", Parties).nullable()

    init {
        check("sender_or_recipient_not_null") { sender.isNotNull() or recipient.isNotNull() }
    }
}