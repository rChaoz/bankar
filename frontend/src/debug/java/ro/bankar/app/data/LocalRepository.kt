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
import kotlinx.datetime.todayIn
import ro.bankar.app.R
import ro.bankar.banking.Currency
import ro.bankar.model.InvalidParamResponse
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SBankTransfer
import ro.bankar.model.SCardTransaction
import ro.bankar.model.SConversation
import ro.bankar.model.SCountries
import ro.bankar.model.SDirection
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.SSocketNotification
import ro.bankar.model.STransferRequest
import ro.bankar.model.SUser
import ro.bankar.model.SUserMessage
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import ro.bankar.util.nowHere
import ro.bankar.util.nowUTC
import ro.bankar.util.todayHere
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
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
    override val friends = mockFlow(
        listOf(
            SPublicUser(
                "bombasticus", "Bomba", "Maximus", "Extremus", "RO",
                LocalDate(1969, 6, 9), "straight up dead ngl", SDirection.Sent, null
            ),
            SPublicUser(
                "koleci.alexandru", "Andi", null, "Koleci", "AL",
                LocalDate(2001, 2, 15), "", SDirection.Sent, null
            ),
            SPublicUser(
                "chad.gpt", "Chad", "Thundercock", "GPT", "DE",
                Clock.System.todayIn(TimeZone.UTC), "not your business", SDirection.Sent, null
            )
        )
    )

    override suspend fun sendAddFriend(id: String) = mockStatusResponse<StatusResponse, StatusResponse>()
    override suspend fun sendRemoveFriend(tag: String) = mockStatusResponse<StatusResponse, StatusResponse>()
    override val friendRequests = mockFlow(
        listOf(
            SPublicUser(
                "not.a.scammer", "Your", "Computer", "Virus", "NL",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 5), "hello your computer has wirus", SDirection.Received, null
            ),
            SPublicUser(
                "mary.poppins", "Marry", null, "Poppins", "RO",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 25), "Poppin'", SDirection.Sent, null
            )
        )
    )
    override suspend fun sendFriendRequestResponse(tag: String, accept: Boolean) = mockStatusResponse<StatusResponse, StatusResponse>()
    override fun conversation(tag: String): RequestFlow<SConversation> {
        val today = Clock.System.todayHere()
        val yesterday = today - DatePeriod(days = 1)
        return mockFlow(listOf(
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
            SUserMessage(SDirection.Sent, "I see you learned how to capitalize the 'H' in 'Hello'. That's acceptable." +
                    "How are you? Also this a pretty long message, for no apparent reason.", today.atTime(12, 44)),
            SUserMessage(SDirection.Sent, "Also I forgot to say I hate you", today.atTime(12, 44, 30)),
            SUserMessage(SDirection.Received, ":(", today.atTime(12, 45)),
            SUserMessage(SDirection.Received, "u mean", today.atTime(12, 45, 10)),
            SUserMessage(SDirection.Sent, "unlucky.", today.atTime(15, 0))
        ).reversed())
    }
    override suspend fun sendFriendMessage(recipientTag: String, message: String) = mockStatusResponse<StatusResponse, StatusResponse>()

    private val mockRecentActivity = SRecentActivity(
        listOf(
            SBankTransfer(
                SDirection.Received, "Koleci 1", "testIBAN",
                25.215, Currency.EURO, "ia bani", Clock.System.nowHere()
            ),
            SBankTransfer(
                SDirection.Sent, "Koleci 2", "testIBAN!!",
                15.0, Currency.US_DOLLAR, "nu, ia tu bani :3", Clock.System.nowHere()
            ),
        ),
        listOf(
            SCardTransaction(
                1L, 2, "1373",
                23.2354, Currency.ROMANIAN_LEU, Clock.System.nowUTC(), "Sushi Terra", "nimic bun"
            )
        ),
        listOf(
            STransferRequest(
                0, SDirection.Received, "Big", "Boy", "Long-Nameus Whatsapp",
                50.25, Currency.ROMANIAN_LEU, "Tesla Dealer for like 25 model S and idk what else", 5, Clock.System.nowUTC()
            ),
            STransferRequest(
                1, SDirection.Received, "Gimme", null, "Cash",
                12345.0, Currency.EURO, "Like a lot of cash", null, Clock.System.nowUTC()
            )
        ),
    )

    override suspend fun sendCancelFriendRequest(tag: String) = mockStatusResponse<StatusResponse, StatusResponse>()


    override val recentActivity = mockFlow(mockRecentActivity)
    override val allRecentActivity = mockFlow(mockRecentActivity)
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
    override suspend fun sendTransfer(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<StatusResponse>()
    override suspend fun sendTransferRequest(recipientTag: String, sourceAccount: SBankAccount, amount: Double, note: String) = mockResponse<StatusResponse>()
    override suspend fun sendCancelTransferRequest(id: Int) = mockStatusResponse<StatusResponse, StatusResponse>()

    init {
        init()
    }
}