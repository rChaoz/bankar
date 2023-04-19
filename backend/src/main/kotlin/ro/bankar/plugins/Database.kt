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
import ro.bankar.database.TransferRequests
import ro.bankar.database.Users

fun Database.Companion.connect() = this.connect(
    url = "jdbc:h2:file:./build/test",
    user = "root",
    driver = "org.h2.Driver",
    password = ""
)

private val tables = listOf(
    Users, FriendRequests, FriendPairs,
    BankAccounts, BankTransfers, BankCards, CardTransactions,
    Parties, PartyMembers, TransferRequests,
    AssetAccounts,
)

fun Database.Companion.reset() {
    this.connect()
    transaction { tables.reversed().forEach { SchemaUtils.drop(it) } }
    init()
}

fun Database.Companion.init() {
    this.connect()
    transaction { tables.forEach { SchemaUtils.create(it) } }
}