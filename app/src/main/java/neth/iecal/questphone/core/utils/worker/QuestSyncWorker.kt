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
import neth.iecal.questphone.R
import neth.iecal.questphone.core.utils.managers.Supabase
import neth.iecal.questphone.data.SyncStatus
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider

class QuestSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {

            val dao = QuestDatabaseProvider.getInstance(applicationContext).questDao()
            val userId = Supabase.supabase.auth.currentUserOrNull()?.id ?: return Result.success()
            val isFirstTimeSync = inputData.getBoolean("is_first_time",false)

            Log.d("QuestSyncManager", "Starting sync for $userId")
            showSyncNotification(applicationContext)
            sendSyncBroadcast(applicationContext, SyncStatus.ONGOING)

            if(isFirstTimeSync){
                val remoteQuests = Supabase.supabase
                    .postgrest["quests"]
                    .select()
                    {
                        filter {
                            eq("user_id",userId)
                        }
                    }
                    .decodeList<CommonQuestInfo>()

                remoteQuests.forEach {
                    dao.upsertQuest(it.copy(synced = true))
                }
                return Result.success()
            }

            val unSyncedQuests = dao.getUnSyncedQuests().first()
            unSyncedQuests.forEach {
                Supabase.supabase.postgrest["quests"].upsert(it)
                dao.markAsSynced(it.id)
            }

//
//            val localQuests = dao.getAllQuests().first() // not just unsynced
//            val remoteQuests = Supabase.supabase
//                .postgrest["quests"]
//                .select()
//                {
//                    filter {
//                        eq("user_id",userId)
//                }
//                }
//                .decodeList<CommonQuestInfo>()
//
//            val localMap = localQuests.associateBy { it.id }
//            val remoteMap = remoteQuests.associateBy { it.id }
//
//            // Merge both directions
//            val allIds = (localMap.keys + remoteMap.keys)
//
//            for (id in allIds) {
//                val local = localMap[id]
//                val remote = remoteMap[id]
//
//                when {
//                    local != null && remote == null -> {
//                        // New local quest not on server yet
//                        Supabase.supabase.postgrest["quests"].upsert(local)
//                    }
//
//                    local == null && remote != null -> {
//                        // Remote quest not in local DB
//                        dao.upsertQuest(remote)
//                    }
//
//                    local != null && remote != null -> {
//                        // Compare timestamps
//                        when {
//                            local.last_updated > remote.last_updated -> {
//                                Supabase.supabase.postgrest["quests"].upsert(local)
//                            }
//
//                            remote.last_updated > local.last_updated -> {
//                                dao.upsertQuest(remote)
//                            }
//                        }
//                    }
//                }
//
//                if (local != null) dao.markAsSynced(id)
//            }


        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(1043)
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
        .setContentText("Your quests are syncing...")
        .setSmallIcon(R.drawable.baseline_info_24) // replace with your icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    manager.notify(1043, notification)
}

