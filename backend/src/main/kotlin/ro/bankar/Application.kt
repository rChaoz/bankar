package ro.bankar

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import ro.bankar.database.TableService
import ro.bankar.database.UsersService
import ro.bankar.plugins.*

fun main(args: Array<String>) {
    fun forServices(block: TableService.() -> Unit) = listOf(UsersService).forEach(block)

    if (args.size == 1 && args[0] == "reset") {
        Database.connect()
        forServices { dropTable(); createTable() }
    } else if (args.size == 1 && args[0] == "init") {
        Database.connect()
        forServices { createTable() }
    }
    io.ktor.server.netty.EngineMain.main(args)
}

@Suppress("unused")
fun Application.module() {
    configureSecurity()
    configureSerialization()
    configureDatabase()
    configureSockets()
    configureRouting()
}
