package ro.bankar.plugins

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.database.AssetAccounts
import ro.bankar.database.BankAccounts
import ro.bankar.database.BankCards
import ro.bankar.database.BankTransfers
import ro.bankar.database.CardTransactions
import ro.bankar.database.FriendPairs
import ro.bankar.database.FriendRequests
import ro.bankar.database.Parties
import ro.bankar.database.PartyMembers
import ro.bankar.database.Statements
import ro.bankar.database.TransferRequests
import ro.bankar.database.UserMessages
import ro.bankar.database.Users

private val tables = listOf(
    Users, FriendRequests, FriendPairs, UserMessages,
    BankAccounts, BankTransfers, BankCards, CardTransactions,
    Parties, PartyMembers, TransferRequests,
    AssetAccounts, Statements
)

private fun Database.Companion.connect() = this.connect(
    url = "jdbc:h2:file:./build/test",
    user = "root",
    driver = "org.h2.Driver",
    password = ""
)

fun Database.Companion.reset() {
    connect()
    transaction { tables.reversed().forEach { SchemaUtils.drop(it) } }
}

fun Database.Companion.init() {
    connect()
    transaction { tables.forEach { SchemaUtils.create(it) } }
}