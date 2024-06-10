package ro.bankar.app.data

import android.app.DownloadManager
import android.net.Uri
import androidx.compose.runtime.compositionLocalOf
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.banking.SCreditData
import ro.bankar.model.Response
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SBankCard
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SConversation
import ro.bankar.model.SDefaultBankAccount
import ro.bankar.model.SDirection
import ro.bankar.model.SFriend
import ro.bankar.model.SFriendRequest
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SNewUser
import ro.bankar.model.SPartyInformation
import ro.bankar.model.SPartyMember
import ro.bankar.model.SPartyPreview
import ro.bankar.model.SPublicUser
import ro.bankar.model.SPublicUserBase
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SSocketNotification
import ro.bankar.model.SStatement
import ro.bankar.model.STransferRequest
import ro.bankar.model.SUser
import ro.bankar.model.SUserMessage
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.util.todayHere
import kotlin.time.Duration.Companion.minutes

val LocalRepository = compositionLocalOf<Repository> { MockRepository }

// During debugging, we might run screen previews when session_token is not set; in that case, display mock data instead of forcing log-in
val EmptyRepository: Repository = MockRepository

@Suppress("SpellCheckingInspection")
private object MockRepository : Repository() {
    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> mockFlow(value: T) = object : AbstractRequestFlow<T>(GlobalScope) {
        override suspend fun emit() = value
    }.also { it.requestEmit() }

    private fun <T> mockResponse(): RequestResult<Response<T>> = RequestFail(R.string.connection_error)

    // TODO If socket is ever used - change this to some mock version so it doesn't crash
    override lateinit var socket: DefaultClientWebSocketSession
    override val socketFlow = MutableSharedFlow<SSocketNotification>().asSharedFlow()
    override suspend fun openAndMaintainSocket() {
        // do nothing
    }


    override val countryData = mockFlow<SCountries>(emptyList())
    override val exchangeData = mockFlow(
        mapOf(
            Currency.EURO to listOf(Currency.ROMANIAN_LEU to 4.91, Currency.US_DOLLAR to 1.05),
            Currency.ROMANIAN_LEU to listOf(Currency.US_DOLLAR to 0.21, Currency.EURO to 0.20),
            Currency.US_DOLLAR to listOf(Currency.ROMANIAN_LEU to 4.55, Currency.EURO to 0.91),
        )
    )
    override val creditData = mockFlow(
        listOf(
            SCreditData(Currency.ROMANIAN_LEU, 15.5, 1000.0, 10000.0)
        )
    )

    override suspend fun sendCheckPassword(password: String) = mockResponse<Unit>()

    override val profile = mockFlow(
        SUser(
            email = "test_email@example.site",
            tag = "MrExample02",
            phone = "+40123456789",
            firstName = "Mister",
            middleName = null,
            lastName = "Examplus",
            dateOfBirth = LocalDate(2000, 5, 5),
            countryCode = "RO",
            state = "Bucurest",
            city = "Bucuresti",
            address = "Incredibly Clean address, version 2",
            joinDate = LocalDate(2023, 1, 1),
            about = "Just vibing. Nothing more. innit",
            avatar = null
        )
    )

    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = mockResponse<Unit>()
    override suspend fun sendUpdate(data: SNewUser) = mockResponse<Unit>()

    private val bombasticus = SPublicUser(
        "bombasticus", "Bomba", "Maximus", "Extremus", "RO",
        LocalDate(1969, 6, 9), "straight up dead ngl", null, true
    )
    private val koleci = SPublicUser(
        "koleci.alexandru", "Andi", null, "Koleci", "AL",
        LocalDate(2001, 2, 15), "", null, true
    )
    private val chadGPT = SPublicUser(
        "chad.gpt", "Chad", "Thundercock", "GPT", "DE",
        Clock.System.todayIn(TimeZone.UTC), "not your business", null, true
    )

    private fun SPublicUserBase.friend(lastMessage: String?, unreadCount: Int) =
        SFriend(
            tag, firstName, middleName, lastName, countryCode, joinDate, about, avatar,
            lastMessage?.let { SUserMessage(SDirection.Received, it, Clock.System.now()) }, unreadCount
        )

    override val friends = mockFlow(
        listOf(
            bombasticus.friend(null, 0),
            koleci.friend("sall", 1),
            chadGPT.friend("please respond", 99)
        )
    )

