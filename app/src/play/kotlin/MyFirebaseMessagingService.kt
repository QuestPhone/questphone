package neth.iecal.questphone

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.EntryPointAccessors
import neth.iecal.questphone.core.utils.FcmHandler
import neth.iecal.questphone.backed.repositories.UserRepositoryEntryPoint


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("remoteMSg",remoteMessage.data.toString())
        Log.d("remoteNotif", remoteMessage.notification?.body.toString())

        val entryPoint = EntryPointAccessors.fromApplication(this, UserRepositoryEntryPoint::class.java)
        val userRepository = entryPoint.userRepository()

        if(remoteMessage.data.isNotEmpty()){
            FcmHandler.handleData(this,remoteMessage.data,userRepository)
            remoteMessage.notification?.let {
                if(!remoteMessage.data.containsKey("no_notification")) {
                    showNotification(remoteMessage.data["title"], remoteMessage.data["body"])
                }
            }
        }
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
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
    override fun handleIntent(intent: Intent) {
        try {
            if (intent.extras != null) {
                val builder = RemoteMessage.Builder("MessagingService")

                for (key in intent.extras!!.keySet()) {
                    builder.addData(key, intent.extras!!.get(key).toString())
                }

                onMessageReceived(builder.build())
            } else {
                super.handleIntent(intent)
            }
        } catch (e: Exception) {
            super.handleIntent(intent)
        }
    }
}
