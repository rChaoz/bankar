package ro.bankar.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class MessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Send a notification that, when clicked, opens the conversation, not just the app
        val from = remoteMessage.data["source"]
        val message = remoteMessage.data["message"]
        if (from == null || message == null) {
            // Invalid message
            Log.w(TAG, "Invalid message received: $remoteMessage")
            return
        }

        // TODO Get user profile from cache

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "messages",
                getString(R.string.messages),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            Notification.Builder(this, "messages")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        notification
            .setContentTitle("@$from")
            .setContentText(message)
            .setColor(getColor(R.color.primary))
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setAutoCancel(true)
            // TODO .setContentIntent(navDeepLink { uriPattern = "${Nav.Main.route}/" }.)
            .build().let { notificationManager.notify(Random.nextInt(), it) }
    }
}