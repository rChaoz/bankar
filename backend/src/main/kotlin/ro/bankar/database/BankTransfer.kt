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
import ro.bankar.currency
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SDirection
import java.math.BigDecimal

class BankTransfer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankTransfer>(BankTransfers) {
        fun findRecent(accounts: Iterable<BankAccount>, count: Int) = accounts.map { it.id }.let { ids ->
            find { (BankTransfers.sender inList ids) or (BankTransfers.recipient inList ids) }
                .orderBy(BankTransfers.dateTime to SortOrder.DESC)
                .limit(count)
        }

        fun transfer(sourceAccount: BankAccount, targetAccount: BankAccount, amount: BigDecimal, note: String): Boolean {
            if (sourceAccount.balance < amount) return false
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
                this.note = note
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
    var currency by BankTransfers.currency
    var note by BankTransfers.note
    var dateTime by BankTransfers.dateTime

    fun serializable(direction: SDirection) = SBankTransfer(
        direction,
        if (direction == SDirection.Sent) recipientName else senderName,
        if (direction == SDirection.Sent) recipientIban else senderIban,
        amount.toDouble(), currency, note, dateTime
    )

    fun serializable(user: User) =
        serializable(if (sender?.user?.id == user.id) ro.bankar.model.SDirection.Sent else SDirection.Received)
}

fun SizedIterable<BankTransfer>.serializable(user: User) = map { it.serializable(user) }

internal object BankTransfers : IntIdTable(columnName = "transfer_id") {
    val sender = reference("sender_account", BankAccounts).nullable()
    val senderName = varchar("sender_name", 50)
    val senderIban = varchar("sender_iban", 34)

    val recipient = reference("recipient_account", BankAccounts).nullable()
    val recipientName = varchar("recipient_name", 50)
    val recipientIban = varchar("recipient_iban", 34)

    val amount = amount("amount")
    val currency = currency("currency")
    val note = varchar("note", 200)
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
}