package ro.bankar.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ro.bankar.routing.configureAPIs
import ro.bankar.routing.configureBanking
import ro.bankar.routing.configureUsers

fun Application.configureRouting() {
    routing {
        route("api") {
            configureUsers()
            configureAPIs()
            authenticate {
                configureBanking()
            }
            static("data") {
                resources("data")
            }
        }
    }
}