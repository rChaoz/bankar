package ro.bankar.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SCountries
import ro.bankar.model.SInitialLoginData
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SSendRequestMoney
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

abstract class Repository {
    // Country data & password check
    abstract val countryData: RequestFlow<SCountries>
    abstract suspend fun sendCheckPassword(password: String): SafeStatusResponse<StatusResponse, StatusResponse>
    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): SafeStatusResponse<StatusResponse, InvalidParamResponse>
    abstract suspend fun sendAddFriend(id: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract suspend fun sendRemoveFriend(tag: String): SafeStatusResponse<StatusResponse, StatusResponse>
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
    abstract suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): SafeResponse<StatusResponse>
    abstract suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): SafeResponse<StatusResponse>

    // Load data on Repository creation to avoid having to wait when going to each screen
    protected fun init() {
        countryData.requestEmit()
        // Home page (and profile)
        profile.requestEmit()
        accounts.requestEmit()
        recentActivity.requestEmit()
        // Friends page
        friends.requestEmit()
        friendRequests.requestEmit()
    }
}

private class RepositoryImpl(private val scope: CoroutineScope, sessionToken: String, private val onSessionExpire: () -> Unit) : Repository() {
    private val client = HttpClient(OkHttp) {
        defaultRequest {
            configUrl()
            bearerAuth(sessionToken)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json()
        }
    }

    // Country data & password check
    override val countryData = createFlow<SCountries>("data/countries.json")
    override suspend fun sendCheckPassword(password: String) = client.safePost<StatusResponse, StatusResponse> {
        url("verifyPassword")
        setBody(SInitialLoginData("", password))
    }
    // User profile & friends
    override val profile = createFlow<SUser>("profile")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = client.safeStatusRequest<StatusResponse, InvalidParamResponse> {
        put("profile/update") {
            setBody(data)
        }
    }

    override suspend fun sendAddFriend(id: String) = client.safeGet<StatusResponse, StatusResponse>(HttpStatusCode.OK) {
        url("profile/friends/add/$id")
    }

    override suspend fun sendRemoveFriend(tag: String) = client.safeGet<StatusResponse, StatusResponse>(HttpStatusCode.OK) {
        url("profile/friends/remove/$tag")
    }

    override val friends = createFlow<List<SPublicUser>>("profile/friends")
    override val friendRequests = createFlow<List<SPublicUser>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = client.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag")
    }

    override suspend fun sendCancelFriendRequest(tag: String) = client.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/cancel/$tag")
    }

    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recent")
    override val allRecentActivity = createFlow<SRecentActivity>("recent?count=100000")

    // Bank accounts
    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = client.safePost<StatusResponse, InvalidParamResponse>(HttpStatusCode.Created) {
        url("accounts/new")
        setBody(account)
    }
    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<StatusResponse> {
        post("transfer/send") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
        }
    }
    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<StatusResponse> {
        post("transfer/request") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
        }
    }

    // Utility functions
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(continuation: Continuation<Unit>?) {
            when (val r = client.safeRequest<T> { get(url) }) {
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

    init {
        init()
    }
}