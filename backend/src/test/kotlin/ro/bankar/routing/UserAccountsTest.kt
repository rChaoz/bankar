package ro.bankar.routing

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import ro.bankar.client
import ro.bankar.model.SInitialLoginData
import ro.bankar.model.SNewUser
import ro.bankar.model.SSMSCodeData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class UserAccountsTest {
    companion object {
        private const val USERNAME = "test-user"
        private const val PASSWORD = "Str0ngP@ss"
    }

    private lateinit var authToken: String

    @Test
    @Order(1)
    fun testSignup() = testApplication {
        val client = client()

        var response = client.post("signup/initial") {
            setBody(SNewUser(
                email = "sample@email.com",
                tag = USERNAME,
                phone = "+40123456789",
                password = PASSWORD,
                firstName = "Test",
                middleName = null,
                lastName = "User",
                dateOfBirth = LocalDate(2002, 2, 3),
                countryCode = "RO",
                state = "Bucuresti",
                city = "Bucuresti",
                address = "the address"
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val session = response.headers["SignupSession"]
        assertNotNull(session)

        response = client.post("signup/final") {
            setBody(SSMSCodeData("123456"))
            header("SignupSession", session)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue { response.headers[HttpHeaders.Authorization]?.startsWith("Bearer ") ?: false }
        authToken = response.headers[HttpHeaders.Authorization]!!
    }

    @Test
    @Order(2)
    fun testLogout() = testApplication {
        val client = client()
        val response = client.get("signOut") {
            header(HttpHeaders.Authorization, authToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    @Order(3)
    fun testLogin() = testApplication {
        val client = client()

        var response = client.post("login/initial") {
            setBody(SInitialLoginData(USERNAME, PASSWORD))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val session = response.headers["LoginSession"]
        assertNotNull(session)

        response = client.post("login/final") {
            setBody(SSMSCodeData("123456"))
            header("LoginSession", session)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue { response.headers[HttpHeaders.Authorization]?.startsWith("Bearer ") ?: false }
    }
}