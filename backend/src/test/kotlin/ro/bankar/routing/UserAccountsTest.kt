package ro.bankar.routing

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import ro.bankar.baseClient
import ro.bankar.model.*
import ro.bankar.resetDatabase
import ro.bankar.testUser
import kotlin.test.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class UserAccountsTest {
    companion object {
        private lateinit var authToken: String

        @JvmStatic
        @BeforeAll
        fun init() {
            resetDatabase()
        }
    }

    /**
     * Should not be able to access protected routes without logging in
     */
    @Test
    @Order(0)
    fun testAccessDenied() = testApplication {
        val client = baseClient()
        val response = client.get("profile")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    @Order(1)
    fun testSignup() = testApplication {
        val client = baseClient()

        var response = client.post("signup/initial") {
            setBody(testUser)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val session = response.headers["SignupSession"]
        assertNotNull(session)

        response = client.post("signup/final") {
            setBody(SSMSCodeData("123456"))
            header("SignupSession", session)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val token = response.headers[HttpHeaders.Authorization]
        assertTrue { token?.startsWith("Bearer ") ?: false }
        authToken = token!!
    }

    @Test
    @Order(2)
    fun testLogout() = testApplication {
        val client = baseClient(authToken)
        val response = client.get("signOut")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @Order(3)
    fun testLogin() = testApplication {
        val client = baseClient()

        var response = client.post("login/initial") {
            setBody(SInitialLoginData(testUser.tag, testUser.password))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val session = response.headers["LoginSession"]
        assertNotNull(session)

        response = client.post("login/final") {
            setBody(SSMSCodeData("123456"))
            header("LoginSession", session)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val token = response.headers[HttpHeaders.Authorization]
        assertTrue { token?.startsWith("Bearer ") ?: false }
        authToken = token!!
    }

    @Test
    @Order(4)
    fun testAccessGranted() = testApplication {
        val client = baseClient(authToken)
        val response = client.get("profile")
        assertEquals(HttpStatusCode.OK, response.status)
        when (val data = response.body<Response<SUser>>()) {
            is ValueResponse -> assertEquals(testUser.tag, data.value.tag)
            else -> fail("incorrect response type")
        }
    }
}