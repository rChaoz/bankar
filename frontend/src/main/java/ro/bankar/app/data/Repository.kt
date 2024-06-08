package ro.bankar.app.data

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.datastore.core.DataStore
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.timeout
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import ro.bankar.app.TAG
import ro.bankar.banking.SCountries
import ro.bankar.banking.SCreditData
import ro.bankar.banking.SExchangeData
import ro.bankar.model.Response
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SConversation
import ro.bankar.model.SCreateParty
import ro.bankar.model.SCustomiseBankAccount
import ro.bankar.model.SDefaultBankAccount
import ro.bankar.model.SExternalTransfer
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SMessagingToken
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewUser
import ro.bankar.model.SOwnTransfer
import ro.bankar.model.SPartyInformation
import ro.bankar.model.SPasswordData
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SSendMessage
import ro.bankar.model.SSendRequestMoney
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SStatement
import ro.bankar.model.SStatementRequest
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.ValueResponse
import ro.bankar.util.dashFormat
import ro.bankar.util.here
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * A Shared flow with the option to request emitting
 */
interface RequestFlow<T> : SharedFlow<T> {
    fun requestEmit()

    suspend fun emitNow(): T
}


abstract class AbstractRequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    private val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : RequestFlow<T>, SharedFlow<T> by flow.asSharedFlow() {
    override fun requestEmit() {
        scope.launch { emitNow() }
    }

    override suspend fun emitNow(): T {
        var timer = 1
        var result = emit()
        while (result == null) {
            if (timer < 10) ++timer
            delay(timer.seconds)
            result = emit()
        }
        flow.emit(result)
        return result
    }

    protected abstract suspend fun emit(): T?
}

/**
 * A request flow cached locally
 */
abstract class CachedRequestFlow<T> protected constructor(
    protected val scope: CoroutineScope,
    protected val cache: DataStore<Cache>,
    protected val mapFunc: (Cache) -> T?,
    protected val updateFunc: Cache.(T) -> Cache
) : RequestFlow<T>, SharedFlow<T> by (MutableSharedFlow<T>(replay = 1).apply {
    scope.launch(Dispatchers.IO) {
        cache.data.map(mapFunc).collect {
            Log.d(TAG, "Emitting cached value: $it")
            if (it != null) emit(it)
        }
    }
}).asSharedFlow() {
    override fun requestEmit() {
        scope.launch { emitNow() }
    }

    override suspend fun emitNow(): T {
        var timer = 1
        var result = emit()
        while (result == null) {
            if (timer < 10) ++timer
            delay(timer.seconds)
            result = emit()
        }
        withContext(Dispatchers.IO) {
            cache.updateData { it.updateFunc(result) }
        }
        return result
    }

    protected abstract suspend fun emit(): T?
}

abstract class UpdateRequestFlow<T> protected constructor(
    scope: CoroutineScope,
    cache: DataStore<Cache>,
    mapFunc: (Cache) -> T?,
    updateFunc: Cache.(T) -> Cache
) : CachedRequestFlow<T>(scope, cache, mapFunc, updateFunc) {
    fun sendUpdate(update: suspend T?.() -> T) = scope.launch { updateNow(update) }
    suspend fun updateNow(update: suspend T.() -> T) = withContext(Dispatchers.IO) {
        var shouldEmit = false
        cache.updateData {
            val old = mapFunc(it)
            if (old == null) {
                shouldEmit = true
                it
            } else it.updateFunc(update(old))
        }
        if (shouldEmit) emitNow()
    }
}

// LocalRepository defined in debug/release source sets
fun repository(scope: CoroutineScope, sessionToken: String, cache: DataStore<Cache>, onLogout: () -> Unit): Repository =
    RepositoryImpl(scope, sessionToken, cache, onLogout)

abstract class Repository {
    // WebSocket for transmitting live data
    abstract val socket: DefaultClientWebSocketSession
    abstract val socketFlow: Flow<SSocketNotification>
    abstract suspend fun openAndMaintainSocket()

