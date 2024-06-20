package ro.bankar.routing

import io.ktor.client.request.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import ro.bankar.*
import ro.bankar.banking.Currency
import ro.bankar.database.BankAccount
import ro.bankar.database.User
import ro.bankar.model.*
import kotlin.test.Test
import kotlin.test.assertEquals

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class BankTransfersTest {
    companion object {
        private lateinit var tokenA: String
        private lateinit var tokenB: String
        private var accountA = 0
        private var accountB = 0

        @BeforeAll
        @JvmStatic
        fun init() {
            resetDatabase()
            transaction {
                val a = User.createUser(testUserA)
                val b = User.createUser(testUserB)
                a.addFriend(b)
                b.addFriend(a)

                accountA = BankAccount.create(
                    a,
                    SNewBankAccount(SBankAccountType.Debit, "A's account", 0, Currency.ROMANIAN_LEU, 0.0),
                    null
                ).also { it.balance = 100.0.toBigDecimal() }.id.value
                val acc = BankAccount.create(
                    b,
                    SNewBankAccount(SBankAccountType.Debit, "B's account", 0, Currency.ROMANIAN_LEU, 0.0),
                    null
                )
                accountB = acc.id.value
                b.setDefaultAccount(acc, false)

                tokenA = a.createSessionToken()
                tokenB = b.createSessionToken()
            }
        }
    }

    @Test
    @Order(1)
    fun testTransferSendDefaultAccount() = testApplication {
        val clientA = baseClient(tokenA)
        val clientB = baseClient(tokenB)

        assertEquals(0, clientA.getValue<List<SBankTransfer>>("transfer/list/${testUserB.tag}").size)
        assertEquals(0, clientB.getValue<List<SBankTransfer>>("transfer/list/${testUserA.tag}").size)
        clientA.post("transfer/send") {
            setBody(SSendRequestMoney(testUserB.tag, accountA, 100.0, Currency.ROMANIAN_LEU, "a note"))
        }.assertOK()
        assertEquals(1, clientA.getValue<List<SBankTransfer>>("transfer/list/${testUserB.tag}").size)
        assertEquals(1, clientB.getValue<List<SBankTransfer>>("transfer/list/${testUserA.tag}").size)
        assertEquals(0.0, transaction { BankAccount.findById(accountA)!!.balance.toDouble() })
        assertEquals(100.0, transaction { BankAccount.findById(accountB)!!.balance.toDouble() })
    }

    @Test
    @Order(2)
    fun testTransferSend() = testApplication {
        val clientA = baseClient(tokenA)
        val clientB = baseClient(tokenB)

        clientB.post("transfer/send") {
            setBody(SSendRequestMoney(testUserA.tag, accountB, 100.0, Currency.ROMANIAN_LEU, "a note"))
        }.assertOK()

        val requests = clientA.getValue<SRecentActivity>("recentActivity/short").transferRequests
        assertEquals(1, requests.size)
        clientA.get("transfer/respond/${requests[0].id}?action=accept&accountID=$accountA").assertOK()
        assertEquals(100.0, transaction { BankAccount.findById(accountA)!!.balance.toDouble() })
        assertEquals(0.0, transaction { BankAccount.findById(accountB)!!.balance.toDouble() })
    }
}