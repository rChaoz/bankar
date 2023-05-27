package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.banking.Currency
import ro.bankar.banking.SCountries
import ro.bankar.banking.SExchangeData
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.NotFoundResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SConversation
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
import ro.bankar.model.STransferRequest
import ro.bankar.model.SUser
import ro.bankar.model.SUserMessage
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import ro.bankar.util.nowUTC
import ro.bankar.util.todayHere
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val LocalRepository = compositionLocalOf<Repository> { MockRepository }

// During debugging, we might run screen previews when session_token is not set; in that case, display mock data instead of forcing log-in
val EmptyRepository: Repository = MockRepository

@Suppress("SpellCheckingInspection")
private object MockRepository : Repository() {
    @OptIn(DelicateCoroutinesApi::class)
    private fun <T> mockFlow(value: T) = object : RequestFlow<T>(GlobalScope) {
        override suspend fun onEmissionRequest(continuation: Continuation<Unit>?) {
            delay(3.seconds)
            flow.emit(EmissionResult.Success(value))
            continuation?.resume(Unit)
        }
    }

    private fun <Result, Fail> mockStatusResponse() = SafeStatusResponse.InternalError<Result, Fail>(R.string.connection_error)
    private fun <Result> mockResponse() = SafeResponse.InternalError<Result>(R.string.connection_error)

    // TODO If socket is ever used - change this to some mock version so it doesn't crash
    override lateinit var socket: DefaultClientWebSocketSession
    override val socketFlow = MutableSharedFlow<SSocketNotification>().asSharedFlow()
    override suspend fun openAndMaintainSocket() {
        // do nothing
    }


    override val countryData = mockFlow<SCountries>(emptyList())
    override val exchangeData: RequestFlow<SExchangeData> = mockFlow(
        mapOf(
            Currency.EURO to listOf(Currency.ROMANIAN_LEU to 4.91, Currency.US_DOLLAR to 1.05),
            Currency.ROMANIAN_LEU to listOf(Currency.US_DOLLAR to 0.21, Currency.EURO to 0.20),
            Currency.US_DOLLAR to listOf(Currency.ROMANIAN_LEU to 4.55, Currency.EURO to 0.91),
        )
    )

