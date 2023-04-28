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
    class Success<T, S>(val r: HttpResponse, val result: T) : Response<T, S>()
    class Fail<T, S>(val r: HttpResponse, val s: S) : Response<T, S>()
    class Error<T, S>(val message: Int) : Response<T, S>()
}

suspend inline fun <reified T, reified S> HttpClient.safeRequest(
    successCode: HttpStatusCode,
    crossinline request: suspend HttpClient.() -> HttpResponse
): Response<T, S> =
    withContext(Dispatchers.IO) {
        val result = runCatching { request() }
        if (result.isFailure) {
            Log.e(TAG, "HttpRequest", result.exceptionOrNull()!!)
            Response.Error(R.string.connection_error)
        } else {
            try {
                val response = result.getOrThrow()
                if (response.status == successCode) Response.Success(response, response.body())
                else Response.Fail(response, response.body())
            } catch (e: Exception) {
                Log.e(TAG, "HttpRequest", e)
                Response.Error(R.string.invalid_server_response)
            }
        }
    }

suspend inline fun <reified T, reified R> HttpClient.safeGet(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline builder: HttpRequestBuilder.() -> Unit
) = safeRequest<T, R>(successCode) { get(builder) }

suspend inline fun <reified T, reified R> HttpClient.safePost(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline builder: HttpRequestBuilder.() -> Unit
) = safeRequest<T, R>(successCode) { post(builder) }
