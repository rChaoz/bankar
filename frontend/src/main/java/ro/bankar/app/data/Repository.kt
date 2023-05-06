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

    /**
     * @param mustRetry if this emission cannot fail. If true and there is an error, emission will be reattempted until successful
     */
    fun requestEmit(mustRetry: Boolean) {
        scope.launch { onEmissionRequest(mustRetry) }
    }

    protected abstract suspend fun onEmissionRequest(mustRetry: Boolean)
}

// LocalRepository defined in debug/release source sets

fun repository(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository = RepositoryImpl(scope, sessionToken, onSessionExpire)

abstract class Repository(protected val scope: CoroutineScope, protected val sessionToken: String, protected val onSessionExpire: () -> Unit) {
    data class Error(val message: Int, val mustRetry: Boolean, val retry: (mustRetry: Boolean) -> Unit)
    protected val mutableErrorFlow = MutableSharedFlow<Error>(replay = 0)
    val errorFlow = mutableErrorFlow.asSharedFlow()

    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract val friends: RequestFlow<List<SPublicUser>>
    abstract val friendRequests: RequestFlow<List<SPublicUser>>

    // Bank accounts
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
}

private class RepositoryImpl(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository(scope, sessionToken, onSessionExpire) {
    // User profile & friends
    override val profile = createFlow<SUser>("profile")
    override val friends = createFlow<List<SPublicUser>>("friends")
    override val friendRequests = createFlow<List<SPublicUser>>("friend_requests")

    // Bank accounts
    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = ktorClient.safePost<StatusResponse, InvalidParamResponse>(HttpStatusCode.Created) {
        url("accounts/new")
        bearerAuth(sessionToken)
        setBody(account)
    }


    // Load data on Repository creation to avoid having to wait when going to each screen
    init {
        profile.requestEmit(false)
        friends.requestEmit(false)
        friendRequests.requestEmit(false)
        accounts.requestEmit(false)
    }

    // Utility functions
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(mustRetry: Boolean) {
            val r = ktorClient.safeRequest<T> {
                get(url) {
                    bearerAuth(sessionToken)
                }
            }
            when (r) {
                is SafeResponse.InternalError -> mutableErrorFlow.emit(Error(r.message, mustRetry, this::requestEmit))
                is SafeResponse.Fail -> {
                    if (r.r.status == HttpStatusCode.Unauthorized || r.r.status == HttpStatusCode.Forbidden) onSessionExpire()
                    else mutableErrorFlow.emit(Error(0, mustRetry, this::requestEmit))
                }
                is SafeResponse.Success -> flow.emit(r.result)
            }
        }
    }
}