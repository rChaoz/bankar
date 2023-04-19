package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import ro.bankar.routing.*

fun Application.configureRouting() {
    routing {
        configureUsers()
        authenticate {
            configureBanking()
        }
    }
}