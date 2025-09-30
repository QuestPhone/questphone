package neth.iecal.questphone.push

import android.util.Log
import neth.iecal.questphone.app.screens.launcher.PushProvider

class FdroidPushProvider : PushProvider {
    override fun getFCMToken(onTokenReceived: (String?) -> Unit) {
        Log.d("FCM", "Skipping FCM on F-Droid build")
        onTokenReceived(null)
    }
}
class PlayPushProvider : PushProvider {
    override fun getFCMToken(onTokenReceived: (String?) -> Unit) {
        Log.d("FCM", "Skipping FCM on F-Droid build")
        onTokenReceived(null)
    }
}
