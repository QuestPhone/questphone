package neth.iecal.questphone.utils

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import neth.iecal.questphone.services.LockScreenService

fun isLockScreenServiceEnabled(context: Context): Boolean {
    val expectedService = "${context.packageName}/${LockScreenService::class.java.name}"
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val services = TextUtils.SimpleStringSplitter(':')
    services.setString(enabledServices)
    for (service in services) {
        if (service.equals(expectedService, ignoreCase = true)) {
            return true
        }
    }
    return false
}

@RequiresApi(Build.VERSION_CODES.P)
fun performLockScreenAction() {
    LockScreenService.instance?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
}

/*
fun isAccessibilityServiceEnabled( context : Context, serviceClass: Class<out AccessibilityService>): Boolean {
    val serviceName = ComponentName(context, serviceClass).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val isAccessibilityEnabled = Settings.Secure.getInt(
        context.contentResolver,
        Settings.Secure.ACCESSIBILITY_ENABLED,
        0
    )
    return isAccessibilityEnabled == 1 && enabledServices.contains(serviceName)
}
*/

fun openAccessibilityServiceScreen(context: Context,cls: Class<*>) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val componentName = ComponentName(context, cls)
        intent.putExtra(":settings:fragment_args_key", componentName.flattenToString())
        val bundle = Bundle()
        bundle.putString(":settings:fragment_args_key", componentName.flattenToString())
        intent.putExtra(":settings:show_fragment_args", bundle)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to general Accessibility Settings
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
fun sendRefreshRequest(context: Context, action: String) {
    Log.d("refreshReq","loaded")
    val intent = Intent(action)
    context.sendBroadcast(intent)
}
