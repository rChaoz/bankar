package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import ro.bankar.model.SBankAccount
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewUser
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SStatement
import ro.bankar.model.SUserProfileUpdate

val LocalRepository = compositionLocalOf<Repository> { throw RuntimeException("LocalRepository provider not found") }

object EmptyRepository : Repository() {
    // Web socket
    override val socket: DefaultClientWebSocketSession get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val socketFlow: Flow<SSocketNotification> get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun openAndMaintainSocket() = throw RuntimeException("EmptyRepository cannot be accessed")
    // Static data
    override val countryData get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val exchangeData get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val creditData get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCheckPassword(password: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    // User profile & friends
    override val profile get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendUpdate(data: SNewUser)= throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendAddFriend(id: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendRemoveFriend(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friends get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val friendRequests get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCancelFriendRequest(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun conversation(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendFriendMessage(recipientTag: String, message: String) = throw RuntimeException("EmptyRepository cannot be accessed")

    // Parties
    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override fun partyData(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCancelParty(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")

    // Recent activity
    override val recentActivity get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override val allRecentActivity get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun recentActivityWith(tag: String) = throw RuntimeException("EmptyRepository cannot be accessed")

    // Bank accounts
    override val defaultAccount get() = throw RuntimeException("EmptyRepository cannot be accessed")

    override suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean) = throw RuntimeException("EmptyRepository cannot be accessed")
    override val accounts get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun account(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCreateAccount(account: SNewBankAccount) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCloseAccount(account: SBankAccount) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendOwnTransfer(sourceAccount: SBankAccount, targetAccount: SBankAccount, amount: Double, note: String) =
        throw RuntimeException("EmptyRepository cannot be accessed")

    override suspend fun sendExternalTransfer(sourceAccount: SBankAccount, targetIBAN: String, amount: Double, note: String) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendCancelTransferRequest(id: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override val statements get() = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate) =
        throw RuntimeException("EmptyRepository cannot be accessed")
    override fun createDownloadStatementRequest(statement: SStatement) = throw RuntimeException("EmptyRepository cannot be accessed")

    // Cards
    override suspend fun sendCreateCard(accountID: Int, name: String) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendUpdateCard(accountID: Int, cardID: Int, name: String, limit: Double) = throw RuntimeException("EmptyRepository cannot be accessed")
    override suspend fun sendResetCardLimit(accountID: Int, cardID: Int) = throw RuntimeException("EmptyRepository cannot be accessed")
    override fun card(accountID: Int, cardID: Int) = throw RuntimeException("EmptyRepository cannot be accessed")

    override fun logout() {}
    override fun initNotifications() {}
}