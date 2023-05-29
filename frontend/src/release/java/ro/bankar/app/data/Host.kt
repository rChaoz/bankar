package ro.bankar.app.data

import android.net.Uri
import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import ro.bankar.model.SStatement

fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTPS
        host = "rct33b.asuscomm.com"
        path("api/")
    }
}
val SStatement.downloadURI get() = Uri.parse("https://rct33b.asuscomm.com/api/statements/$id")!!

fun URLBuilder.setSocketProtocol() {
    protocol = URLProtocol.WSS
}