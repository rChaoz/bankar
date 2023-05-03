package ro.bankar.app.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ro.bankar.model.SPublicUser
import ro.bankar.model.SUser

abstract class KtorFlow<T> protected constructor(
    private val scope: CoroutineScope,
    protected val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : SharedFlow<T> by flow.asSharedFlow() {

    fun requestRefresh() {
        scope.launch { refresh() }
    }

    protected abstract suspend fun refresh()
}

class Repository(private val scope: CoroutineScope, private val sessionToken: String, private val onSessionExpire: () -> Unit) {
    private inline fun <T> KtorFlow(crossinline refreshBlock: suspend MutableSharedFlow<T>.() -> Unit) = object : KtorFlow<T>(scope) {
        override suspend fun refresh() = flow.refreshBlock()
    }

    private suspend inline fun <reified T> get(url: String, crossinline block: HttpRequestBuilder.() -> Unit = {}): T? {
        val result = ktorClient.safeRequest<T> {
            get(url) {
                bearerAuth(sessionToken)
                block()
            }
        }
        return when (result) {
            is SafeResponse.InternalError -> null
            is SafeResponse.Fail -> {
                if (result.r.status == HttpStatusCode.Unauthorized || result.r.status == HttpStatusCode.Forbidden) onSessionExpire()
                null
            }
            is SafeResponse.Success -> result.result
        }
    }

    private inline fun <reified T> ktorGetFlow(url: String) = KtorFlow<T> { get<T>(url)?.let { emit(it) } }

    val profile = ktorGetFlow<SUser>("profile")
    val friends = ktorGetFlow<List<SPublicUser>>("friends")
    val friendsRequests = ktorGetFlow<List<SPublicUser>>("friendsRequests")
}