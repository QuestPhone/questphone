package neth.iecal.questphone.core.utils.reminder


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.CallSuper
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.R
import neth.iecal.questphone.backed.repositories.QuestRepository
import nethical.questphone.core.core.utils.getCurrentDate
import javax.inject.Inject

/**
 * A BroadcastReceiver that receives intents from AlarmManager when a scheduled reminder fires.
 * It's responsible for building and displaying the notification.
 */
abstract class HiltBroadcastReceiver : BroadcastReceiver() {
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {}
}

@AndroidEntryPoint(BroadcastReceiver::class)
class ReminderBroadcastReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var questRepository: QuestRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Ensure context and intent are not null
        context.let {
            // Extract reminder details from the intent extras
            val reminderId = intent.getStringExtra(NotificationScheduler.EXTRA_REMINDER_ID) ?: ""
            val title = intent.getStringExtra(NotificationScheduler.EXTRA_REMINDER_TITLE) ?: "Reminder"
            val description = intent.getStringExtra(NotificationScheduler.EXTRA_REMINDER_DESCRIPTION)
                ?: "You have a new reminder."

            CoroutineScope(Dispatchers.Default).launch {
                val quest = questRepository.getQuestById(reminderId)
                withContext(Dispatchers.Main) {
                    if(quest!=null){
                        val isNotCompleted = quest.last_completed_on != getCurrentDate()
                        generateQuestReminder(context,quest)
                        if (reminderId.isNotEmpty()) {
                            if(isNotCompleted) {
                                // Display the notification using the extracted details
                                showNotification(it, reminderId, title, description)
                                Log.d("ReminderBroadcastReceiver", "Received alarm for reminder ID: $reminderId, Title: '$title'")
                            }
                        } else {
                            Log.e("ReminderBroadcastReceiver", "Received intent with an invalid reminder ID.")
                        }
                    }
                }
            }

        }
    }

    /**
     * Builds and displays the actual notification.
     * @param context The application context.
     * @param reminderId The unique ID of the reminder.
     * @param title The title of the notification.
     * @param description The body text of the notification.
     */
    private fun showNotification(context: Context, reminderId: String, title: String, description: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        // Create an Intent to open the main activity of the app when the notification is tapped.
        // flags:
        //   - FLAG_ACTIVITY_NEW_TASK: Starts the activity in a new task.
        //   - FLAG_ACTIVITY_CLEAR_TASK: Clears any existing task associated with the activity.
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.putExtra("quest_id",reminderId)
        // Create a PendingIntent for the notification content.
        // Use the reminderId as the request code to ensure uniqueness.
        val pendingIntent = PendingIntent.getActivity(
            context,4693,
//            reminderId,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification using NotificationCompat.Builder for backward compatibility.
        val notification = NotificationCompat.Builder(context, NotificationScheduler.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // **IMPORTANT: Replace with your app's notification icon.**
            // This icon must be monochrome for Android 5.0+
            .setContentTitle(title) // Set the notification title
            .setContentText(description) // Set the notification body text
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority (visual prominence)
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Categorize as a reminder
            .setContentIntent(pendingIntent) // Set the action when notification is clicked
            .setGroup(null) // avoid accidental grouping
            .setGroupSummary(false)
            .setAutoCancel(false) // Automatically dismisses the notification when tapped by the user
            .build()

        // Display the notification.
        // The notification ID is formed by adding a prefix to the reminder ID to ensure it's unique
        // and doesn't conflict with other possible notification IDs in your app.
        notificationManager.notify(NotificationScheduler.REMINDER_NOTIFICATION_ID_PREFIX + reminderId.hashCode(), notification)
        Log.d("ReminderBroadcastReceiver", "Notification displayed for ID: $reminderId.")
    }
}