package ro.bankar.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.logging.*
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ro.bankar.DEV_MODE
import ro.bankar.generateToken
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object SmsService {
    private val log = KtorSimpleLogger(SmsService::class.qualifiedName ?: "BankarSmsService")

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
        if (!DEV_MODE) {
            if (username == null || apiKey == null)
                throw IllegalStateException("SmsService requires non-null username, apiKey, reportURL outside DEV mode")
            if (reportURL === null)
                log.warn("SmsService was configured without a reportURL - delivery check will not happen")
        }
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
        if (user == null || key == null) throw IllegalStateException("Cannot send SMS if username/key have not been configured")
        val messageID = generateToken()

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
        if (reportURL == null) return true
        // Await delivery confirmation
        return try {
            withTimeout(5000) { suspendCancellableCoroutine { smsMap[messageID] = it } }
        } catch (e: TimeoutCancellationException) {
            // Sometimes, delivery check doesn't work, which won't allow us to detect bad phone number/similar
            // But don't prevent user from logging in due to this
            true
        } finally {
            smsMap.remove(messageID)
        }
    }

    suspend fun sendCode(phone: String, code: String) = sendMessage(phone, "Hey! Your BanKAR code is $code. Do not share this with anyone!")
}