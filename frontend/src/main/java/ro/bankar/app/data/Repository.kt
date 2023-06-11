package ro.bankar.app.data

import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import ro.bankar.app.TAG
import ro.bankar.banking.SCountries
import ro.bankar.banking.SExchangeData
import ro.bankar.model.Response
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SConversation
import ro.bankar.model.SCreateParty
import ro.bankar.model.SCustomiseBankAccount
import ro.bankar.model.SDefaultBankAccount
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SMessagingToken
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewUser
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
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * A Shared flow with the option to request emitting
 */
abstract class RequestFlow<T> protected constructor(
    private val scope: CoroutineScope,
    private val flow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
) : SharedFlow<T> by flow.asSharedFlow() {

    fun requestEmit() {
        scope.launch { emitNow() }
    }

    suspend fun emitNow(): T {
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

// LocalRepository defined in debug/release source sets
fun repository(scope: CoroutineScope, sessionToken: String, onLogout: () -> Unit): Repository =
    RepositoryImpl(scope, sessionToken, onLogout)

abstract class Repository {
    // WebSocket for transmitting live data
    abstract val socket: DefaultClientWebSocketSession
    abstract val socketFlow: Flow<SSocketNotification>
    abstract suspend fun openAndMaintainSocket()

    // Static data & password check
    abstract val countryData: RequestFlow<SCountries>
    abstract val exchangeData: RequestFlow<SExchangeData>
    abstract suspend fun sendCheckPassword(password: String): RequestResult<Unit>

    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): RequestResult<Unit>
    abstract suspend fun sendUpdate(data: SNewUser): RequestResult<Unit>
    abstract suspend fun sendAddFriend(id: String): RequestResult<Unit>
    abstract suspend fun sendRemoveFriend(tag: String): RequestResult<Unit>
    abstract val friends: RequestFlow<List<SFriend>>
    abstract val friendRequests: RequestFlow<List<SFriendRequest>>
    abstract suspend fun sendFriendRequestResponse(tag: String, accept: Boolean): RequestResult<Unit>
    abstract suspend fun sendCancelFriendRequest(tag: String): RequestResult<Unit>
    abstract fun conversation(tag: String): RequestFlow<SConversation>
    abstract suspend fun sendFriendMessage(recipientTag: String, message: String): RequestResult<Unit>

    // Parties
    abstract suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>): RequestResult<Unit>
    abstract fun partyData(id: Int): RequestFlow<SPartyInformation>
    abstract suspend fun sendCancelParty(id: Int): RequestResult<Unit>

    // Recent activity
    abstract val recentActivity: RequestFlow<SRecentActivity>
    abstract val allRecentActivity: RequestFlow<SRecentActivity>
    abstract fun recentActivityWith(tag: String): RequestFlow<List<SBankTransfer>>

    // Bank accounts
    abstract val defaultAccount: RequestFlow<SDefaultBankAccount>
    abstract suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean): RequestResult<Unit>
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
    abstract suspend fun sendCreateAccount(account: SNewBankAccount): RequestResult<Unit>
    abstract suspend fun sendCustomiseAccount(id: Int, name: String, color: Int): RequestResult<Unit>
    abstract suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): RequestResult<String>
    abstract suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String): RequestResult<String>
    abstract suspend fun sendCancelTransferRequest(id: Int): RequestResult<Unit>
    abstract suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?): RequestResult<Unit>
    abstract val statements: RequestFlow<List<SStatement>>
    abstract suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate): RequestResult<SStatement>
    abstract fun createDownloadStatementRequest(statement: SStatement): DownloadManager.Request

    abstract fun logout()
    abstract fun initNotifications()

    // Load data on Repository creation to avoid having to wait when going to each screen
    protected fun init() {
        countryData.requestEmit()
        exchangeData.requestEmit()
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

private class RepositoryImpl(private val scope: CoroutineScope, private val sessionToken: String, private val onLogout: () -> Unit) : Repository() {

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
    override val countryData = createRawFlow<SCountries>("data/countries.json")
    override val exchangeData = createRawFlow<SExchangeData>("data/exchange.json")
    override suspend fun sendCheckPassword(password: String) = client.safeRequest<Unit> {
        post("verifyPassword") { setBody(SPasswordData(password)) }
    }

    // User profile & friends
    override val profile = createFlow<SUser>("profile")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = client.safeRequest<Unit> {
        put("profile/update") {
            setBody(data)
        }
    }

    override suspend fun sendUpdate(data: SNewUser) = client.safeRequest<Unit> {
        put("updateAccount") { setBody(data) }
    }

    override suspend fun sendAddFriend(id: String) = client.safeRequest<Unit> {
        get("profile/friends/add/$id")
    }

    override suspend fun sendRemoveFriend(tag: String) = client.safeRequest<Unit> {
        get("profile/friends/remove/$tag")
    }

    override val friends = createFlow<List<SFriend>>("profile/friends")
    override val friendRequests = createFlow<List<SFriendRequest>>("profile/friend_requests")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = client.safeRequest<Unit> {
        get("profile/friend_requests/${if (accept) "accept" else "decline"}/$tag")
    }

    override suspend fun sendCancelFriendRequest(tag: String) = client.safeRequest<Unit> {
        get("profile/friend_requests/cancel/$tag")
    }

    override fun conversation(tag: String) = createFlow<SConversation>("messaging/conversation/$tag")
    override suspend fun sendFriendMessage(recipientTag: String, message: String) = client.safeRequest<Unit> {
        post("messaging/send") { setBody(SSendMessage(message, recipientTag)) }
    }

    // Parties
    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) = client.safeRequest<Unit> {
        post("party/create") {
            setBody(SCreateParty(account, note, amounts))
        }
    }

    override fun partyData(id: Int) = createFlow<SPartyInformation>("party/$id")
    override suspend fun sendCancelParty(id: Int) = client.safeRequest<Unit> { get("party/cancel/$id") }


    // Recent activity
    override val recentActivity = createFlow<SRecentActivity>("recent")
    override val allRecentActivity = createFlow<SRecentActivity>("recent?count=100000")
    override fun recentActivityWith(tag: String) = createFlow<List<SBankTransfer>>("transfer/list/$tag")

    // Bank accounts
    override val defaultAccount = createFlow<SDefaultBankAccount>("defaultAccount")
    override suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean) =
        client.safeRequest<Unit> { post("defaultAccount") { setBody(SDefaultBankAccount(id, alwaysUse)) } }

    override val accounts = createFlow<List<SBankAccount>>("accounts")
    override fun account(id: Int) = createFlow<SBankAccountData>("accounts/$id")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = client.safeRequest<Unit> {
        post("accounts/new") { setBody(account) }
    }

    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = client.safeRequest<Unit> {
        post("accounts/$id/customise") { setBody(SCustomiseBankAccount(name, color)) }
    }

    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/send") { setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note)) }
    }

    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = client.safeRequest<String> {
        post("transfer/request") { setBody(SSendRequestMoney(recipientTag, sourceAccount.id, amount, sourceAccount.currency, note)) }
    }

    override suspend fun sendCancelTransferRequest(id: Int) = client.safeRequest<Unit> {
        get("transfer/cancel/$id")
    }

    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = client.safeRequest<Unit> {
        get("transfer/respond/$id") {
            parameter("action", if (accept) "accept" else "decline")
            parameter("accountID", sourceAccountID)
        }
    }

    override val statements = createFlow<List<SStatement>>("statements")
    override suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate) =
        client.safeRequest<SStatement> {
            post("statements/request") { setBody(SStatementRequest(name, accountID, from, to)) }
        }

    override fun createDownloadStatementRequest(statement: SStatement) = DownloadManager.Request(statement.downloadURI).apply {
        val name = "Statement-${statement.dateTime.dashFormat()}.pdf"
        setDestinationUri(Uri.fromFile(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name)))
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        addRequestHeader(HttpHeaders.Authorization, "Bearer $sessionToken")
    }

    // Utility functions
    private inline fun <reified T> createRawFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun emit(): T? = try {
            client.get(url).body<T>()
        } catch (e: Exception) {
            Log.w(TAG, "Exception in raw flow", e)
            null
        }
    }

    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun emit(): T? {
            try {
                val result = client.get(url)
                if (result.status == HttpStatusCode.Unauthorized) onLogout()
                val response = result.body<Response<T>>()
                if (response is ValueResponse) return response.value
                else Log.w(TAG, "Invalid server response for flow \"$url\": $response")
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.w(TAG, "Exception in repository flow", e)
            }
            return null
        }
    }

    override fun logout() {
        scope.launch { client.safeRequest<Unit> { get("signout") } }
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