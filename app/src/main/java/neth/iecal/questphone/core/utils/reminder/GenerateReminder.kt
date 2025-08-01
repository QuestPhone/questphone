package neth.iecal.questphone.core.utils.reminder

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import nethical.questphone.backend.Supabase
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.unixToReadable
import nethical.questphone.data.ReminderData
import nethical.questphone.data.ReminderDatabaseProvider
import nethical.questphone.data.quest.CommonQuestInfo
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

data class ReminderResult(
    val title: String,
    val description: String
)

fun generateReminders(context: Context, quest: CommonQuestInfo) {
    val dao = ReminderDatabaseProvider.getInstance(context).reminderDao()
    val userId = Supabase.supabase.auth.currentAccessTokenOrNull().toString()

    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val now = Calendar.getInstance()
    val today = sdf.format(now.time)

    val (startHour, endHour) = quest.time_range
    val currentHour = now.get(Calendar.HOUR_OF_DAY)

    CoroutineScope(Dispatchers.IO).launch {
        val existing = dao.getRemindersByQuestId(quest.id)

        var count = 0
        var scheduledDate = today

        if (existing != null && existing.date == today) {
            count = existing.count
        }

        if(quest.last_completed_on == getCurrentDate()){
            count = 3 // This seems to mark it as "done" for today, preventing more reminders today.
        }

        // Flag to determine if we should schedule for tomorrow
        var shouldScheduleTomorrow = false

        val tooLateTodayByTimeRange = currentHour >= endHour

        // --- Check conditions that directly lead to scheduling for tomorrow ---
        if (count >= 2 || tooLateTodayByTimeRange) {
            shouldScheduleTomorrow = true
        }

        // --- Otherwise, try to schedule for today ---
        var nextHourToday: Int = -1 // Initialize with a value that indicates it's not set yet
        var randomMinuteToday: Int = 0

        if (!shouldScheduleTomorrow) { // Only calculate for today if we aren't already scheduling for tomorrow
            when (count) {
                0 -> {
                    // First reminder for today: try to schedule in the morning if possible, otherwise slightly after current hour
                    val morningCutoffHour = 10 // until 10 AM is considered morning

                    if (currentHour < morningCutoffHour) {
                        val fromHour = maxOf(currentHour + 1, startHour, 7)
                        val toHour = minOf(morningCutoffHour, endHour)

                        if (fromHour < toHour) {
                            nextHourToday = Random.nextInt(fromHour, toHour)
                        } else {
                            // fallback logic: use `fromHour` directly or reschedule later
                            Log.w("Reminder", "Invalid hour range: $fromHour to $toHour. Using fallback.")
                            nextHourToday = fromHour
                        }

                        randomMinuteToday = Random.nextInt(0, 60)
                    } else {
                        nextHourToday = maxOf(currentHour + 1, startHour)
                        randomMinuteToday = Random.nextInt(0, 60)
                    }

                }
                1 -> {
                    // Second reminder for today: schedule a bit later, but before endHour
                    nextHourToday = minOf(endHour - 1, currentHour + 2)
                    randomMinuteToday = Random.nextInt(0, 60)
                }
                // No 'else' needed here, as count >= 2 would set shouldScheduleTomorrow = true
            }

            // --- Check if the calculated 'today' time falls into sleep hours ---
            // If it does, we need to schedule for tomorrow instead
            if (nextHourToday != -1 && (nextHourToday in 22..23 || nextHourToday < 7)) {
                Log.d("Reminder", "Calculated today's reminder ($nextHourToday) falls in sleep hours. Scheduling for tomorrow instead.")
                shouldScheduleTomorrow = true
            }

            // If after calculating for today, and not deciding to go to tomorrow, ensure it's not actually too late
            // This is a safety check, as the `tooLateTodayByTimeRange` handles most cases.
            if (!shouldScheduleTomorrow && nextHourToday != -1 && nextHourToday >= endHour) {
                Log.d("Reminder", "Calculated today's reminder ($nextHourToday) is after quest endHour. Scheduling for tomorrow instead.")
                shouldScheduleTomorrow = true
            }
        }


        if (shouldScheduleTomorrow) {
            val tomorrow = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
            scheduledDate = sdf.format(tomorrow.time)

            // Randomize hour for next day, focusing on morning (7 AM to 10 AM)
            val randomHourTomorrow = Random.nextInt(7, 11) // Generates a random hour between 7 (inclusive) and 11 (exclusive)
            val nextHourTomorrow = maxOf(startHour, randomHourTomorrow) // Ensure it's not before quest start hour

            val timeMillis = Calendar.getInstance().apply {
                time = tomorrow.time
                set(Calendar.HOUR_OF_DAY, nextHourTomorrow)
                set(Calendar.MINUTE, Random.nextInt(0, 60)) // More random minutes for next day
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val data = ReminderResult(quest.title, getRandomReminderLine(context) ?: "")

            val tData = ReminderData(
                quest_id = quest.id,
                timeMillis = timeMillis,
                title = data.title,
                description = data.description,
                date = scheduledDate,
                count = 1  // starting fresh tomorrow
            )

            CoroutineScope(Dispatchers.IO).launch {
                dao.upsertReminder(tData)
            }
            Log.d("Reminder Set for $userId at ", unixToReadable(tData.timeMillis))

            NotificationScheduler(context).scheduleReminder(tData)
        } else {
            // --- Otherwise, schedule for today (if shouldScheduleTomorrow is false) ---
            val scheduledTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, nextHourToday)
                set(Calendar.MINUTE, randomMinuteToday)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val data = ReminderResult(quest.title, getRandomReminderLine(context).toString())

            val updated = ReminderData(
                quest_id = quest.id,
                timeMillis = scheduledTime,
                title = data.title,
                description = data.description,
                date = scheduledDate,
                count = count + 1
            )

            CoroutineScope(Dispatchers.IO).launch {
                dao.upsertReminder(updated)
                NotificationScheduler(context).scheduleReminder(updated)
            }
            Log.d("Reminder Set for $userId at ", unixToReadable(updated.timeMillis))
        }
    }
}


fun getRandomReminderLine(context: Context): String? {
    return try {
        val inputStream = context.assets.open("reminders.csv")
        val reader = BufferedReader(inputStream.reader())
        val lines = reader.readLines()

        if (lines.isNotEmpty()) {
            lines[Random.nextInt(lines.size)]
        } else {
            null // File is empty
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}