    override suspend fun sendCheckPassword(password: String) = mockStatusResponse<StatusResponse, StatusResponse>()

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

    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = mockStatusResponse<StatusResponse, InvalidParamResponse>()
    override suspend fun sendUpdate(data: SNewUser) = mockResponse<StatusResponse>()

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
            lastMessage?.let { SUserMessage(SDirection.Received, it, Clock.System.nowUTC()) }, unreadCount
        )

    override val friends = mockFlow(
        listOf(
            bombasticus.friend(null, 0),
            koleci.friend("sall", 1),
            chadGPT.friend("please respond", 99)
        )
    )

    override suspend fun sendAddFriend(id: String) = mockStatusResponse<StatusResponse, StatusResponse>()
    override suspend fun sendRemoveFriend(tag: String) = mockStatusResponse<StatusResponse, StatusResponse>()
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

    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = mockStatusResponse<StatusResponse, StatusResponse>()
    override fun conversation(tag: String): RequestFlow<SConversation> {
        val today = Clock.System.todayHere()
        val yesterday = today - DatePeriod(days = 1)
        return mockFlow(
            listOf(
                SUserMessage(SDirection.Received, "test", yesterday.atTime(0, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(0, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(1, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0)),
                SUserMessage(SDirection.Received, "test", yesterday.atTime(5, 0)),

                SUserMessage(SDirection.Received, "hello!", yesterday.atTime(17, 25)),
                SUserMessage(SDirection.Sent, "no.", yesterday.atTime(18, 50)),


                SUserMessage(SDirection.Received, "Hello again!", today.atTime(12, 41)),
                SUserMessage(
                    SDirection.Sent, "I see you learned how to capitalize the 'H' in 'Hello'. That's acceptable." +
                            "How are you? Also this a pretty long message, for no apparent reason.", today.atTime(12, 44)
                ),
                SUserMessage(SDirection.Sent, "Also I forgot to say I hate you", today.atTime(12, 44, 30)),
                SUserMessage(SDirection.Received, ":(", today.atTime(12, 45)),
                SUserMessage(SDirection.Received, "u mean", today.atTime(12, 45, 10)),
                SUserMessage(SDirection.Sent, "unlucky.", today.atTime(15, 0))
            ).reversed()
        )
    }

    override suspend fun sendFriendMessage(recipientTag: String, message: String) = mockStatusResponse<StatusResponse, StatusResponse>()

    override suspend fun sendCreateParty(account: Int, note: String, amounts: List<Pair<String, Double>>) = mockResponse<StatusResponse>()
    override fun partyData(id: Int) = mockFlow(
        SPartyInformation(
            bombasticus, 180.05, Currency.ROMANIAN_LEU, "A lot of Taco Bell", listOf(
                SPartyMember(koleci, 105.23, SPartyMember.Status.Pending),
                SPartyMember(bombasticus, 51.01, SPartyMember.Status.Declined),
                SPartyMember(chadGPT, 999.99, SPartyMember.Status.Accepted)
            )
        )
    )

    override suspend fun sendCancelParty(id: Int) = mockStatusResponse<StatusResponse, NotFoundResponse>()

    private val mockRecentActivity = (Clock.System.now() - 5.minutes).toLocalDateTime(TimeZone.UTC).let { earlier ->
        SRecentActivity(
            listOf(
                SBankTransfer(
                    null, 1, 2, koleci, "Kolecii", "testIBAN.",
                    100.0, 20.01, Currency.ROMANIAN_LEU, Currency.EURO, "", Clock.System.nowUTC()
                ),
                SBankTransfer(
                    SDirection.Received, 1, null, koleci, "Koleci 1", "test_iban",
                    25.215, null, Currency.EURO, Currency.EURO, "ia bani", Clock.System.nowUTC()
                ),
                SBankTransfer(
                    null, 1, 2, koleci, "Koleciii", "testIBAN123!!",
                    5.5, null, Currency.US_DOLLAR, Currency.US_DOLLAR, "", earlier
                ),
                SBankTransfer(
                    SDirection.Sent, 1, null, koleci, "Koleci 2", "testIBAN!!",
                    15.0, 69.75, Currency.US_DOLLAR, Currency.ROMANIAN_LEU, "nu, ia tu bani :3", earlier
                ),
            ),
            listOf(
                SCardTransaction(
                    1L, 2, 1, "1373",
                    23.2354, Currency.ROMANIAN_LEU, Clock.System.nowUTC(), "Sushi Terra", "nimic bun"
                ),
                SCardTransaction(
                    2L, 3, 1, "6969",
                    0.01, Currency.EURO, earlier, "200 houses", "yea.."
                )
            ),
            listOf(SPartyPreview(1, 57.3, 22.8, Currency.ROMANIAN_LEU, "to buy 23784923 grains of sand (real)")),
            listOf(
                STransferRequest(
                    0, SDirection.Received, bombasticus,
                    50.25, Currency.ROMANIAN_LEU, "Tesla Dealer for like 25 model S and idk what else", 5, Clock.System.nowUTC()
                ),
                STransferRequest(
                    1, SDirection.Received, chadGPT,
                    12345.0, Currency.EURO, "Like a lot of cash", null, Clock.System.nowUTC()
                )
            ),
        )
    }

    override suspend fun sendCancelFriendRequest(tag: String) = mockStatusResponse<StatusResponse, StatusResponse>()


    override val recentActivity = mockFlow(mockRecentActivity)
    override val allRecentActivity = mockFlow(mockRecentActivity)
    override fun recentActivityWith(tag: String) = mockFlow(mockRecentActivity.transfers)

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

    override fun account(id: Int) = mockFlow(
        SBankAccountData(emptyList(), emptyList(), emptyList()) // TODO
    )

    override suspend fun sendCreateAccount(account: SNewBankAccount) = mockStatusResponse<StatusResponse, InvalidParamResponse>()
    override suspend fun sendCustomiseAccount(id: Int, name: String, color: Int) = mockResponse<StatusResponse>()
    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<StatusResponse>()
    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<StatusResponse>()
    override suspend fun sendCancelTransferRequest(id: Int) = mockStatusResponse<StatusResponse, StatusResponse>()
    override suspend fun sendRespondToTransferRequest(id: Int, accept: Boolean, sourceAccountID: Int?) = mockStatusResponse<StatusResponse, StatusResponse>()

    override fun logout() {}

    init {
        init()
    }
}