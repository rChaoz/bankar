package ro.bankar.plugins

import io.ktor.server.application.*
import io.ktor.server.auth.*
import ro.bankar.database.User

data class UserPrincipal(val user: User) : Principal

fun Application.configureAuthentication() {
    authentication {
        bearer {
            authenticate { credential -> User.findBySessionToken(credential.token)?.let { it.updateTokenExpiration(); UserPrincipal(it) } }
        }
    }
}