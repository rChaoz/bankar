package ro.bankar.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ro.bankar.DEV_MODE
import ro.bankar.SKIP_DELIVERY_CHECK
import ro.bankar.generateNumeric
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object SmsService {
    @Serializable
    data class SmsResponse(val status: Int)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        defaultRequest {
            url("https://api.sendsms.ro/json")
        }
    }

    private var username: String? = null
    private var apiKey: String? = null
    private var reportURL: String? = null

    fun configure(username: String?, apiKey: String?, reportURL: String?) {
        this.username = username
        this.apiKey = apiKey
        this.reportURL = reportURL
        if (DEV_MODE && (username == null || apiKey == null || reportURL == null))
            throw IllegalStateException("SmsService requires non-null username, apiKey, reportURL outside DEV mode")
    }

    private val smsMap = HashMap<String, Continuation<Boolean>>()

    fun onSmsReport(messageID: String, status: Int) =
        if (messageID in smsMap) {
            smsMap[messageID]!!.resume(status == 1)
            true
        } else false

    private suspend fun sendMessage(phone: String, message: String): Boolean {
        // Don't actually send SMS message in development mode, just pretend we did
        if (DEV_MODE) return true

        val user = username
        val key = apiKey
        if (user == null || key == null) throw IllegalStateException("Cannot send SMS if username/key are not set outside DEV mode")
        val messageID = generateNumeric(10)
        println(messageID)

        // Send request
        val response = client.get {
            url.parameters.apply {
                append("action", "message_send")
                append("username", user)
                append("password", key)
                append("to", phone)
                append("from", "BanKAR")
                append("text", message)
                if (reportURL != null) {
                    append("report_mask", "19")
                    append("report_url", "$reportURL&message_id=$messageID")
                }
            }
        }.body<SmsResponse>()
        // Check status code
        if (response.status != 1) return false
        if (SKIP_DELIVERY_CHECK) return true
        // Await delivery confirmation
        return try {
            withTimeout(10000) { suspendCancellableCoroutine { smsMap[messageID] = it } }
        } catch (e: TimeoutCancellationException) {
            false
        } finally {
            smsMap.remove(messageID)
        }
    }

    suspend fun sendCode(phone: String, code: String) = sendMessage(phone, "Hey! Your BanKAR code is $code. Do not share this with anyone!")
}