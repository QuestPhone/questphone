package neth.iecal.questphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.backend.triggerProfileSync
import nethical.questphone.backend.triggerQuestSync
import nethical.questphone.backend.triggerStatsSync
import javax.inject.Inject

@AndroidEntryPoint(FirebaseMessagingService::class)
class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Inject what you need
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var questRepository: QuestRepository

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage.data.isNotEmpty()){
            if(remoteMessage.data.contains("refreshQuestId")){
                triggerQuestSync(this, pullForQuest = remoteMessage.data["refreshQuestId"])
                triggerStatsSync(this, pullAllForToday = true)
            }
            if(remoteMessage.data.contains("refreshProfile")){
                triggerProfileSync(this)
            }
        }
        remoteMessage.notification?.let {
            showNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        userRepository.saveFcmToken(token)
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
