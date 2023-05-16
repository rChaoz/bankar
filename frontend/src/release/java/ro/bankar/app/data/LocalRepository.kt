package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SUserProfileUpdate

val LocalRepository = compositionLocalOf<Repository> { throw RuntimeException("LocalRepository provider not found") }

@OptIn(DelicateCoroutinesApi::class)
object EmptyRepository : Repository() {
    override val countryData get() = throw RuntimeException("EmptyRepository cannot be accessed")
    // User profile & friends
    override val profile get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendAddFriend(id: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendRemoveFriend(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friends get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friendRequests get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCancelFriendRequest(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")

    // Recent activity
    override val recentActivity get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val allRecentActivity get() = throw RuntimeException("EmptyRepository cannot be accessed")

    // Bank accounts
    override val accounts get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun account(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = throw RuntimeException("EmptyRepository cannot be accessed")
}