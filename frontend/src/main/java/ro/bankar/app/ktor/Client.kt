package ro.bankar.app.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLProtocol

val ktorClient = HttpClient(OkHttp) {
    install(DefaultRequest) {
        url {
            protocol = URLProtocol.HTTP
            host = "localhost"
        }
    }
    // TODO Install serialization, content negotiation
}