package ro.bankar.routing

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import ro.bankar.*
import ro.bankar.banking.Currency
import ro.bankar.database.User
import ro.bankar.model.SBankAccount
import ro.bankar.model.SBankAccountType
import ro.bankar.model.SNewBankAccount
import kotlin.test.Test
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BankAccountsTest {
    companion object {
        lateinit var token: String

        @JvmStatic
        @BeforeAll
        fun init() {
            resetDatabase()
            token = "Bearer " + transaction { User.createUser(testUser).createSessionToken() }
        }
    }

    @Test
    @Order(1)
    fun testCreateAccount() = testApplication {
        val client = baseClient(token)
        var accounts = client.getValue<List<SBankAccount>>("accounts")
        assertEquals(0, accounts.size)
        client.post("accounts/new") {
            setBody(SNewBankAccount(SBankAccountType.Debit, "Account", 1, Currency.ROMANIAN_LEU, 0.0))
        }.assertOK(HttpStatusCode.Created)
        client.post("accounts/new") {
            setBody(SNewBankAccount(SBankAccountType.Credit, "Account", 1, Currency.ROMANIAN_LEU, 5000.0))
        }.assertOK(HttpStatusCode.Created)
        accounts = client.getValue<List<SBankAccount>>("accounts")
        assertEquals(2, accounts.size)
    }

    @Test
    @Order(2)
    fun testCloseAccount() = testApplication {
        val client = baseClient(token)
        var accounts = client.getValue<List<SBankAccount>>("accounts")
        assertEquals(2, accounts.size)
        for (acc in accounts) client.delete("accounts/${acc.id}")
        accounts = client.getValue<List<SBankAccount>>("accounts")
        assertEquals(0, accounts.size)
    }
}