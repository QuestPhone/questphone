package neth.iecal.questphone.core.utils.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.data.ReminderData
import neth.iecal.questphone.data.ReminderDatabaseProvider
import java.util.Date

/**
 * Handles the scheduling and cancellation of reminder notifications using AlarmManager.
 * It also manages the creation of the notification channel.
 */
class NotificationScheduler(private val context: Context,val questRepository: QuestRepository? = null) {

    internal val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val REMINDER_CHANNEL_ID = "habit_tracker_reminder_channel"
        const val REMINDER_CHANNEL_NAME = "Habit Tracker Reminders"

        // Prefix for notification IDs to ensure uniqueness and avoid conflicts with other app notifications
        const val REMINDER_NOTIFICATION_ID_PREFIX = 1000

        // Keys for passing data to the ReminderBroadcastReceiver
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_DESCRIPTION = "extra_reminder_description"
    }

    /**
     * Creates the notification channel for reminders.
     */
    fun createNotificationChannel() {
        val soundUri = "android.resource://${context.packageName}/raw/notification".toUri()
        val attributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        // Notification channels are only required for Android O (API 26) and above
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            REMINDER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // High importance ensures the notification is prominent
        ).apply {
            description = "Channel for habit tracker reminder notifications."
            enableLights(true) // Enable LED light for notifications
            lightColor = Color.GREEN // Set the LED light color
            enableVibration(true) // Enable vibration for notifications
            setSound(soundUri, attributes)
        }
        notificationManager.createNotificationChannel(channel)
        Log.d("NotificationScheduler", "Notification channel '$REMINDER_CHANNEL_NAME' created.")
    }

    /**
     * Schedules a single reminder notification using AlarmManager.
     * This method uses `setExactAndAllowWhileIdle` for efficiency and reliability,
     * especially during Doze mode (Android M+).
     *
     * @param reminder The [ReminderData] object containing all details for the reminder.
     */
    fun scheduleReminder(reminder: ReminderData) {
        // Create an Intent that will be broadcast when the alarm fires.
        // This intent targets our ReminderBroadcastReceiver.
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            // Put reminder data as extras in the intent
            putExtra(EXTRA_REMINDER_ID, reminder.quest_id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_DESCRIPTION, reminder.description)
        }

        // Create a PendingIntent. This is a token that the AlarmManager can use
        // to send the Intent on behalf of our application.
        // FLAG_UPDATE_CURRENT: If a PendingIntent with the same request code already exists,
        // its extras will be updated with the new ones.
        // FLAG_IMMUTABLE: Required for Android 6.0 (API 23) and above for security reasons.
        // It means the PendingIntent cannot be modified after creation.
        // The reminder.id is used as the request code to ensure each reminder has a unique PendingIntent.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.quest_id.hashCode(), // Unique request code for each reminder
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine which AlarmManager method to use based on API level for maximum efficiency.
        // RTC_WAKEUP: Wakes up the device to fire the alarm at the specified time.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 (API 31) and above, explicit permission is required for exact alarms.
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.timeMillis,
                    pendingIntent
                )
                Log.d(
                    "NotificationScheduler",
                    "Scheduled exact and allow while idle reminder ID: ${reminder.quest_id} for ${
                        Date(reminder.timeMillis)
                    }"
                )
            } else {
                // If permission is not granted, exact alarms cannot be scheduled.
                // You should inform the user and potentially direct them to settings.
                Log.w(
                    "NotificationScheduler",
                    "SCHEDULE_EXACT_ALARM permission not granted. Cannot schedule exact alarm for ID: ${reminder.quest_id}. Please grant the permission in app settings."
                )
                // Optionally, you can show a dialog or toast directing the user to settings:
                // val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // context.startActivity(settingsIntent) // Requires activity context
            }
        } else {
            // For Android M (API 23) to Android R (API 30), use setExactAndAllowWhileIdle.
            // This is crucial for reliability when the device is in Doze mode.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.timeMillis,
                pendingIntent
            )
            Log.d(
                "NotificationScheduler",
                "Scheduled exact and allow while idle reminder ID: ${reminder.quest_id} for ${
                    Date(reminder.timeMillis)
                }"
            )
        }

        /**
         * Cancels a previously scheduled reminder notification.
         * This will remove the alarm from AlarmManager and cancel the associated PendingIntent.
         *
         * @param reminderId The unique ID of the reminder to cancel.
         */
        fun cancelReminder(reminderId: Int) {
            // Recreate the same Intent used for scheduling to get the correct PendingIntent
            val intent = Intent(context, ReminderBroadcastReceiver::class.java)

            // FLAG_NO_CREATE: Returns null if the PendingIntent does not exist,
            // otherwise it returns the existing one.
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent) // Cancel the alarm
                pendingIntent.cancel() // Cancel the PendingIntent itself
                Log.d("NotificationScheduler", "Cancelled reminder ID: $reminderId.")
            } else {
                Log.d(
                    "NotificationScheduler",
                    "Reminder ID $reminderId not found in AlarmManager to cancel."
                )
            }
            // Also ensure to remove the notification from the notification bar if it's currently visible
            notificationManager.cancel(REMINDER_NOTIFICATION_ID_PREFIX + reminderId)
        }

        /**
         * Reschedules all previously set alarms. This method is typically called on device boot
         * to restore alarms that were lost when the device was turned off or restarted.
         * It requires the app to persist the reminder data (e.g., in a database like Room or SharedPreferences).
         *
         * @param persistedReminders A list of [ReminderData] objects that were previously saved.
         */
        fun rescheduleAllReminders(persistedReminders: List<ReminderData>) {
            Log.d(
                "NotificationScheduler",
                "Attempting to reschedule ${persistedReminders.size} reminders."
            )
            persistedReminders.forEach { reminder ->
                // Only reschedule reminders whose time is in the future.
                // Past reminders should not be rescheduled.
                if (reminder.timeMillis > System.currentTimeMillis()) {
                    scheduleReminder(reminder)
                } else {
                    Log.d(
                        "NotificationScheduler",
                        "Skipping past reminder ID: ${reminder.quest_id} for reschedule."
                    )
                }
            }
        }
    }

    fun getPersistedReminders(context: Context): List<ReminderData> {
        val dao = ReminderDatabaseProvider.getInstance(context).reminderDao()
        return dao.getAll()
    }

    /**
     * Resets reminders that may have been deleted + generate reminders for the quests that have not been generated
     */
    fun reloadAllReminders() {
        Log.d("Rescheduling reminders", "Attempting to reschedule reminders...")
        createNotificationChannel()

        val reminderDao = ReminderDatabaseProvider.getInstance(context).reminderDao()

        CoroutineScope(Dispatchers.Default).launch {
            val persistedReminders = reminderDao.getAllUpcoming()
            val persistedQuestIds = persistedReminders.map { it.quest_id }.toSet()

            val allQuests = questRepository!!.getAllQuests().first()
            val allQuestIds = allQuests.map { it.id }.toSet()

            // Generate reminders for quests that have no reminder yet
            allQuestIds.forEach { questId ->
                if (questId !in persistedQuestIds) {
                    questRepository.getQuestById(questId)
                        ?.let { generateQuestReminder(context, it) }
                }
            }

            // Reschedule existing reminders
            persistedReminders.forEach { reminder ->
                if (reminder.timeMillis < System.currentTimeMillis()) {
                    questRepository.getQuestById(reminder.quest_id)
                        ?.let { generateQuestReminder(context, it) }
                } else {
                    scheduleReminder(reminder)
                }
            }

            Log.d(
                "Rescheduling reminders",
                "Finished rescheduling ${persistedReminders.size} reminders."
            )
        }
    }
}