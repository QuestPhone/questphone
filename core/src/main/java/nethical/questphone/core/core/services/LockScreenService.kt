package nethical.questphone.core.core.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

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
