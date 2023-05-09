package ro.bankar.app.data

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ro.bankar.app.R
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse

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
    fun requestEmit(mustRetry: Boolean, sendError: Boolean = true) {
        scope.launch { onEmissionRequest(mustRetry, sendError) }
    }

    protected abstract suspend fun onEmissionRequest(mustRetry: Boolean, sendError: Boolean)
}

// LocalRepository defined in debug/release source sets

fun repository(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository = RepositoryImpl(scope, sessionToken, onSessionExpire)

abstract class Repository(protected val scope: CoroutineScope, protected val sessionToken: String, protected val onSessionExpire: () -> Unit) {
    data class Error(val message: Int, val mustRetry: Boolean, val retry: (mustRetry: Boolean) -> Unit)
    protected val mutableErrorFlow = MutableSharedFlow<Error>(replay = 0)
    val errorFlow = mutableErrorFlow.asSharedFlow()

    // User profile & friends
    abstract val profile: RequestFlow<SUser>
    abstract suspend fun sendAboutOrPicture(data: SUserProfileUpdate): SafeStatusResponse<StatusResponse, InvalidParamResponse>
    abstract suspend fun sendAddFriend(id: String): SafeStatusResponse<StatusResponse, StatusResponse>
    abstract val friends: RequestFlow<List<SPublicUser>>
    abstract val friendRequests: RequestFlow<List<SPublicUser>>
    abstract suspend fun sendFriendRequestResponse(tag: String, accept: Boolean): SafeStatusResponse<StatusResponse, StatusResponse>

    // Recent activity
    abstract val recentActivity: RequestFlow<SRecentActivity>
    abstract val allRecentActivity: RequestFlow<SRecentActivity>

    // Bank accounts
    abstract val accounts: RequestFlow<List<SBankAccount>>
    abstract fun account(id: Int): RequestFlow<SBankAccountData>
    abstract suspend fun sendCreateAccount(account: SNewBankAccount): SafeStatusResponse<StatusResponse, InvalidParamResponse>
}

private class RepositoryImpl(scope: CoroutineScope, sessionToken: String, onSessionExpire: () -> Unit): Repository(scope, sessionToken, onSessionExpire) {
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
        profile.requestEmit(false, sendError = false)
        friends.requestEmit(false, sendError = false)
        friendRequests.requestEmit(false, sendError = false)
        accounts.requestEmit(false, sendError = false)
    }

    // Utility functions
    private inline fun <reified T> createFlow(url: String) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(mustRetry: Boolean, sendError: Boolean) {
            val r = ktorClient.safeRequest<T> {
                get(url) {
                    bearerAuth(sessionToken)
                }
            }
            when (r) {
                is SafeResponse.InternalError -> if (sendError) mutableErrorFlow.emit(Error(r.message, mustRetry, this::requestEmit))
                is SafeResponse.Fail -> {
                    if (r.r.status == HttpStatusCode.Unauthorized || r.r.status == HttpStatusCode.Forbidden) onSessionExpire()
                    else if (sendError) mutableErrorFlow.emit(Error(R.string.unknown_error, mustRetry, this::requestEmit))
                }
                is SafeResponse.Success -> flow.emit(r.result)
            }
        }
    }
}