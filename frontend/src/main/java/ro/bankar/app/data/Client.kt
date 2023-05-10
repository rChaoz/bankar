package ro.bankar.app.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.bankar.app.R
import ro.bankar.app.TAG

val ktorClient = HttpClient(OkHttp) {
    defaultRequest {
        configUrl()
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json()
    }
}

sealed class SafeStatusResponse<Result, Fail> {
    class Success<Result, Fail>(val r: HttpResponse, val result: Result) : SafeStatusResponse<Result, Fail>()
    class Fail<Result, Fail>(val r: HttpResponse, val s: Fail) : SafeStatusResponse<Result, Fail>()
    class InternalError<Result, Fail>(val message: Int) : SafeStatusResponse<Result, Fail>()
}

sealed class SafeResponse<Result> {
    class Success<Result>(val r: HttpResponse, val result: Result) : SafeResponse<Result>()
    class Fail<Result>(val r: HttpResponse, val body: String) : SafeResponse<Result>()
    class InternalError<Result>(val message: Int) : SafeResponse<Result>()
}

suspend inline fun <reified Result, reified Fail> HttpClient.safeStatusRequest(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline request: suspend HttpClient.() -> HttpResponse
): SafeStatusResponse<Result, Fail> =
    withContext(Dispatchers.IO) {
        val result = runCatching { request() }
        if (result.isFailure) {
            Log.e(TAG, "HttpRequest", result.exceptionOrNull()!!)
            SafeStatusResponse.InternalError(R.string.connection_error)
        } else {
            try {
                val response = result.getOrThrow()
                if (response.status == successCode) SafeStatusResponse.Success(response, response.body())
                else {
                    val body = response.body<Fail>()
                    Log.w(TAG, "HttpRequest failed: $response\n$body")
                    SafeStatusResponse.Fail(response, body)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "HttpRequest", e)
                SafeStatusResponse.InternalError(R.string.invalid_server_response)
            }
        }
    }

suspend inline fun <reified Result> HttpClient.safeRequest(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline request: suspend HttpClient.() -> HttpResponse
): SafeResponse<Result> =
    withContext(Dispatchers.IO) {
        val result = runCatching { request() }
        if (result.isFailure) {
            Log.e(TAG, "HttpRequest", result.exceptionOrNull()!!)
            SafeResponse.InternalError(R.string.connection_error)
        } else {
            try {
                val response = result.getOrThrow()
                if (response.status == successCode) SafeResponse.Success(response, response.body())
                else if (response.status == HttpStatusCode.InternalServerError) SafeResponse.InternalError(R.string.internal_error)
                else {
                    val body = response.bodyAsText()
                    Log.w(TAG, "HttpRequest failed: $response\n$body")
                    SafeResponse.Fail(response, body)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "HttpRequest", e)
                SafeResponse.InternalError(R.string.invalid_server_response)
            }
        }
    }

suspend inline fun <reified Result, reified Fail> HttpClient.safeGet(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline builder: HttpRequestBuilder.() -> Unit
) = safeStatusRequest<Result, Fail>(successCode) { get(builder) }

suspend inline fun <reified Result, reified Fail> HttpClient.safePost(
    successCode: HttpStatusCode = HttpStatusCode.OK,
    crossinline builder: HttpRequestBuilder.() -> Unit
) = safeStatusRequest<Result, Fail>(successCode) { post(builder) }
