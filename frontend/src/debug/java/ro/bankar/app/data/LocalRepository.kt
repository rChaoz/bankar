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
import ro.bankar.banking.Currency
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountData
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SPublicUser
import ro.bankar.model.SUser
import kotlin.time.Duration.Companion.seconds

val LocalRepository = compositionLocalOf<Repository> { MockRepository }

// During debugging, we might run screen previews when session_token is not set; in that case, display mock data instead of forcing log-in
val EmptyRepository: Repository = MockRepository

@Suppress("SpellCheckingInspection")
@OptIn(DelicateCoroutinesApi::class)
private object MockRepository : Repository(GlobalScope, "", {}) {
    fun <T> mockFlow(value: T) = object : RequestFlow<T>(scope) {
        override suspend fun onEmitRequest() {
            delay(2.seconds)
            flow.emit(value)
        }
    }

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
    override val friendRequests = mockFlow(
        listOf(
            SPublicUser(
                "not.a.scammer", "Your", "Computer", "Virus", "NL",
                Clock.System.todayIn(TimeZone.UTC) - DatePeriod(days = 5), "hello your computer has wirus", null,
            )
        )
    )
    override val accounts = mockFlow(
        listOf(
            SBankAccount(
                1, "RO24RBNK1921081333473500", SBankAccountType.DEBIT, 123.456, 0.0, Currency.ROMANIAN_LEU,
                "Debit Account", 0, 0.0
            ),
            SBankAccount(
                2, "RO56RBNK2342345546435657", SBankAccountType.CREDIT, -500.32, 0.0, Currency.EURO,
                "Credit Account", 0, 20.0
            ),
        )
    )

    override fun account(id: Int) = mockFlow(
        SBankAccountData(emptyList(), emptyList(), emptyList()) // TODO
    )
}