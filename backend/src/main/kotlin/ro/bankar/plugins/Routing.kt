package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import ro.bankar.routing.*

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
                configureParties()

                configureBankAccounts()
                configureBankTransfers()
                configureStatements()
            }
            staticResources("data", "data")
        }
    }
}