    override suspend fun sendAddFriend(id: String) = mockResponse<Unit>()
    override suspend fun sendRemoveFriend(tag: String) = mockResponse<Unit>()
    override val friendRequests = mockFlow(
        listOf(
            SFriendRequest(
                "not.a.scammer", "Your", "Computer", "Virus", "NL",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 5), "hello your computer has wirus", null, SDirection.Received
            ),
            SFriendRequest(
                "mary.poppins", "Marry", null, "Poppins", "RO",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 25), "Poppin'", null, SDirection.Sent
            )
        )
    )

    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = mockResponse<Unit>()
    override fun conversation(tag: String): AbstractRequestFlow<SConversation> {
        val today = Clock.System.todayHere()
        val yesterday = today - DatePeriod(days = 1)

        fun LocalDateTime.i() = toInstant(TimeZone.currentSystemDefault())

        return mockFlow(
            listOf(
                SUserMessage(SDirection.Received, "test", yesterday.atTime(0, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(0, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0).i()),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0).i()),

                SUserMessage(SDirection.Received, "hello!", yesterday.atTime(17, 25).i()),
                SUserMessage(SDirection.Sent, "no.", yesterday.atTime(18, 50).i()),


                SUserMessage(SDirection.Received, "Hello again!", today.atTime(12, 41).i()),
                SUserMessage(
                    SDirection.Sent, "I see you learned how to capitalize the 'H' in 'Hello'. That's acceptable." +
                            "How are you? Also this a pretty long message, for no apparent reason.", today.atTime(12, 44).i()
                ),
                SUserMessage(SDirection.Sent, "Also I forgot to say I hate you", today.atTime(12, 44, 30).i()),
                SUserMessage(SDirection.Received, ":(", today.atTime(12, 45).i()),
                SUserMessage(SDirection.Received, "u mean", today.atTime(12, 45, 10).i()),
                SUserMessage(SDirection.Sent, "unlucky.", today.atTime(15, 0).i())
            ).reversed()
        )
    }

    override suspend fun sendFriendMessage(recipientTag: String, message: String) = mockResponse<Unit>()

    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) = mockResponse<Unit>()
    override fun partyData(id: Int) = mockFlow(
        SPartyInformation(
            bombasticus, 180.05, Currency.ROMANIAN_LEU, "A lot of Taco Bell",
            SPartyMember(koleci, 12.12, SPartyMember.Status.Pending, null),
            listOf(
                SPartyMember(koleci, 105.23, SPartyMember.Status.Pending, null),
                SPartyMember(bombasticus, 51.01, SPartyMember.Status.Cancelled, null),
                SPartyMember(chadGPT, 999.99, SPartyMember.Status.Accepted, null)
            ), 1
        )
    )

    override suspend fun sendCancelParty(id: Int) = mockResponse<Unit>()

    private val mockRecentActivity = (Clock.System.now() to (Clock.System.now() - 15.minutes)).let { (now, earlier) ->
        SRecentActivity(
            listOf(
                SBankTransfer(
                    null, 1, 2, koleci, "Kolecii", "testIBAN.", null,
                    100.0, 20.01, Currency.ROMANIAN_LEU, Currency.EURO, "", now
                ),
                SBankTransfer(
                    SDirection.Sent, 1, null, koleci, "Mister Sparanghel", "testIBAN!!1", 1,
                    11.49, null, Currency.ROMANIAN_LEU, Currency.ROMANIAN_LEU, "O berica mica", earlier
                ),
                SBankTransfer(
                    SDirection.Received, 1, null, koleci, "Koleci 1", "test_iban", null,
                    25.215, null, Currency.EURO, Currency.EURO, "ia bani", now
                ),
                SBankTransfer(
                    null, 1, 2, koleci, "Koleciii", "testIBAN123!!", null,
                    5.5, null, Currency.US_DOLLAR, Currency.US_DOLLAR, "", earlier
                ),
            ),
            listOf(
                SCardTransaction(
                    1L, 2, 1,
                    23.2354, Currency.ROMANIAN_LEU, now, "Sushi Terra", "nimic bun"
                ),
                SCardTransaction(
                    2L, 3, 1,
                    0.01, Currency.EURO, earlier, "200 houses", "yea.."
                )
            ),
            listOf(
                SPartyPreview(1, false, now, 57.3, 22.8, Currency.ROMANIAN_LEU, "to buy 23784923 grains of sand (real)"),
                SPartyPreview(1, true, earlier, 57.3, 22.8, Currency.ROMANIAN_LEU, "also for 23784924 grains of sand")
            ),
            listOf(
                STransferRequest(
                    0, SDirection.Received, bombasticus,
                    50.25, Currency.ROMANIAN_LEU, "Tesla Dealer for like 25 model S and idk what else", 5, now
                ),
                STransferRequest(
                    1, SDirection.Received, chadGPT,
                    12345.0, Currency.EURO, "Like a lot of cash", null, now
                )
            ),
        )
    }

    override suspend fun sendCancelFriendRequest(tag: String) = mockResponse<Unit>()


    override val recentActivity = mockFlow(mockRecentActivity)
    override val allRecentActivity = mockFlow(
        mockRecentActivity.run { copy(transferRequests = emptyList(), parties = parties.filter(SPartyPreview::completed)) }
    )

    override fun recentActivityWith(tag: String) = mockFlow(mockRecentActivity.transfers)

    override val defaultAccount = mockFlow(SDefaultBankAccount(1, true))
    override suspend fun sendDefaultAccount(id: Int?, alwaysUse: Boolean) = mockResponse<Unit>()
    override val accounts = mockFlow(
        listOf(
            SBankAccount(
                1, "RO24RBNK1921081333473500", SBankAccountType.Debit, 123.456, 0.0, Currency.ROMANIAN_LEU,
                "Debit Account", 0, 0.0
            ),
            SBankAccount(
                2, "RO56RBNK2342345546435657", SBankAccountType.Credit, -500.32, 1000.0, Currency.EURO,
                "Credit Account", 0, 20.0
            ),
        )
    )

    private val card = SBankCard(
        1, "Physical Card", "1234567813571234", "1234", "9876", Month.JUNE, 2069,
        "420", 0.0, 0.0, Currency.ROMANIAN_LEU,
        listOf(
            SCardTransaction(
                127836L, 1, 1, 25.55, Currency.ROMANIAN_LEU,
                Clock.System.now(), "Taco Bell", "details"
            ),
            SCardTransaction(
                7439287432897L, 1, 1, 13.12, Currency.ROMANIAN_LEU,
                Clock.System.now(), "Nimic bun", "detailssssss"
            ),
        )
    )

    override fun account(id: Int) = mockFlow(
        SBankAccountData(
            listOf(
                card,
                SBankCard(
                    2, "Virtual Card", "1111222233334444", "4444", "1234", Month.JANUARY, 2025,
                    "123", 100.0, 34.45, Currency.EURO, emptyList()
                ),
            ),
            emptyList(),
            emptyList(),
            emptyList()
        )
    )

    override suspend fun sendCreateAccount(account: SNewBankAccount) = mockResponse<Unit>()
    override suspend fun sendCloseAccount(account: SBankAccount) = mockResponse<Unit>()
    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = mockResponse<Unit>()
    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<String>()
    override suspend fun sendOwnTransfer(sourceAccount: SBankAccount, targetAccount: SBankAccount, amount: Double, note: String) = mockResponse<Unit>()
    override suspend fun sendExternalTransfer(sourceAccount: SBankAccount, targetIBAN: String, amount: Double, note: String) = mockResponse<Unit>()
    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<String>()
    override suspend fun sendCancelTransferRequest(id: Int) = mockResponse<Unit>()
    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = mockResponse<Unit>()
    override val statements = mockFlow(
        listOf(
            SStatement(1, "My Statement", Clock.System.now(), 1)
        )
    )

    override suspend fun sendCreateCard(accountID: Int, name: String) = mockResponse<Int>()
    override suspend fun sendUpdateCard(accountID: Int, cardID: Int, name: String, limit: Double) = mockResponse<Unit>()
    override fun card(accountID: Int, cardID: Int)= mockFlow(card)

    override suspend fun sendStatementRequest(name: String?, accountID: Int, from: LocalDate, to: LocalDate) = mockResponse<SStatement>()
    override fun createDownloadStatementRequest(statement: SStatement) = DownloadManager.Request(Uri.parse("http://example.com"))

    override fun logout() {}
    override fun initNotifications() {}
}