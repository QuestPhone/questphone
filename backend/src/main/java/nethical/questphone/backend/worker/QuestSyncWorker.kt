package nethical.questphone.backend.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.core.R
import nethical.questphone.data.SyncStatus

@HiltWorker
class QuestSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val questRepository: QuestRepository
) : CoroutineWorker(context, params) {

    val context = context.applicationContext
    override suspend fun doWork(): Result {
        try {

            val userId = Supabase.supabase.auth.currentUserOrNull()?.id ?: return Result.success()
            val isFirstTimeSync = inputData.getBoolean("is_first_time",false)

            Log.d("QuestSyncManager", "Starting sync for $userId")
            showSyncNotification(context)
            sendSyncBroadcast(context, SyncStatus.ONGOING)

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
                    questRepository.upsertQuest(it.copy(synced = true))
                }
                return Result.success()
            }

            val unSyncedQuests = questRepository.getUnSyncedQuests().first()
            unSyncedQuests.forEach {
                Supabase.supabase.postgrest["quests"].upsert(it)
                questRepository.markAsSynced(it.id)
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
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

