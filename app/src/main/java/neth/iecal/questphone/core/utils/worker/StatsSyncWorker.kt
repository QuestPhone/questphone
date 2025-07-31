package neth.iecal.questphone.core.utils.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import neth.iecal.questphone.R
import neth.iecal.questphone.core.utils.managers.Supabase
import neth.iecal.questphone.data.SyncStatus
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.core.utils.calculateMonthsPassedAndRoundedStart

class StatsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val isFirstTimeSync = inputData.getBoolean("is_first_time",false)
            val dao = QuestDatabaseProvider.getInstance(applicationContext).questDao()
            val userId = Supabase.supabase.auth.currentUserOrNull()?.id ?: return Result.success()


            Log.d("QuestSyncManager", "Starting sync for $userId")
            showSyncNotification(applicationContext)
            sendSyncBroadcast(applicationContext, SyncStatus.ONGOING)

            val statsDao = StatsDatabaseProvider.getInstance(applicationContext).statsDao()
            val unSyncedStats = statsDao.getAllUnSyncedStats().first()

            if(isFirstTimeSync){
                val userId = Supabase.supabase.auth.currentUserOrNull()!!.id
                val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
                val startDate = calculateMonthsPassedAndRoundedStart(User.userInfo.created_on)

                val start = startDate.toString()  // e.g., "2023-01-01"
                val end = today.toString()        // e.g., "2025-06-16"

                var stats = Supabase.supabase
                    .postgrest["quest_stats"]
                    .select {
                        filter {
                            eq("user_id",userId)
                            gte("date", start)
                            lte("date", end)
                        }
                    }
                    .decodeList<StatsInfo>()
                stats.forEach {
                    statsDao.upsertStats(it.copy(isSynced = true))
                }
                return Result.success()
            }

            unSyncedStats.forEach {
                Supabase.supabase.postgrest["quest_stats"].upsert(
                    it
                )
                dao.markAsSynced(it.id)
            }

            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(1044)
            return Result.success()
        }catch (e: Exception){
            Log.e("SyncError",e.stackTraceToString())
            return Result.failure()
        }
    }
}

private fun sendSyncBroadcast(context: Context,msg: SyncStatus) {
    val intent = Intent("launcher.launcher.quest_sync")
    intent.putExtra("status", msg.ordinal)
    context.sendBroadcast(intent)
}

private fun showSyncNotification(context: Context) {
    val channelId = "sync_channel"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Create notification channel (required for Android 8+)
    val channel = NotificationChannel(
        channelId,
        "Sync Status",
        NotificationManager.IMPORTANCE_LOW
    )
    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Sync in Progress")
        .setContentText("Your Stats are syncing...")
        .setSmallIcon(R.drawable.baseline_info_24) // replace with your icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    manager.notify(1044, notification)
}

