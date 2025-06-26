package neth.iecal.questphone.utils.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.serialization.json.Json
import neth.iecal.questphone.ReminderData

/**
 * A BroadcastReceiver that listens for device boot completion and package replacement events.
 * Its purpose is to reschedule all previously set alarms, as alarms are cleared by the system
 * when the device restarts or the app is updated/reinstalled.
 */
class BootReceiver : BroadcastReceiver() {
    // Configure Json parser to ignore unknown keys for robust parsing
    private val json = Json { ignoreUnknownKeys = true }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Check if the received action is ACTION_BOOT_COMPLETED (device boot)
        // or ACTION_MY_PACKAGE_REPLACED (app updated/reinstalled)
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            context?.let {
                Log.d("BootReceiver", "Device booted or package replaced. Attempting to reschedule reminders...")
                val scheduler = NotificationScheduler(it)
                scheduler.createNotificationChannel() // Ensure notification channel is recreated/exists

                // IMPORTANT: You need to retrieve your persisted reminder data here.
                // In a real application, this data would typically be stored in a database (like Room DB),
                // or for simpler cases, SharedPreferences.
                // The `getPersistedReminders` function below is a placeholder that demonstrates
                // loading from SharedPreferences.
                val persistedReminders: List<ReminderData> = getPersistedReminders(it)
                scheduler.rescheduleAllReminders(persistedReminders)
                Log.d("BootReceiver", "Finished rescheduling ${persistedReminders.size} reminders.")
            }
        }
    }

    /**
     * Dummy function to simulate retrieving persisted reminder data.
     * In a real application, replace this with your actual data persistence logic
     * (e.g., querying a Room database).
     * This example loads a JSON array of reminders from SharedPreferences.
     *
     * @param context The application context.
     * @return A list of [ReminderData] objects loaded from persistence.
     */
    private fun getPersistedReminders(context: Context): List<ReminderData> {
        val sharedPrefs = context.getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
        // Retrieve the JSON string of scheduled reminders. Default to an empty array if not found.
        val jsonString = sharedPrefs.getString("scheduled_reminders_json", "[]") ?: "[]"
        val reminders = mutableListOf<ReminderData>()
        try {
            // Use kotlinx.serialization to decode the JSON string into a List<ReminderData>
            reminders.addAll(json.decodeFromString(jsonString))
        } catch (e: Exception) { // Catch generic Exception for kotlinx.serialization errors
            Log.e("BootReceiver", "Error parsing persisted reminders JSON from SharedPreferences: ${e.message}", e)
        }
        return reminders
    }
}