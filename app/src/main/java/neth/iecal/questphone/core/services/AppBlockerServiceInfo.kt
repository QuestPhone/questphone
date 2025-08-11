package neth.iecal.questphone.core.services

import android.content.Context


const val INTENT_ACTION_REFRESH_APP_BLOCKER = "launcher.launcher.refresh.appblocker"
const val INTENT_ACTION_UNLOCK_APP = "launcher.launcher.refresh.appblocker.cooldown"
const val INTENT_ACTION_START_DEEP_FOCUS = "launcher.launcher.start.deepfocus"
const val INTENT_ACTION_STOP_DEEP_FOCUS = "launcher.launcher.stop.deepfocus"

object AppBlockerServiceInfo{
    var appBlockerService: AppBlockerService? = null
    var isUsingAccessibilityService = false
    val deepFocus = DeepFocus()

    // Store the unlock time for each app that is temporarily unlocked
    var unlockedApps = mutableMapOf<String, Long>()

}

fun reloadServiceInfo(context: Context){
    val sp = context.getSharedPreferences("service_info", Context.MODE_PRIVATE)
    AppBlockerServiceInfo.isUsingAccessibilityService = sp.getBoolean("is_using_accessibility",false)
}
data class DeepFocus (
    var exceptionApps: HashSet<String> = hashSetOf(),
    var isRunning: Boolean = false,
    var duration: Long = 0L
)