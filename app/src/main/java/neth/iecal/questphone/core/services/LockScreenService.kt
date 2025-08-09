package neth.iecal.questphone.core.services

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
fun performLockScreenAction() {
    LockScreenService.Companion.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
}

class LockScreenService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        var instance: LockScreenService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}