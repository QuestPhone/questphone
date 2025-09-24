package neth.iecal.questphone
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import neth.iecal.questphone.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle data messages or notification messages
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // send token to your backend if needed
    }

    private fun showNotification(title: String?, message: String?) {
        val channelId = "firebase_notifs"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Important Sync Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title ?: "FCM Message")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(0, notification)
    }
}
