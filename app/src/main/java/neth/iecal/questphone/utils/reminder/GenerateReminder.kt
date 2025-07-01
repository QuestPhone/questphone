package neth.iecal.questphone.utils.reminder

import android.content.Context
import android.util.Log
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.ReminderData
import neth.iecal.questphone.data.ReminderDatabaseProvider
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.utils.Supabase
import neth.iecal.questphone.utils.ai.ReminderClient
import neth.iecal.questphone.utils.getTimeRemainingDescription
import neth.iecal.questphone.utils.unixToReadable
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

fun generateReminders(context: Context, quest: CommonQuestInfo) {
    val dao = ReminderDatabaseProvider.getInstance(context).reminderDao()
    val reminderClient = ReminderClient()
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

        val tooLateToday = currentHour >= endHour

        // --- If already hit limit or it's too late, schedule for next day ---
        if (count >= 2 || tooLateToday) {
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

            reminderClient.generateReminder(getTimeRemainingDescription(endHour), quest.id, userId) { result ->
                val data = result.getOrDefault(
                    ReminderClient.ReminderResult(quest.title, getRandomReminderLine(context) ?: "")
                )

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
            }
            return@launch
        }

        // --- Otherwise, schedule for today ---
        val nextHourToday: Int
        val randomMinuteToday: Int

        when (count) {
            0 -> {
                // First reminder for today: try to schedule in the morning if possible, otherwise slightly after current hour
                val morningCutoffHour = 10 // e.g., until 10 AM is considered morning
                if (currentHour < morningCutoffHour) {
                    nextHourToday = Random.nextInt(maxOf(currentHour + 1, startHour, 7), minOf(morningCutoffHour, endHour)) // Random hour between current+1 (or 7am) and morning cutoff
                    randomMinuteToday = Random.nextInt(0, 60)
                } else {
                    // If already past morning, schedule for currentHour + 1 or startHour
                    nextHourToday = maxOf(currentHour + 1, startHour)
                    randomMinuteToday = Random.nextInt(0, 60)
                }
            }
            1 -> {
                // Second reminder for today: schedule a bit later, but before endHour
                // Option 1: A few hours after the first reminder's typical time (if available in DB)
                // For simplicity here, let's just schedule currentHour + 2 or minOf(endHour - 1)
                nextHourToday = minOf(endHour - 1, currentHour + 2)
                randomMinuteToday = Random.nextInt(0, 60)
            }
            else -> return@launch // Should not happen with count >= 2 handled above
        }

        // Skip scheduling during explicit sleep hours (e.g., after 22:00 and before 7:00)
        // This check should be applied to the *final calculated* nextHourToday
        if (nextHourToday in 22..23 || nextHourToday < 7) {
            Log.d("Reminder", "Skipping reminder as it falls in sleep hours: $nextHourToday")
            return@launch
        }


        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextHourToday)
            set(Calendar.MINUTE, randomMinuteToday)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        reminderClient.generateReminder(getTimeRemainingDescription(endHour), quest.id, userId) { result ->
            val data = result.getOrDefault(
                ReminderClient.ReminderResult(quest.title, "Time to do this quest")
            )

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