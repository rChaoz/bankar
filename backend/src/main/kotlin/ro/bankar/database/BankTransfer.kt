package ro.bankar.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import ro.bankar.amount
import ro.bankar.banking.Currency
import ro.bankar.currency
import ro.bankar.model.SBankTransfer

class BankTransfer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BankTransfer>(BankTransfers)

    var sender by BankAccount optionalReferencedOn BankTransfers.sender
    var senderName by BankTransfers.senderName
    var senderIban by BankTransfers.senderIban

    var recipient by BankAccount optionalReferencedOn BankTransfers.recipient
    var recipientName by BankTransfers.recipientName
    var recipientIban by BankTransfers.recipientIban

    var amount by BankTransfers.amount
    private var currencyString by BankTransfers.currency
    var currency: Currency
        get() = Currency.from(currencyString)
        set(value) {
            currencyString = value.code
        }
    var dateTime by BankTransfers.dateTime

    fun serializable() = SBankTransfer(senderName, senderIban, recipientName, recipientIban, amount.toDouble(), currency, dateTime)
}

internal object BankTransfers : IntIdTable(columnName = "transfer_id") {
    val sender = reference("sender_account", BankAccounts).nullable()
    val senderName = varchar("sender_name", 50)
    val senderIban = varchar("sender_iban", 34)

    val recipient = reference("recipient_account", BankAccounts).nullable()
    val recipientName = varchar("recipient_name", 50)
    val recipientIban = varchar("recipient_iban", 34)

    val amount = amount("amount")
    val currency = currency("currency")
    val dateTime = datetime("datetime").defaultExpression(CurrentDateTime)
}