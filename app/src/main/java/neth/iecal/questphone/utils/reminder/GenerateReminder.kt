package neth.iecal.questphone.utils.reminder

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.ReminderData
import neth.iecal.questphone.data.ReminderDatabaseProvider
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.utils.ai.ReminderClient
import neth.iecal.questphone.utils.getTimeRemainingDescription
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun GenerateReminders(context: Context, quest: CommonQuestInfo) {
    val dao = ReminderDatabaseProvider.getInstance(context).reminderDao()
    val reminderClient = ReminderClient()
    val userId = User.userInfo.userId

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

            val nextHour = maxOf(startHour, 8)
            val timeMillis = Calendar.getInstance().apply {
                time = tomorrow.time
                set(Calendar.HOUR_OF_DAY, nextHour)
                set(Calendar.MINUTE, (5..10).random())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            reminderClient.generateReminder(getTimeRemainingDescription(endHour), quest.id, userId) { result ->
                val data = result.getOrDefault(
                    ReminderClient.ReminderResult(quest.title, "Time to do this quest")
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
                    NotificationScheduler(context).scheduleReminder(tData)
                }
            }
            return@launch
        }

        // --- Otherwise, schedule for today ---
        val nextHour = when (count) {
            0 -> maxOf(currentHour + 1, startHour)
            1 -> minOf(endHour - 1, currentHour + 2)
            else -> return@launch
        }

        // Skip scheduling during sleep hours (e.g., after 22:00)
        if (nextHour in 22..23 || nextHour < 7) {
            return@launch
        }

        val scheduledTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, nextHour)
            set(Calendar.MINUTE, (3..10).random())
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
        }
    }
}


fun getTodayMillis(hour: Int): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}