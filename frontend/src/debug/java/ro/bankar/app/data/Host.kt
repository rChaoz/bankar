package ro.bankar.app.data

import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path

fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTP
        // For emulator to connect to host PC
        host = "10.0.2.2:8080"
        path("api/")
    }
}

fun URLBuilder.setSocketProtocol() {
    protocol = URLProtocol.WS
}