    // Static data & password check
    abstract val countryData: RequestFlow<SCountries>
    abstract val exchangeData: RequestFlow<SExchangeData>
    abstract val creditData: RequestFlow<List<SCreditData>>
    abstract suspend fun sendCheckPassword(password: String): ResponseRequestResult<Unit>

    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): ResponseRequestResult<Unit>
    abstract suspend fun sendUpdate(data: SNewUser): ResponseRequestResult<Unit>
    abstract suspend fun sendAddFriend(id: String): ResponseRequestResult<Unit>
    abstract suspend fun sendRemoveFriend(tag: String): ResponseRequestResult<Unit>
    abstract val friends: RequestFlow<List<SFriend>>
    abstract val friendRequests: RequestFlow<List<SFriendRequest>>
    abstract suspend fun sendFriendRequestResponse(tag: String, accept: Boolean): ResponseRequestResult<Unit>
    abstract suspend fun sendCancelFriendRequest(tag: String): ResponseRequestResult<Unit>
    abstract fun conversation(tag: String): RequestFlow<SConversation>
    abstract suspend fun sendFriendMessage(recipientTag: String, message: String): ResponseRequestResult<Unit>

    // Parties
    abstract suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>): ResponseRequestResult<Unit>
    abstract fun partyData(id: Int): RequestFlow<SPartyInformation>
    abstract suspend fun sendCancelParty(id: Int): ResponseRequestResult<Unit>

    // Recent activity
    abstract val recentActivity: RequestFlow<SRecentActivity>
    abstract val allRecentActivity: RequestFlow<SRecentActivity>
    abstract fun recentActivityWith(tag: String): RequestFlow<List<SBankTransfer>>

    // Bank accounts
    abstract val defaultAccount: RequestFlow<SDefaultBankAccount>
    abstract suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean): ResponseRequestResult<Unit>
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
    abstract suspend fun sendCreateAccount(account: SNewBankAccount): ResponseRequestResult<Unit>
    abstract suspend fun sendCustomiseAccount(id: Int, name: String, color: Int): ResponseRequestResult<Unit>
    abstract suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): ResponseRequestResult<String>
    abstract suspend fun sendOwnTransfer(sourceAccount: SBankAccount, targetAccount: SBankAccount, amount: Double, note: String): ResponseRequestResult<Unit>
    abstract suspend fun sendExternalTransfer(sourceAccount: SBankAccount, targetIBAN: String, amount: Double, note: String): ResponseRequestResult<Unit>
    abstract suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): ResponseRequestResult<String>
    abstract suspend fun sendCancelTransferRequest(id: Int): ResponseRequestResult<Unit>
    abstract suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?): ResponseRequestResult<Unit>
    abstract val statements: RequestFlow<List<SStatement>>
    abstract suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate): ResponseRequestResult<SStatement>
    abstract fun createDownloadStatementRequest(statement: SStatement): DownloadManager.Request

    abstract fun logout()
    abstract fun initNotifications()

    // Load data on Repository creation to avoid having to wait when going to each screen
    protected fun init() {
        countryData.requestEmit()
        exchangeData.requestEmit()
        creditData.requestEmit()
        defaultAccount.requestEmit()
        // Home page (and profile)
        profile.requestEmit()
        accounts.requestEmit()
        recentActivity.requestEmit()
        // Friends page
        friends.requestEmit()
        friendRequests.requestEmit()
        // Account statements
        statements.requestEmit()
        // Initialize push notifications
        initNotifications()
    }
}

