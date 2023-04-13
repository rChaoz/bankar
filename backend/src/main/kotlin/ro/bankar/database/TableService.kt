package ro.bankar.database

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

open class TableService(private val table: Table) {
    internal fun createTable() = transaction { SchemaUtils.create(table) }
    internal fun dropTable() = transaction { SchemaUtils.drop(table) }
    protected suspend fun <T> dbQuery(block: suspend () -> T): T = newSuspendedTransaction(Dispatchers.IO) { block() }
}