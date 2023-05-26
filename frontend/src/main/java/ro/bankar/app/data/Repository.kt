package ro.bankar.app.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import ro.bankar.banking.SCountries
import ro.bankar.banking.SExchangeData
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SConversation
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewUser
import ro.bankar.model.SPasswordData
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SSendMessage
import ro.bankar.model.SSendRequestMoney
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.seconds

/**
 * A Shared flow with the option to request emitting
 */
abstract class RequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    protected val flow: MutableSharedFlow<EmissionResult<T>> = MutableSharedFlow(replay = 1)
) : SharedFlow<RequestFlow.EmissionResult<T>> by flow.asSharedFlow() {
    sealed class EmissionResult<T> {
        class Fail<T>(val continuation: Continuation<Unit>?) : EmissionResult<T>() {
            val hasRetried = AtomicBoolean(false)
        }
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
            is RequestFlow.EmissionResult.Fail -> {
                delay(2.seconds)
                if (it.hasRetried.compareAndSet(false, true)) requestEmit(it.continuation)
            }
            is RequestFlow.EmissionResult.Success -> value = mapFunction(it.value)
        }
    }
}

suspend fun <T> RequestFlow<T>.collectRetrying(collector: FlowCollector<T>): Nothing = collect {
    when (it) {
        is RequestFlow.EmissionResult.Fail -> {
            delay(2.seconds)
            if (it.hasRetried.compareAndSet(false, true)) requestEmit(it.continuation)
        }
        is RequestFlow.EmissionResult.Success -> collector.emit(it.value)
    }
}

// LocalRepository defined in debug/release source sets

fun repository(scope: CoroutineScope, sessionToken: String, onLogout: () -> Unit): Repository = RepositoryImpl(scope, sessionToken, onLogout)

abstract class Repository {
    // WebSocket for transmitting live data
    abstract val socket: DefaultClientWebSocketSession
    abstract val socketFlow: Flow<SSocketNotification>
    abstract suspend fun openAndMaintainSocket()

    // Static data & password check
    abstract val countryData: RequestFlow<SCountries>
    abstract val exchangeData: RequestFlow<SExchangeData>
    abstract suspend fun sendCheckPassword(password: String): SafeStatusResponse<StatusResponse, StatusResponse>

    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): SafeStatusResponse<StatusResponse, InvalidParamResponse>
    abstract suspend fun sendUpdate(data: SNewUser): SafeResponse<StatusResponse>
    abstract suspend fun sendAddFriend(id: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract suspend fun sendRemoveFriend(tag: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract val friends: RequestFlow<List<SFriend>>
    abstract val friendRequests: RequestFlow<List<SFriendRequest>>
    abstract suspend fun sendFriendRequestResponse(tag: String, accept: Boolean): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract suspend fun sendCancelFriendRequest(tag: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract fun conversation(tag: String): RequestFlow<SConversation>
    abstract suspend fun sendFriendMessage(recipientTag: String, message: String): SafeStatusResponse<StatusResponse, StatusResponse>

    // Recent activity
    abstract val recentActivity: RequestFlow<SRecentActivity>
    abstract val allRecentActivity: RequestFlow<SRecentActivity>

    // Bank accounts
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
    abstract suspend fun sendCreateAccount(account: SNewBankAccount): SafeStatusResponse<StatusResponse, InvalidParamResponse>
    abstract suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): SafeResponse<StatusResponse>
    abstract suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): SafeResponse<StatusResponse>
    abstract suspend fun sendCancelTransferRequest(id: Int): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?): SafeStatusResponse<StatusResponse, StatusResponse>

    abstract fun logout()

    // Load data on Repository creation to avoid having to wait when going to each screen
    protected fun init() {
        countryData.requestEmit()
        exchangeData.requestEmit()
        // Home page (and profile)
        profile.requestEmit()
        accounts.requestEmit()
        recentActivity.requestEmit()
        // Friends page
        friends.requestEmit()
        friendRequests.requestEmit()
    }
}

