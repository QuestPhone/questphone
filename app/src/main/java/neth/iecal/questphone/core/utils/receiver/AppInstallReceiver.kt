package neth.iecal.questphone.core.utils.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppInstallReceiver(
    private val onAppInstalled: (String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart
            if (packageName != null) {
                onAppInstalled(packageName)
            }
        }
    }
}
