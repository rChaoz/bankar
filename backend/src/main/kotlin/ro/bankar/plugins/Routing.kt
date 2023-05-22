package ro.bankar.plugins

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.resources
import io.ktor.server.http.content.static
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import ro.bankar.routing.configureAPIs
import ro.bankar.routing.configureBankAccounts
import ro.bankar.routing.configureBankTransfers
import ro.bankar.routing.configureRecentActivity
import ro.bankar.routing.configureSockets
import ro.bankar.routing.configureUserAccounts
import ro.bankar.routing.configureUserMessaging
import ro.bankar.routing.configureUserProfiles

fun Application.configureRouting() {
    routing {
        route("api") {
            configureUserAccounts()
            configureAPIs()
            authenticate {
                configureSockets()

                configureRecentActivity()
                configureUserProfiles()
                configureUserMessaging()

                configureBankAccounts()
                configureBankTransfers()
            }
            static("data") {
                resources("data")
            }
        }
    }
}