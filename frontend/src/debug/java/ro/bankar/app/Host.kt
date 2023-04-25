package ro.bankar.app

import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLProtocol


fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTP
        // For emulator to connect to host PC
        host = "10.0.2.2:8080"
    }
}