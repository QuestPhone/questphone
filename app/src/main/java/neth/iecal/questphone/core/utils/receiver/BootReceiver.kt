package neth.iecal.questphone.core.utils.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler
import neth.iecal.questphone.backed.repositories.QuestRepository
import javax.inject.Inject

/**
 * A BroadcastReceiver that listens for device boot completion and package replacement events.
 * Its purpose is to reschedule all previously set alarms, as alarms are cleared by the system
 * when the device restarts or the app is updated/reinstalled.
 */
@AndroidEntryPoint(BroadcastReceiver::class)
class BootReceiver : BroadcastReceiver() {
    // Configure Json parser to ignore unknown keys for robust parsing
    @Inject lateinit var questRepository: QuestRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        // Check if the received action is ACTION_BOOT_COMPLETED (device boot)
        // or ACTION_MY_PACKAGE_REPLACED (app updated/reinstalled)
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            context?.let {
                Log.d("BootReceiver", "Device booted or package replaced. Attempting to reschedule reminders...")
                val scheduler = NotificationScheduler(it,questRepository)
                scheduler.reloadAllReminders()
            }
        }
    }
}