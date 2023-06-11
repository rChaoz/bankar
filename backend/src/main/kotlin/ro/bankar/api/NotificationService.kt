package ro.bankar.api

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.error
import ro.bankar.DEV_MODE
import ro.bankar.database.User

object NotificationService {
    private const val JSON_KEY = "/firebase-admin-key.private.json"
    private var messaging: FirebaseMessaging? = null
    private val logger = KtorSimpleLogger("NotificationService")

    fun sendMessageNotification(from: User, to: User, message: String) = runCatching {
        // If messaging service is not initialized, don't send a notification
        val messaging = this.messaging ?: return@runCatching

        val token = to.notificationToken ?: run {
            // Can't send a message if the user doesn't have a token
            logger.info("User @${to.tag} doesn't have a notification token")
            return@runCatching
        }
        messaging.send(
            Message.builder().apply {
                setToken(token)
                putData("source", from.tag)
                putData("message", message)
            }.build()
        )
        logger.info("Sent message notification to @${to.tag} from @${from.tag}")
    }.onFailure { e ->
        logger.error("Failed to send message notification to @${to.tag} from @${from.tag}")
        logger.error(e)
    }

    init {
        // Initialize Firebase messaging service
        val stream = try {
            javaClass.getResourceAsStream(JSON_KEY)
        } catch (e: Exception) {
            if (!DEV_MODE) {
                logger.error("Failed to load Firebase credentials: ${e.message ?: e::class.simpleName}")
                throw e
            } else println("Failed to load Firebase credentials: ${e.message ?: e::class.simpleName}")
            null
        }
        messaging = if (stream != null) {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build()
            val app = FirebaseApp.initializeApp(options)
            FirebaseMessaging.getInstance(app)
        } else null
    }
}