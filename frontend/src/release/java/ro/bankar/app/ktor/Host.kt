package ro.bankar.app.ktor

import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.path

fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTPS
        host = "rct33b.asuscomm.com"
        path("api/")
    }
}