package neth.iecal.questphone.core.utils.reminder

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.core.utils.reminder.streak.getLine
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.ReminderData
import nethical.questphone.backend.ReminderDatabaseProvider
import nethical.questphone.backend.Supabase
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.unixToReadable
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
fun generateQuestReminder(context: Context, quest: CommonQuestInfo) {
    val dao = ReminderDatabaseProvider.getInstance(context).reminderDao()
    val userId = Supabase.supabase.auth.currentAccessTokenOrNull().toString()

    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val now = Calendar.getInstance()
    val today = sdf.format(now.time)

    fun getNextLine(): String? {
        val prefs = context.getSharedPreferences("quest_guilt_notif", Context.MODE_PRIVATE)
        val lastSent = prefs.getInt("last_sent", -1)
        var index = lastSent + 1
        var line = getLine(context, "quest_guilt.txt", index)
        if (line == null) { // restart from beginning
            index = 0
            line = getLine(context, "quest_guilt.txt", index)
        }
        prefs.edit(commit = true) { putInt("last_sent", index) }
        return line
    }

    fun isFuture(timeMillis: Long) = timeMillis > System.currentTimeMillis()

    suspend fun scheduleNextDayReminder() {
        val nextDayCal = Calendar.getInstance().apply { add(Calendar.DATE, 1) }
        val nextDay = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(nextDayCal.time)

        val (startHour, endHour) = quest.time_range
        val reminderTimeCal = Calendar.getInstance().apply {
            time = nextDayCal.time
            if (startHour == 0 && endHour == 24) {
                // All-day quest: schedule morning reminder (7–10 AM) with random minutes
                set(Calendar.HOUR_OF_DAY, Random.nextInt(7, 11))
                set(Calendar.MINUTE, Random.nextInt(0, 60))
            } else {
                // Time-bound quest: schedule first reminder exactly at startHour
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val reminderTime = reminderTimeCal.timeInMillis

        val nextReminder = ReminderData(
            quest_id = quest.id,
            timeMillis = reminderTime,
            description = "Reminder: ${quest.title}",
            title = getAQuote(context).toString(),
            date = nextDay,
            count = 1
        )

        dao.upsertReminder(nextReminder)
        NotificationScheduler(context).scheduleReminder(nextReminder)
        Log.d("Reminder", "Set for ${quest.id} next day at ${unixToReadable(nextReminder.timeMillis)}")
    }

    CoroutineScope(Dispatchers.IO).launch {
        val existing = dao.getRemindersByQuestId(quest.id)

        // Clear any reminders if quest already completed today
        if (quest.last_completed_on == getCurrentDate()) {
            // Clear any reminders for today
            if (existing != null && existing.date == today) {
                dao.deleteByQuestId(quest.id)
                Log.d("Reminder", "Cleared reminders for ${quest.id} since quest already completed today.")
            }

            // Schedule for next day
            scheduleNextDayReminder()
            return@launch
        }

        val currentCount = existing?.count ?: 0
        // If we already sent both reminders, schedule for next day and stop
        if (currentCount >= 2) {
            scheduleNextDayReminder()
            return@launch
        }

        val (startHour, endHour) = quest.time_range
        var reminder: ReminderData? = null

        if (startHour == 0 && endHour == 24) {
            // --- All-day quest ---
            val morningTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, Random.nextInt(7, 11))
                set(Calendar.MINUTE, Random.nextInt(0, 60))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val eveningTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, Random.nextInt(21, 23))
                set(Calendar.MINUTE, Random.nextInt(0, 60))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            when (currentCount) {
                0 -> {
                    reminder = if (isFuture(morningTime)) {
                        ReminderData(
                            quest_id = quest.id,
                            timeMillis = morningTime,
                            description = "Reminder: ${quest.title}",
                            title = getAQuote(context).toString(),
                            date = today,
                            count = 1
                        )
                    } else if (isFuture(eveningTime)) {
                        // First reminder already missed → schedule second directly
                        ReminderData(
                            quest_id = quest.id,
                            timeMillis = eveningTime,
                            description = "Ending soon: ${quest.title}",
                            title = getNextLine().toString(),
                            date = today,
                            count = 2
                        )
                    } else {
                        // Both times have passed, schedule for next day
                        scheduleNextDayReminder()
                        null
                    }
                }
                1 -> {
                    if (isFuture(eveningTime)) {
                        reminder = ReminderData(
                            quest_id = quest.id,
                            timeMillis = eveningTime,
                            description = "Ending soon: ${quest.title}",
                            title = getNextLine().toString(),
                            date = today,
                            count = 2
                        )
                    } else {
                        // Evening time has passed, schedule for next day
                        scheduleNextDayReminder()
                    }
                }
            }
        } else {
            // --- Time-bound quest ---
            val startTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endTime = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, endHour)
                set(Calendar.MINUTE, 0)
                add(Calendar.MINUTE, -15)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            when (currentCount) {
                0 -> {
                    reminder = if (isFuture(startTime)) {
                        ReminderData(
                            quest_id = quest.id,
                            timeMillis = startTime,
                            title = "Reminder: ${quest.title}",
                            description = getAQuote(context).toString(),
                            date = today,
                            count = 1
                        )
                    } else if (isFuture(endTime)) {
                        // First reminder missed → schedule second
                        ReminderData(
                            quest_id = quest.id,
                            timeMillis = endTime,
                            description = "Ending soon: ${quest.title}",
                            title = getNextLine().toString(),
                            date = today,
                            count = 2
                        )
                    } else {
                        // Both times have passed, schedule for next day
                        scheduleNextDayReminder()
                        null
                    }
                }
                1 -> {
                    if (isFuture(endTime)) {
                        reminder = ReminderData(
                            quest_id = quest.id,
                            timeMillis = endTime,
                            description = "Ending soon: ${quest.title}",
                            title = getNextLine().toString(),
                            date = today,
                            count = 2
                        )
                    } else {
                        // End time has passed, schedule for next day
                        scheduleNextDayReminder()
                    }
                }
            }
        }

        if (reminder != null) {
            dao.upsertReminder(reminder)
            NotificationScheduler(context).scheduleReminder(reminder)
            Log.d("Reminder", "Set for $userId at ${unixToReadable(reminder.timeMillis)}")
        }
    }
}
fun getAQuote(context: Context): String? {
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

fun isFuture(timeMillis: Long) = timeMillis > System.currentTimeMillis()
