package ro.bankar.app.data

import android.net.Uri
import io.ktor.client.plugins.DefaultRequest
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import ro.bankar.model.SStatement

fun DefaultRequest.DefaultRequestBuilder.configUrl() {
    url {
        protocol = URLProtocol.HTTP
        // For emulator to connect to host PC
        host = "10.0.2.2:8080"
        path("api/")
    }
}

val SStatement.downloadURI get() = Uri.parse("http://10.0.2.2:8080/api/statements/$id")!!

fun URLBuilder.setSocketProtocol() {
    protocol = URLProtocol.WS
}