private class RepositoryImpl(private val scope: CoroutineScope, sessionToken: String, private val onLogout: () -> Unit) : Repository() {
    private val client = HttpClient(OkHttp) {
        defaultRequest {
            configUrl()
            bearerAuth(sessionToken)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json()
        }
        install(WebSockets) {
            pingInterval = 15_000L
            maxFrameSize = Int.MAX_VALUE.toLong()
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }

    // WebSocket
    override lateinit var socket: DefaultClientWebSocketSession
        private set
    private val socketMutableFlow = MutableSharedFlow<SSocketNotification>()
    override val socketFlow = socketMutableFlow.asSharedFlow()

    override suspend fun openAndMaintainSocket() {
        while (true) {
            try {
                client.webSocket(request = {
                    url(path = "socket") { setSocketProtocol() }
                }) {
                    socket = this
                    while (true) {
                        val data = receiveDeserialized<SSocketNotification>()
                        socketMutableFlow.emit(data)
                        when (data) {
                            SSocketNotification.STransferNotification -> {
                                recentActivity.requestEmit()
                                accounts.requestEmit()
                            }
                            SSocketNotification.SFriendNotification -> {
                                friends.requestEmit()
                                friendRequests.requestEmit()
                            }
                            SSocketNotification.SRecentActivityNotification -> recentActivity.emitNow()
                            is SSocketNotification.SMessageNotification -> {
                                friends.requestEmit()
                                socketMutableFlow.emit(data)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5.seconds)
        }
    }

    // Static data & password check
    override val countryData = createFlow<SCountries>("data/countries.json")
    override val exchangeData = createFlow<SExchangeData>("data/exchange.json")
    override suspend fun sendCheckPassword(password: String) = client.safePost<StatusResponse, StatusResponse> {
        url("verifyPassword")
        setBody(SPasswordData(password))
    }

    // User profile & friends
    override val profile = createFlow<SUser>("profile")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = client.safeStatusRequest<StatusResponse, InvalidParamResponse> {
        put("profile/update") {
            setBody(data)
        }
    }

    override suspend fun sendUpdate(data: SNewUser) = client.safeRequest<StatusResponse> {
        put("updateAccount") { setBody(data) }
    }

    override suspend fun sendAddFriend(id: String) = client.safeGet<StatusResponse, StatusResponse>(HttpStatusCode.OK) {
        url("profile/friends/add/$id")
    }

    override suspend fun sendRemoveFriend(tag: String) = client.safeGet<StatusResponse, StatusResponse>(HttpStatusCode.OK) {
        url("profile/friends/remove/$tag")
    }

    override val friends = createFlow<List<SFriend>>("profile/friends")
    override val friendRequests = createFlow<List<SFriendRequest>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = client.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag")
    }

    override suspend fun sendCancelFriendRequest(tag: String) = client.safeGet<StatusResponse, StatusResponse> {
        url("profile/friend_requests/cancel/$tag")
    }

    override fun conversation(tag: String) = createFlow<SConversation>("messaging/conversation/$tag")
    override suspend fun sendFriendMessage(recipientTag: String, message: String) = client.safePost<StatusResponse, StatusResponse> {
        url("messaging/send")
        setBody(SSendMessage(message, recipientTag))
    }


    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recent")
    override val allRecentActivity = createFlow<SRecentActivity>("recent?count=100000")

    // Bank accounts
    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) =
        client.safePost<StatusResponse, InvalidParamResponse>(HttpStatusCode.Created) {
            url("accounts/new")
            setBody(account)
        }

    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<StatusResponse> {
        post("transfer/send") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
        }
    }

    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) =
        client.safeRequest<StatusResponse> {
            post("transfer/request") {
                setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
            }
        }

    override suspend fun sendCancelTransferRequest(id: Int) = client.safeGet<StatusResponse, StatusResponse> {
        url("transfer/cancel/$id")
    }

    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = client.safeGet<StatusResponse, StatusResponse> {
        url(path = "transfer/respond/$id") {
            parameter("action", if (accept) "accept" else "decline")
            parameter("accountID", sourceAccountID)
        }
    }

    // Utility functions
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(continuation: Continuation<Unit>?) {
            when (val r = client.safeRequest<T> { get(url) }) {
                is SafeResponse.InternalError -> flow.emit(EmissionResult.Fail(continuation))
                is SafeResponse.Fail -> {
                    if (r.r.status == HttpStatusCode.Unauthorized || r.r.status == HttpStatusCode.Forbidden) onLogout()
                    else flow.emit(EmissionResult.Fail(continuation))
                }

                is SafeResponse.Success -> {
                    flow.emit(EmissionResult.Success(r.result))
                    continuation?.resume(Unit)
                }
            }
        }
    }

    override fun logout() {
        scope.launch { client.safeGet<StatusResponse, StatusResponse> { url("signout") } }
        onLogout()
    }

    init {
        init()
    }
}