package ro.bankar

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ro.bankar.plugins.configureSessions
import ro.bankar.plugins.*

fun main(args: Array<String>) {

    if (args.size == 1 && args[0] == "reset") {
        Database.connect()
        Database.tables.reversed().forEach { transaction { SchemaUtils.drop(it) } }
    }
    if (args.size == 1 && (args[0] == "init" || args[0] == "reset")) {
        Database.connect()
        Database.tables.forEach { transaction { SchemaUtils.create(it) } }
    }
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    configureSerialization()
    configureSessions()
    configureAuthentication()
    configureDatabase()
    configureSockets()
    configureRouting()
}
