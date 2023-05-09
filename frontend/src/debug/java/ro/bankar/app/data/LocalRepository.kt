package ro.bankar.app.data

import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
import ro.bankar.model.SNewBankAccount
import ro.bankar.model.SPublicUser
import ro.bankar.model.SRecentActivity
import ro.bankar.model.STransferRequest
import ro.bankar.model.SUser
import ro.bankar.model.SUserProfileUpdate
import ro.bankar.model.StatusResponse
import ro.bankar.model.TransferDirection
import ro.bankar.util.nowHere
import ro.bankar.util.nowUTC
import kotlin.time.Duration.Companion.seconds

val LocalRepository = compositionLocalOf<Repository> { MockRepository }

// During debugging, we might run screen previews when session_token is not set; in that case, display mock data instead of forcing log-in
val EmptyRepository: Repository = MockRepository

@Suppress("SpellCheckingInspection")
@OptIn(DelicateCoroutinesApi::class)
private object MockRepository : Repository(GlobalScope, "", {}) {
    private fun <T> mockFlow(value: T) = object : RequestFlow<T>(scope) {
        override suspend fun onEmissionRequest(mustRetry: Boolean, sendError: Boolean) {
            delay(2.seconds)
            flow.emit(value)
        }
    }
    private fun <Result, Fail> mockResponse() = SafeStatusResponse.InternalError<Result, Fail>(R.string.connection_error)

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

    override suspend fun sendAboutOrPicture(data: SUserProfileUpdate) = mockResponse<StatusResponse, InvalidParamResponse>()
    override val friends = mockFlow(
        listOf(
            SPublicUser(
                "bombasticus", "Bomba", "Maximus", "Extremus", "RO",
                LocalDate(1969, 6, 9), "straight up dead ngl", null
            ),
            SPublicUser(
                "koleci.alexandru", "Andi", null, "Koleci", "AL",
                LocalDate(2001, 2, 15), "", null
            ),
            SPublicUser(
                "chad.gpt", "Chad", "Thundercock", "GPT", "DE",
                Clock.System.todayIn(TimeZone.UTC), "not your business", null
            )
        )
    )

    override suspend fun sendAddFriend(id: String) = mockResponse<StatusResponse, StatusResponse>()
    override val friendRequests = mockFlow(
        listOf(
            SPublicUser(
                "not.a.scammer", "Your", "Computer", "Virus", "NL",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 5), "hello your computer has wirus", null,
            )
        )
    )
    private val mockRecentActivity = SRecentActivity(
        listOf(
            SBankTransfer(TransferDirection.Received, "Koleci 1", "testIBAN",
                25.215, Currency.EURO, "ia bani", Clock.System.nowHere()),
            SBankTransfer(TransferDirection.Sent, "Koleci 2", "testIBAN!!",
                15.0, Currency.US_DOLLAR, "nu, ia tu bani :3", Clock.System.nowHere()),
        ),
        listOf(SCardTransaction(1L, 2, "1373",
            23.2354, Currency.ROMANIAN_LEU, Clock.System.nowUTC(), "Sushi Terra", "nimic bun")),
        listOf(
            STransferRequest(
                TransferDirection.Received, "Big", null, "Boy",
                50.25, Currency.ROMANIAN_LEU, "Tesla Dealer", 5, Clock.System.nowUTC()
            )
        ),
    )
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

    override suspend fun sendCreateAccount(account: SNewBankAccount) = mockResponse<StatusResponse, InvalidParamResponse>()
}