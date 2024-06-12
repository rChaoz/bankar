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
import io.ktor.client.request.delete
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import ro.bankar.model.SBankCard
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
import ro.bankar.model.SNewBankCard
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
    /**
     * Request for a new value to be emitted, without waiting for it.
     */
    fun requestEmit()

    /**
     * Request and wait for a new value to be emitted.
     * Cancellation of the caller will not cancel the emission of the new value.
     */
    suspend fun requestEmitNow(): T
}

/**
 * Implementation of [RequestFlow] using a coroutine scope to asynchronously.
 */
abstract class SharedRequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    private val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : RequestFlow<T>, SharedFlow<T> by flow {

    override fun requestEmit() {
        @Suppress("DeferredResultUnused")
        doEmit()
    }

    override suspend fun requestEmitNow() = doEmit().await()

    private fun doEmit(): Deferred<T> = scope.async {
        var timer = 1
        var result = produceValue()
        while (result == null) {
            if (timer < 10) ++timer
            delay(timer.seconds)
            result = produceValue()
        }
        flow.emit(result)
        result
    }

    /**
     * Function that produces values.
     * Returning null means a value cannot be produced right now, causing a retry attempt later.
     */
    protected abstract suspend fun produceValue(): T?
}

/**
 * A request flow cached locally using a [DataStore].
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class CachedRequestFlow<T, C> protected constructor(
    protected val scope: CoroutineScope,
    protected val cache: DataStore<C>,
    protected val mapFunc: (C) -> T?,
    protected val updateFunc: C.(T) -> C
) : RequestFlow<T>, SharedFlow<T> by (MutableSharedFlow<T>(replay = 1).apply {
    scope.launch(Dispatchers.IO) {
        cache.data.map(mapFunc).collect {
            Log.d(TAG, "Emitting cached value: $it")
            if (it != null) emit(it)
        }
    }
}).asSharedFlow() {
    override fun requestEmit() {
        @Suppress("DeferredResultUnused")
        doEmit()
    }

    override suspend fun requestEmitNow() = doEmit().await()

    private fun doEmit(): Deferred<T> = scope.async {
        var timer = 1
        var result = produceValue()
        while (result == null) {
            if (timer < 10) ++timer
            delay(timer.seconds)
            result = produceValue()
        }
        withContext(Dispatchers.IO) {
            cache.updateData { it.updateFunc(result) }
        }
        result
    }

    protected abstract suspend fun produceValue(): T?
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
    abstract suspend fun sendProfileUpdate(data: SNewUser): ResponseRequestResult<Unit>
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
    abstract suspend fun sendCloseAccount(account: SBankAccount): ResponseRequestResult<Unit>
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

    // Cards
    abstract suspend fun sendCreateCard(accountID: Int, name: String): ResponseRequestResult<Int>
    abstract suspend fun sendUpdateCard(accountID: Int, cardID: Int, name: String, limit: Double): ResponseRequestResult<Unit>
    abstract suspend fun sendResetCardLimit(accountID: Int, cardID: Int): ResponseRequestResult<Unit>
    abstract fun card(accountID: Int, cardID: Int): RequestFlow<SBankCard>

    abstract fun logout()
    abstract fun initNotifications()
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
                            SSocketNotification.SRecentActivityNotification -> recentActivity.requestEmitNow()
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

    private val namedFlows = mutableMapOf<Pair<String, String>, RequestFlow<*>>()

    // Static data & password check
    override val countryData = createFlow<SCountries>("data/countries.json", raw = true)
    override val exchangeData = createFlow<SExchangeData>("data/exchange.json", raw = true)
    override val creditData = createFlow<List<SCreditData>>("data/credit.json", raw = true)
    override suspend fun sendCheckPassword(password: String) = client.safeRequest<Unit> {
        post("verifyPassword") {
            setBody(SPasswordData(password))
            configureTimeout()
        }
    }

    // User profile & friends
    override val profile = createCachedFlow<SUser>("profile", { it.profile }, { copy(profile = it) })
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = client.safeRequest<Unit> {
        put("profile/update") {
            setBody(data)
            configureTimeout()
        }
    }.onSuccess { profile.requestEmitNow() }

    override suspend fun sendProfileUpdate(data: SNewUser) = client.safeRequest<Unit> {
        put("updateAccount") {
            setBody(data)
            configureTimeout()
        }
    }.onSuccess { profile.requestEmitNow() }

    override suspend fun sendAddFriend(id: String) = client.safeRequest<Unit> {
        get("profile/friends/add/$id") { configureTimeout() }
    }.onSuccess { friendRequests.requestEmitNow() }

    override suspend fun sendRemoveFriend(tag: String) = client.safeRequest<Unit> {
        get("profile/friends/remove/$tag") { configureTimeout() }
    }.onSuccess { friends.requestEmitNow() }

    override val friends = createCachedFlow<List<SFriend>>("profile/friends", { it.friends }, { copy(friends = it) })
    override val friendRequests = createFlow<List<SFriendRequest>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = client.safeRequest<Unit> {
        get("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag") { configureTimeout() }
    }.onSuccess { friendRequests.requestEmitNow(); if (accept) friends.requestEmitNow() }

    override suspend fun sendCancelFriendRequest(tag: String) = client.safeRequest<Unit> {
        get("profile/friend_requests/cancel/$tag") { configureTimeout() }
    }.onSuccess { friendRequests.requestEmitNow() }

    override fun conversation(tag: String) = getNamedCachedFlow<SConversation>(
        "conversation",
        tag,
        "messaging/conversation/$tag",
        { it.conversations[tag] },
        { copy(conversations = conversations + (tag to it)) }
    )

    override suspend fun sendFriendMessage(recipientTag: String, message: String) = client.safeRequest<Unit> {
        post("messaging/send") {
            setBody(SSendMessage(message, recipientTag))
            configureTimeout()
        }
    }.onSuccess { conversation(recipientTag).requestEmit() }

    // Parties
    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) = client.safeRequest<Unit> {
        post("party/create") {
            setBody(SCreateParty(account, note, amounts))
            configureTimeout()
        }
    }.onSuccess { recentActivity.requestEmitNow() }

    override fun partyData(id: Int) = getNamedFlow<SPartyInformation>("party", id.toString(), "party/$id")
    override suspend fun sendCancelParty(id: Int) = client.safeRequest<Unit> {
        get("party/cancel/$id")
    }.onSuccess { recentActivity.requestEmitNow() }


    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recentActivity/short")
    override val allRecentActivity = createFlow<SRecentActivity>("recentActivity/long")
    override fun recentActivityWith(tag: String) = getNamedFlow<List<SBankTransfer>>("recentActivity", tag, "transfer/list/$tag")

    // Bank accounts
    override val defaultAccount = createFlow<SDefaultBankAccount>("defaultAccount")
    override suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean) =
        client.safeRequest<Unit> {
            post("defaultAccount") {
                setBody(SDefaultBankAccount(id, alwaysUse))
                configureTimeout()
            }
        }.onSuccess { defaultAccount.requestEmitNow() }

    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = getNamedFlow<SBankAccountData>("account", id.toString(), "accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = client.safeRequest<Unit> {
        post("accounts/new") {
            setBody(account)
            configureTimeout()
        }
    }.onSuccess { accounts.requestEmitNow() }

    override suspend fun sendCloseAccount(account: SBankAccount) = client.safeRequest<Unit> {
        delete("accounts/${account.id}") { configureTimeout() }
    }.onSuccess { accounts.requestEmitNow() }

    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = client.safeRequest<Unit> {
        post("accounts/$id/customise") {
            setBody(SCustomiseBankAccount(name, color))
            configureTimeout()
        }
    }.onSuccess { accounts.requestEmitNow() }

    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/send") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
            configureTimeout()
        }
    }.onSuccess {
        coroutineScope {
            launch { recentActivity.requestEmitNow() }
            launch { accounts.requestEmitNow() }
        }
    }

    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/request") {
            setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note))
            configureTimeout()
        }
    }.onSuccess { recentActivity.requestEmitNow() }

    override suspend fun sendOwnTransfer(sourceAccount: SBankAccount, targetAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<Unit> {
        post("transfer/own") {
            setBody(SOwnTransfer(sourceAccount.id, targetAccount.id, amount, sourceAccount.currency != targetAccount.currency, note))
            configureTimeout()
        }
    }.onSuccess {
        coroutineScope {
            launch { recentActivity.requestEmit() }
            launch { accounts.requestEmit() }
        }
    }

    override suspend fun sendExternalTransfer(sourceAccount: SBankAccount, targetIBAN: String, amount: Double, note: String) = client.safeRequest<Unit> {
        post("transfer/external") {
            setBody(SExternalTransfer(sourceAccount.id, targetIBAN, amount, note))
            configureTimeout()
        }
    }.onSuccess {
        coroutineScope {
            launch { recentActivity.requestEmit() }
            launch { accounts.requestEmit() }
        }
    }

    override suspend fun sendCancelTransferRequest(id: Int) = client.safeRequest<Unit> {
        get("transfer/cancel/$id") { configureTimeout() }
    }.onSuccess {
        coroutineScope {
            launch { recentActivity.requestEmitNow() }
            launch { accounts.requestEmitNow() }
        }
    }

    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = client.safeRequest<Unit> {
        get("transfer/respond/$id") {
            parameter("action", if (accept) "accept" else "decline")
            parameter("accountID", sourceAccountID)
            configureTimeout()
        }
    }.onSuccess {
        coroutineScope {
            launch { recentActivity.requestEmitNow() }
            if (accept) launch { accounts.requestEmitNow() }
            if (sourceAccountID != null) launch { account(sourceAccountID).requestEmitNow() }
        }
    }

    override val statements = createFlow<List<SStatement>>("statements")
    override suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate) = client.safeRequest<SStatement> {
        post("statements/request") {
            setBody(SStatementRequest(name, accountID, from, to, TimeZone.currentSystemDefault()))
            configureTimeout()
        }
    }.onSuccess { statements.requestEmitNow() }

    override fun createDownloadStatementRequest(statement: SStatement) = DownloadManager.Request(statement.downloadURI).apply {
        val name = "Statement-${statement.timestamp.here().dashFormat()}.pdf"
        setDestinationUri(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)))
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        addRequestHeader(HttpHeaders.Authorization, "Bearer $sessionToken")
    }

    // Cards
    override suspend fun sendCreateCard(accountID: Int, name: String) = client.safeRequest<Int> {
        post("accounts/$accountID/new") {
            setBody(SNewBankCard(name, 0.0))
            configureTimeout()
        }
    }.onSuccess { account(accountID).requestEmitNow() }

    override suspend fun sendUpdateCard(accountID: Int, cardID: Int, name: String, limit: Double) = client.safeRequest<Unit> {
        post("accounts/$accountID/$cardID") {
            setBody(SNewBankCard(name, limit))
            configureTimeout()
        }
    }.onSuccess {
        coroutineScope {
            launch { account(accountID).requestEmitNow() }
            launch { card(accountID, cardID).requestEmitNow() }
        }
    }

    override suspend fun sendResetCardLimit(accountID: Int, cardID: Int) = client.safeRequest<Unit> {
        post("accounts/$accountID/$cardID/reset_limit") { configureTimeout() }
    }.onSuccess {
        coroutineScope {
            launch { account(accountID).requestEmitNow() }
            launch { card(accountID, cardID).requestEmitNow() }
        }
    }

    override fun card(accountID: Int, cardID: Int) = getNamedFlow<SBankCard>("card", "$accountID:$cardID", "accounts/$accountID/$cardID")

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

    private inline fun <reified T> createFlow(url: String, raw: Boolean = false) = object : SharedRequestFlow<T>(scope) {
        override suspend fun produceValue(): T? = getRequest(url, raw)
    }.also { it.requestEmit() }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> getNamedFlow(type: String, name: String, url: String) = namedFlows.getOrPut(type to name) {
        createFlow<T>(url)
    } as RequestFlow<T>

    private inline fun <reified T> createCachedFlow(
        url: String,
        noinline mapFunc: (Cache) -> T?,
        noinline updateFunc: Cache.(T) -> Cache,
    ) = object : CachedRequestFlow<T, Cache>(scope, cache, mapFunc, updateFunc) {
        override suspend fun produceValue(): T? = getRequest(url, false)
    }.also { it.requestEmit() }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> getNamedCachedFlow(
        type: String,
        name: String,
        url: String,
        noinline mapFunc: (Cache) -> T?,
        noinline updateFunc: Cache.(T) -> Cache,
    ) = namedFlows.getOrPut(type to name) {
        createCachedFlow<T>(url, mapFunc, updateFunc)
    } as RequestFlow<T>

    override fun logout() {
        scope.launch { client.safeRequest<Unit> { get("signOut") } }
        onLogout()
    }

    /**
     * Initialize push notifications
     */
    override fun initNotifications() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            scope.launch { client.safeRequest<Unit> { post("messaging/register") { setBody(SMessagingToken(it)) } } }
        }
    }

    init {
        initNotifications()
    }
}