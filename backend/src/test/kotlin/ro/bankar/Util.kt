package ro.bankar

import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*

internal fun ApplicationTestBuilder.client() = createClient {
    install(ContentNegotiation) { json() }
    defaultRequest {
        contentType(ContentType.Application.Json)
        url.path("api/")
    }
}