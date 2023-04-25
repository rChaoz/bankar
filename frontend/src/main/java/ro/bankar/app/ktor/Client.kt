package ro.bankar.app.ktor

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.bankar.app.R
import ro.bankar.app.TAG
import ro.bankar.app.configUrl

val ktorClient = HttpClient(OkHttp) {
    install(DefaultRequest) {
        configUrl()
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json()
    }
}

sealed class Response<T, S> {
    class ErrorMessage<T, S>(val message: Int) : Response<T, S>() {
    }

    class Status<T, S>(val r: HttpResponse, val s: S) : Response<T, S>()
    class Success<T, S>(val r: HttpResponse, val result: T) : Response<T, S>()
}

suspend inline fun <reified T, reified S> HttpClient.safeRequest(crossinline request: suspend HttpClient.() -> HttpResponse): Response<T, S> =
    withContext(Dispatchers.IO) {
        val result = runCatching { request() }
        if (result.isFailure) {
            Log.e(TAG, "HttpRequest", result.exceptionOrNull()!!)
            Response.ErrorMessage(R.string.connection_error)
        } else {
            try {
                val response = result.getOrThrow()
                if (response.status == HttpStatusCode.OK) Response.Success(response, response.body())
                else Response.Status(response, response.body())
            } catch (e: Exception) {
                Response.ErrorMessage(R.string.invalid_server_response)
            }
        }
    }

suspend inline fun <reified T, reified R> HttpClient.safeGet(crossinline builder: HttpRequestBuilder.() -> Unit) = safeRequest<T, R> { get(builder) }

suspend inline fun <reified T, reified R> HttpClient.safePost(crossinline builder: HttpRequestBuilder.() -> Unit) = safeRequest<T, R> { post(builder) }
