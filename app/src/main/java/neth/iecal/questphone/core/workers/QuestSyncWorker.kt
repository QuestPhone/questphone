package neth.iecal.questphone.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.QuestRepositoryEntryPoint
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.backed.repositories.UserRepositoryEntryPoint
import neth.iecal.questphone.core.Supabase
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.data.SyncStatus

class QuestSyncWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val userRepository: UserRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            UserRepositoryEntryPoint::class.java
        ).userRepository()
    }

    private val questRepository: QuestRepository by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            QuestRepositoryEntryPoint::class.java
        ).questRepository()
    }

    companion object {
        const val EXTRA_IS_FIRST_TIME = "is_first_time"
        const val EXTRA_IS_PULL_SPECIFIC_QUEST = "is_for_specific_quest"
    }

    override suspend fun doWork(): Result {
        setForegroundAsync(createForegroundInfo(context))
        val isFirstTime = inputData.getBoolean(EXTRA_IS_FIRST_TIME, false)
        val specificQuestId = inputData.getString(EXTRA_IS_PULL_SPECIFIC_QUEST)

        sendSyncBroadcast(SyncStatus.ONGOING)

        try {
            performSync(isFirstTime, specificQuestId)
        } catch (e: Exception) {
            Log.e("QuestSyncWorker", "Sync failed", e)
            sendSyncBroadcast(SyncStatus.OVER)
            return Result.failure()
        }

        sendSyncBroadcast(SyncStatus.OVER)
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(context)
    }
    private suspend fun performSync(isFirstTime: Boolean, pullForSpecific: String?) {
        val userId = userRepository.getUserId()
        Log.d("QuestSyncWorker", "Starting quest sync for $userId, firstTime: $isFirstTime")

        when {
            isFirstTime -> performFirstTimeSync(userId)
            pullForSpecific != null -> pushAndPullForSpecific(userId, pullForSpecific)
            else -> performRegularSync()
        }

        Log.d("QuestSyncWorker", "Quest sync completed successfully")
    }

    private suspend fun performFirstTimeSync(userId: String) {
        val remoteQuests = Supabase.supabase
            .postgrest["quests"]
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<CommonQuestInfo>()

        remoteQuests.forEach { quest ->
            try {
                questRepository.upsertQuest(quest.copy(synced = true))
            } catch (e: Exception) {
                Log.e("QuestSyncWorker", "First-time sync failed $quest", e)
            }
        }

        Log.d("QuestSyncWorker", "First time sync completed, synced ${remoteQuests.size} quests")
    }

    private suspend fun performRegularSync() {
        val unSyncedQuests = questRepository.getUnSyncedQuests().first()

        unSyncedQuests.forEach { quest ->
            try {
                Supabase.supabase.postgrest["quests"].upsert(quest)
                questRepository.markAsSynced(quest.id)
            } catch (e: Exception) {
                Log.e("QuestSyncWorker", "Regular sync failed $quest", e)
            }
        }

        Log.d("QuestSyncWorker", "Regular sync completed, synced ${unSyncedQuests.size} quests")
    }

    private suspend fun pushAndPullForSpecific(userId: String, questId: String) {
        val localQuest = questRepository.getQuestById(questId)
        val remoteQuest = Supabase.supabase
            .postgrest["quests"]
            .select {
                filter {
                    eq("user_id", userId)
                    eq("id", questId)
                }
            }
            .decodeSingleOrNull<CommonQuestInfo>()

        when {
            localQuest != null && remoteQuest == null -> Supabase.supabase.postgrest["quests"].upsert(localQuest)
            localQuest == null && remoteQuest != null -> questRepository.upsertQuest(remoteQuest)
            localQuest != null && remoteQuest != null -> {
                if (localQuest.last_updated > remoteQuest.last_updated) {
                    Supabase.supabase.postgrest["quests"].upsert(localQuest)
                } else {
                    questRepository.upsertQuest(remoteQuest)
                }
            }
        }

        if (localQuest != null) questRepository.markAsSynced(questId)
    }

    private fun sendSyncBroadcast(status: SyncStatus) {
        val intent = android.content.Intent("launcher.launcher.quest_sync")
        intent.putExtra("status", status.ordinal)
        applicationContext.sendBroadcast(intent)
    }
}

private fun createForegroundInfo(context: Context): ForegroundInfo {
    val channelId = "quest_sync_channel"
    val channelName = "Quest Sync"

    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_LOW
    )
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Syncing quests")
        .setContentText("Quest data is being synced with server…")
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(70, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
        ForegroundInfo(70, notification)
    }

}
