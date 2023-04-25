package ro.bankar.app

import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLProtocol


fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTPS
        host = "rct33b.asuscomm.com"
    }
}