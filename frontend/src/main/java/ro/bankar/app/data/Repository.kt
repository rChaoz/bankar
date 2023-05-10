package ro.bankar.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A Shared flow with the option to request emitting
 */
abstract class RequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    protected val flow: MutableSharedFlow<EmissionResult<T>> = MutableSharedFlow(replay = 1)
) : SharedFlow<RequestFlow.EmissionResult<T>> by flow.asSharedFlow() {
    sealed class EmissionResult<T> {
        class Fail<T>(val continuation: Continuation<Unit>?) : EmissionResult<T>()
        class Success<T>(val value: T) : EmissionResult<T>()
    }

    fun requestEmit(continuation: Continuation<Unit>? = null) {
        scope.launch { onEmissionRequest(continuation) }
    }

    suspend fun emitNow() = suspendCoroutine {
        scope.launch { onEmissionRequest(it) }
    }

    protected abstract suspend fun onEmissionRequest(continuation: Continuation<Unit>?)
}

@Composable
fun <T> RequestFlow<T>.collectAsStateRetrying() = mapCollectAsStateRetrying { it }

@Composable
fun <T, V> RequestFlow<T>.mapCollectAsStateRetrying(mapFunction: (T) -> V) = produceState<V?>(null) {
    collect {
        when (it) {
            is RequestFlow.EmissionResult.Fail -> requestEmit(it.continuation)
            is RequestFlow.EmissionResult.Success -> value = mapFunction(it.value)
        }
    }
}

suspend fun <T> RequestFlow<T>.collectRetrying(collector: FlowCollector<T>): Nothing = collect {
    when (it) {
        is RequestFlow.EmissionResult.Fail -> requestEmit(it.continuation)
        is RequestFlow.EmissionResult.Success -> collector.emit(it.value)
    }
}

// LocalRepository defined in debug/release source sets

fun repository(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository = RepositoryImpl(scope, sessionToken, onSessionExpire)

abstract class Repository(protected val scope: CoroutineScope, protected val sessionToken: String, protected val onSessionExpire: () -> Unit) {
    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): SafeStatusResponse<StatusResponse, InvalidParamResponse>
    abstract suspend fun sendAddFriend(id: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract val friends: RequestFlow<List<SPublicUser>>
    abstract val friendRequests: RequestFlow<List<SPublicUser>>
    abstract suspend fun sendFriendRequestResponse(tag: String, accept: Boolean): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract suspend fun sendCancelFriendRequest(tag: String): SafeStatusResponse<StatusResponse, StatusResponse>

    // Recent activity
    abstract val recentActivity: RequestFlow<SRecentActivity>
    abstract val allRecentActivity: RequestFlow<SRecentActivity>

    // Bank accounts
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
    abstract suspend fun sendCreateAccount(account: SNewBankAccount): SafeStatusResponse<StatusResponse, InvalidParamResponse>
}

private class RepositoryImpl(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit) : Repository(scope, sessionToken, onSessionExpire) {
    // User profile & friends
    override val profile = createFlow<SUser>("profile")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = ktorClient.safeStatusRequest<StatusResponse, InvalidParamResponse> {
        put("profile/update") {
            bearerAuth(sessionToken)
            setBody(data)
        }
    }

    override suspend fun sendAddFriend(id: String) = ktorClient.safeGet<StatusResponse, StatusResponse>(HttpStatusCode.OK) {
        url("profile/friends/add/$id")
        bearerAuth(sessionToken)
    }

    override val friends = createFlow<List<SPublicUser>>("profile/friends")
    override val friendRequests = createFlow<List<SPublicUser>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = ktorClient.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag")
        bearerAuth(sessionToken)
    }

    override suspend fun sendCancelFriendRequest(tag: String) = ktorClient.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/cancel/$tag")
        bearerAuth(sessionToken)
    }

    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recent")
    override val allRecentActivity = createFlow<SRecentActivity>("recent?count=100000")

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
        // Home page (and profile)
        profile.requestEmit()
        accounts.requestEmit()
        recentActivity.requestEmit()
        // Friends page
        friends.requestEmit()
        friendRequests.requestEmit()
    }

    // Utility functions
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(continuation: Continuation<Unit>?) {
            val r = ktorClient.safeRequest<T> {
                get(url) {
                    bearerAuth(sessionToken)
                }
            }
            when (r) {
                is SafeResponse.InternalError -> flow.emit(EmissionResult.Fail(continuation))
                is SafeResponse.Fail -> {
                    if (r.r.status == HttpStatusCode.Unauthorized || r.r.status == HttpStatusCode.Forbidden) onSessionExpire()
                    else flow.emit(EmissionResult.Fail(continuation))
                }
                is SafeResponse.Success -> {
                    flow.emit(EmissionResult.Success(r.result))
                    continuation?.resume(Unit)
                }
            }
        }
    }
}