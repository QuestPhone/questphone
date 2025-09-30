package neth.iecal.questphone.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import neth.iecal.questphone.app.screens.launcher.PushProvider

class PlayPushProvider : PushProvider {
    override fun getFCMToken(onTokenReceived: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onTokenReceived(task.result)
            } else {
                onTokenReceived(null)
            }
        }
    }
}


class FdroidPushProvider : PushProvider {
    override fun getFCMToken(onTokenReceived: (String?) -> Unit) {
        Log.d("FCM", "Skipping FCM on F-Droid build")
        onTokenReceived(null)
    }
}