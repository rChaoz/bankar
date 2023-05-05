package ro.bankar.app.data

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SPublicUser
import ro.bankar.model.SUser

/**
 * A Shared flow with the option to request emitting
 */
abstract class RequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    protected val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : SharedFlow<T> by flow.asSharedFlow() {

    fun requestEmit() {
        scope.launch { onEmitRequest() }
    }

    protected abstract suspend fun onEmitRequest()
}

// LocalRepository defined in debug/release source sets

fun repository(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository = RepositoryImpl(scope, sessionToken, onSessionExpire)

abstract class Repository(protected val scope: CoroutineScope, protected val sessionToken: String, protected val onSessionExpire: () -> Unit) {
    data class Error(val message: Int, val retry: () -> Unit)
    protected val mutableErrorFlow = MutableSharedFlow<Error>(replay = 0)
    val errorFlow = mutableErrorFlow.asSharedFlow()


    abstract val profile: RequestFlow<SUser>
    abstract val friends: RequestFlow<List<SPublicUser>>
    abstract val friendRequests: RequestFlow<List<SPublicUser>>

    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
}

private class RepositoryImpl(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository(scope, sessionToken, onSessionExpire) {
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmitRequest() {
            val r = ktorClient.safeRequest<T> {
                get(url) {
                    bearerAuth(sessionToken)
                }
            }
            when (r) {
                is SafeResponse.InternalError -> mutableErrorFlow.emit(Error(r.message, this::requestEmit))
                is SafeResponse.Fail -> {
                    if (r.r.status == HttpStatusCode.Unauthorized || r.r.status == HttpStatusCode.Forbidden) onSessionExpire()
                    else mutableErrorFlow.emit(Error(0, this::requestEmit))
                }
                is SafeResponse.Success -> flow.emit(r.result)
            }
        }
    }

    override val profile = createFlow<SUser>("profile")
    override val friends = createFlow<List<SPublicUser>>("friends")
    override val friendRequests = createFlow<List<SPublicUser>>("friend_requests")

    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")

    init {
        profile.requestEmit()
        friends.requestEmit()
        friendRequests.requestEmit()
        accounts.requestEmit()
    }
}