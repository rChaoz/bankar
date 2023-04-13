package ro.bankar.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun Database.Companion.connect() = this.connect(
    url = "jdbc:h2:file:./build/test",
    user = "root",
    driver = "org.h2.Driver",
    password = ""
)

fun Application.configureDatabase() {
    Database.connect()
    // TODO Routing
}
