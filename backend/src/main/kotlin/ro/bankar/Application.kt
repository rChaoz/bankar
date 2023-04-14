package ro.bankar

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import ro.bankar.database.BankAccounts
import ro.bankar.database.BankCards
import ro.bankar.database.Users
import ro.bankar.plugins.*

fun main(args: Array<String>) {
    fun forTables(block: (Table) -> Unit) = listOf(Users, BankAccounts, BankCards).forEach(block)

    if (args.size == 1 && args[0] == "reset") {
        Database.connect()
        forTables { SchemaUtils.drop(it); SchemaUtils.create(it) }
    } else if (args.size == 1 && args[0] == "init") {
        Database.connect()
        forTables { SchemaUtils.create(it) }
    }
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    configureSerialization()
    configureDatabase()
    configureSockets()
    configureRouting()
}
