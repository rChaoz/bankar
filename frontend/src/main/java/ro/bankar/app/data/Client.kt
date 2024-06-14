package ro.bankar.app.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.bankar.app.R
import ro.bankar.app.TAG
import ro.bankar.app.ui.show
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.Response
import ro.bankar.model.SuccessResponse
import ro.bankar.model.ValueResponse

/**
 * Client used for unauthenticated requests
 */
val basicClient = HttpClient(OkHttp) {
    defaultRequest {
        configUrl()
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json()
    }
}

/**
 * Class representing the result of a [safeRequest].
 */
sealed class RequestResult<T>
class RequestFail<T>(val message: Int) : RequestResult<T>()
class RequestSuccess<T>(val response: T) : RequestResult<T>()
typealias ResponseRequestResult<T> = RequestResult<Response<T>>

// Utility functions for dealing with RequestResults
inline fun <T, R> RequestResult<T>.fold(onFail: (Int) -> R, onSuccess: (T) -> R) = when (this) {
    is RequestFail -> onFail(message)
    is RequestSuccess -> onSuccess(response)
}

inline fun <T> RequestResult<Response<T>>.onSuccess(onSuccess: () -> Unit) =
    if (this is RequestSuccess && (response == SuccessResponse || response is ValueResponse)) {
        onSuccess()
        this
    } else this

// With snackbar
inline fun <T> RequestResult<T>.handle(scope: CoroutineScope, snackbar: SnackbarHostState, context: Context, onResponse: (T) -> String?) = fold(
    onFail = { scope.launch { snackbar.show(context.getString(it)) } },
    onSuccess = { onResponse(it)?.let { r -> scope.launch { snackbar.show(r) } } }
)

inline fun <T> RequestResult<Response<T>>.handleSuccess(
    scope: CoroutineScope,
    snackbar: SnackbarHostState,
    context: Context,
    onSuccess: () -> Unit
) = handle(scope, snackbar, context) {
    when (it) {
        SuccessResponse -> { onSuccess(); null }
        is InvalidParamResponse -> context.getString(R.string.invalid_field, it.param)
        else -> context.getString(R.string.unknown_error)
    }
}

inline fun <T> RequestResult<Response<T>>.handleValue(
    scope: CoroutineScope,
    snackbar: SnackbarHostState,
    context: Context,
    onSuccess: (T) -> Unit
) = handle(scope, snackbar, context) {
    when (it) {
        is ValueResponse -> { onSuccess(it.value); null }
        is InvalidParamResponse -> context.getString(R.string.invalid_field, it.param)
        else -> context.getString(R.string.unknown_error)
    }
}

// With toast
inline fun <T> RequestResult<T>.handle(context: Context, onResponse: (T) -> String?): Unit = fold(
    onFail = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
    onSuccess = {
        onResponse(it)?.let { r -> Toast.makeText(context, r, Toast.LENGTH_SHORT).show() }
    }
)

inline fun <T> RequestResult<Response<T>>.handleSuccess(context: Context, onSuccess: () -> Unit) = handle(context) {
    when (it) {
        SuccessResponse -> { onSuccess(); null }
        is InvalidParamResponse -> context.getString(R.string.invalid_field, it.param)
        else -> context.getString(R.string.unknown_error)
    }
}

suspend inline fun <reified T> HttpClient.safeRequest(crossinline request: suspend HttpClient.() -> HttpResponse): RequestResult<Response<T>> =
    safeRawRequest(request)

suspend inline fun <reified T> HttpClient.safeRawRequest(crossinline request: suspend HttpClient.() -> HttpResponse): RequestResult<T> =
    withContext(Dispatchers.IO) {
        runCatching { request() }.fold(
            onFailure = {
                Log.e(TAG, "HttpRequest", it)
                RequestFail(R.string.connection_error)
            },
            onSuccess = {
                if (it.status == HttpStatusCode.InternalServerError) {
                    Log.w(TAG, "Request $it returned status code 500 Internal Server Error")
                    return@fold RequestFail(R.string.internal_error)
                }
                try {
                    RequestSuccess(it.body())
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.w(TAG, "Request $it: unable to parse response", e)
                    RequestFail(R.string.invalid_server_response)
                }
            }
        )
    }
