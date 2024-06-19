package ro.bankar.plugins

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.database.*

private val tables = arrayOf(
    Users, FriendRequests, FriendPairs, UserMessages,
    BankAccounts, BankTransfers, BankCards, CardTransactions,
    Parties, PartyMembers, TransferRequests,
    AssetAccounts, Statements
)

private fun Database.Companion.connect() = this.connect(
    url = System.getenv("DB_URL")!!.let { if (it.startsWith("jdbc:")) it else "jdbc:$it" },
    user = System.getenv("DB_USER") ?: "",
    password = System.getenv("DB_PASS") ?: ""
)

fun Database.Companion.reset() {
    connect()
    transaction {
        // Needed because Exposed doesn't handle circular dependencies
        exec("ALTER TABLE IF EXISTS TRANSFERREQUESTS DROP CONSTRAINT IF EXISTS FK_TRANSFERREQUESTS_PARTY_MEMBER__ID;")
        exec("ALTER TABLE IF EXISTS BANKACCOUNTS DROP CONSTRAINT IF EXISTS FK_BANKACCOUNTS_USER_ID__USER_ID;")
        SchemaUtils.drop(*tables)
    }
}

fun Database.Companion.init() {
    connect()
    transaction {
        SchemaUtils.createMissingTablesAndColumns(*tables)
    }
}