private class RepositoryImpl(
    private val scope: CoroutineScope,
    private val sessionToken: String,
    private val cache: DataStore<Cache>,
    private val onLogout: () -> Unit,
) : Repository() {

    private val client = HttpClient(OkHttp) {
        defaultRequest {
            configUrl()
            bearerAuth(sessionToken)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json()
        }
        install(HttpTimeout)
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
                if (e is CancellationException) throw e
                e.printStackTrace()
            }
            delay(5.seconds)
        }
    }

    // Static data & password check
    override val countryData = createFlow<SCountries>("data/countries.json", raw = true)
    override val exchangeData = createFlow<SExchangeData>("data/exchange.json", raw = true)
    override val creditData = createFlow<List<SCreditData>>("data/credit.json", raw = true)
    override suspend fun sendCheckPassword(password: String) = client.safeRequest<Unit> {
        post("verifyPassword") { setBody(SPasswordData(password))
            configureTimeout() }
    }

    // User profile & friends
    override val profile = createUpdateFlow<SUser>("profile", { it.profile }, { copy(profile = it) })
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = client.safeRequest<Unit> {
        put("profile/update") {
            setBody(data)
            configureTimeout()
        }
    }.onSuccess { profile.updateNow { copy(about = data.about ?: about, avatar = data.avatar ?: avatar) } }

    override suspend fun sendUpdate(data: SNewUser) = client.safeRequest<Unit> {
        put("updateAccount") {
            setBody(data)
            configureTimeout()
        }
    }.onSuccess {
        profile.updateNow {
            copy(
                data.email,
                data.tag,
                data.phone,
                data.firstName,
                data.middleName,
                data.lastName,
                data.dateOfBirth,
                data.countryCode,
                data.state,
                data.city,
                data.address
            )
        }
    }

    override suspend fun sendAddFriend(id: String) = client.safeRequest<Unit> {
        get("profile/friends/add/$id") { configureTimeout() }
    }

    override suspend fun sendRemoveFriend(tag: String) = client.safeRequest<Unit> {
        get("profile/friends/remove/$tag") { configureTimeout() }
    }

    override val friends = createFlow<List<SFriend>>("profile/friends")
    override val friendRequests = createFlow<List<SFriendRequest>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = client.safeRequest<Unit> {
        get("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag") { configureTimeout() }
    }

    override suspend fun sendCancelFriendRequest(tag: String) = client.safeRequest<Unit> {
        get("profile/friend_requests/cancel/$tag") { configureTimeout() }
    }

    override fun conversation(tag: String) = createFlow<SConversation>("messaging/conversation/$tag")
    override suspend fun sendFriendMessage(recipientTag: String, message: String) = client.safeRequest<Unit> {
        post("messaging/send") {
            setBody(SSendMessage(message, recipientTag))
            configureTimeout()
        }
    }

    // Parties
    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) = client.safeRequest<Unit> {
        post("party/create") {
            setBody(SCreateParty(account, note, amounts))
            configureTimeout()
        }
    }

    override fun partyData(id: Int) = createFlow<SPartyInformation>("party/$id")
    override suspend fun sendCancelParty(id: Int) = client.safeRequest<Unit> { get("party/cancel/$id") }


    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recentActivity/short")
    override val allRecentActivity = createFlow<SRecentActivity>("recentActivity/long")
    override fun recentActivityWith(tag: String) = createFlow<List<SBankTransfer>>("transfer/list/$tag")

    // Bank accounts
    override val defaultAccount = createFlow<SDefaultBankAccount>("defaultAccount")
    override suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean) =
        client.safeRequest<Unit> {
            post("defaultAccount") {
                setBody(SDefaultBankAccount(id, alwaysUse))
                configureTimeout()
            }
        }

    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = client.safeRequest<Unit> {
        post("accounts/new") {
            setBody(account)
            configureTimeout()
        }
    }

    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = client.safeRequest<Unit> {
        post("accounts/$id/customise") {
            setBody(SCustomiseBankAccount(name, color))
            configureTimeout()
        }
    }

    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/send") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
            configureTimeout()
        }
    }

    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/request") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
            configureTimeout()
        }
    }

    override suspend fun sendOwnTransfer(sourceAccount: SBankAccount, targetAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<Unit> {
        post("transfer/own") {
            setBody(SOwnTransfer(sourceAccount.id, targetAccount.id, amount, sourceAccount.currency != targetAccount.currency, note))
            configureTimeout()
        }
    }

    override suspend fun sendExternalTransfer(sourceAccount: SBankAccount, targetIBAN: String, amount: Double, note: String) = client.safeRequest<Unit> {
        post("transfer/external") {
            setBody(SExternalTransfer(sourceAccount.id, targetIBAN, amount, note))
            configureTimeout()
        }
    }

    override suspend fun sendCancelTransferRequest(id: Int) = client.safeRequest<Unit> {
        get("transfer/cancel/$id") { configureTimeout() }
    }

    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = client.safeRequest<Unit> {
        get("transfer/respond/$id") {
            parameter("action", if (accept) "accept" else "decline")
            parameter("accountID", sourceAccountID)
            configureTimeout()
        }
    }

    override val statements = createFlow<List<SStatement>>("statements")
    override suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate) =
        client.safeRequest<SStatement> {
            post("statements/request") {
                setBody(SStatementRequest(name, accountID, from, to, TimeZone.currentSystemDefault()))
                configureTimeout()
            }
        }

    override fun createDownloadStatementRequest(statement: SStatement) = DownloadManager.Request(statement.downloadURI).apply {
        val name = "Statement-${statement.timestamp.here().dashFormat()}.pdf"
        setDestinationUri(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)))
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        addRequestHeader(HttpHeaders.Authorization, "Bearer $sessionToken")
    }

    // Request utility
    private fun HttpRequestBuilder.configureTimeout() {
        timeout {
            requestTimeoutMillis = 10_000
        }
    }

    // Flow utilities
    private suspend inline fun <reified T> getRequest(url: String, raw: Boolean): T? = try {
        val result = client.get(url)
        when {
            result.status == HttpStatusCode.Unauthorized -> {
                onLogout()
                null
            }
            raw -> result.body<T>()
            else -> {
                val response = result.body<Response<T>>()
                if (response is ValueResponse) response.value
                else {
                    Log.w(TAG, "Invalid server response for flow \"$url\": $response")
                    null
                }
            }
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Log.w(TAG, "Exception in repository flow", e)
        null
    }

    private inline fun <reified T> createFlow(url: String, raw: Boolean = false) = object : AbstractRequestFlow<T>(scope) {
        override suspend fun emit(): T? = getRequest(url, raw)
    }

    private inline fun <reified T> createCachedFlow(
        url: String,
        noinline mapFunc: (Cache) -> T?,
        noinline updateFunc: Cache.(T) -> Cache,
        raw: Boolean = false
    ) = object : CachedRequestFlow<T>(scope, cache, mapFunc, updateFunc) {
        override suspend fun emit(): T? = getRequest(url, raw)
    }

    private inline fun <reified T> createUpdateFlow(url: String, noinline mapFunc: (Cache) -> T?, noinline updateFunc: Cache.(T) -> Cache) =
        object : UpdateRequestFlow<T>(scope, cache, mapFunc, updateFunc) {
            override suspend fun emit(): T? = getRequest(url, false)
        }


    override fun logout() {
        scope.launch { client.safeRequest<Unit> { get("signOut") } }
        onLogout()
    }

    override fun initNotifications() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            scope.launch { client.safeRequest<Unit> { post("messaging/register") { setBody(SMessagingToken(it)) } } }
        }
    }

    init {
        init()
    }
}