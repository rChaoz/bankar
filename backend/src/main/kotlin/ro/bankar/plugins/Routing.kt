package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import ro.bankar.routing.configureAPIs
import ro.bankar.routing.configureBanking
import ro.bankar.routing.configureUserAccounts
import ro.bankar.routing.configureUserProfiles

fun Application.configureRouting() {
    routing {
        route("api") {
            configureUserAccounts()
            configureAPIs()
            authenticate {
                configureUserProfiles()
                configureBanking()
            }
            static("data") {
                resources("data")
            }
        }
    }
}