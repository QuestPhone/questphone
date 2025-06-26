package neth.iecal.questphone

import kotlinx.serialization.Serializable

/**
 * Data class representing a single reminder.
 * @param id A unique identifier for the reminder. This will be used as the request code for PendingIntent.
 * @param timeMillis The exact time the reminder should fire, in Unix milliseconds.
 * @param title The title of the notification.
 * @param description The detailed text of the notification.
 */
@Serializable
data class ReminderData(
    val id: Int,
    val timeMillis: Long,
    val title: String,
    val description: String
)