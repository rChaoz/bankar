package ro.bankar.app.data

import android.net.Uri
import io.ktor.client.plugins.*
import io.ktor.http.*
import ro.bankar.model.SStatement

fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTPS
        host = "bankar.onrender.com"
        path("api/")
    }
}
val SStatement.downloadURI get() = Uri.parse("https://bankar.onrender.com/api/statements/$id")!!

fun URLBuilder.setSocketProtocol() {
    protocol = URLProtocol.WSS
}