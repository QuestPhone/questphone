package nethical.questphone.backend.services.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.core.R
import nethical.questphone.data.SyncStatus
import javax.inject.Inject

//Todo: Convert to worker
@AndroidEntryPoint(Service::class)
class QuestSyncService : Service() {

    @Inject
    lateinit var questRepository: QuestRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1043
        private const val CHANNEL_ID = "quest_sync_channel"
         const val EXTRA_IS_FIRST_TIME = "is_first_time"

        fun stop(context: Context) {
            val intent = Intent(context, QuestSyncService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isFirstTimeSync = intent?.getBooleanExtra(EXTRA_IS_FIRST_TIME, false) ?: false

        startForeground(NOTIFICATION_ID, createSyncNotification())

        // Cancel any existing sync job
        syncJob?.cancel()

        // Start sync process
        syncJob = serviceScope.launch {
            try {
                performSync(isFirstTimeSync)
            } catch (e: Exception) {
                Log.e("QuestSyncService", "Sync failed", e)
                sendSyncBroadcast(SyncStatus.OVER)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
    }

    private suspend fun performSync(isFirstTimeSync: Boolean) {
        try {
            val userId = Supabase.supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                Log.w("QuestSyncService", "No user logged in, stopping sync")
                return
            }

            Log.d("QuestSyncService", "Starting quest sync for $userId, firstTime: $isFirstTimeSync")
            sendSyncBroadcast(SyncStatus.ONGOING)

            if (isFirstTimeSync) {
                performFirstTimeSync(userId)
            } else {
                performRegularSync()
            }

            sendSyncBroadcast(SyncStatus.ONGOING)
            Log.d("QuestSyncService", "Quest sync completed successfully")

        } catch (e: Exception) {
            Log.e("QuestSyncService", "Quest sync error: ${e.stackTraceToString()}")
            throw e
        }
    }

    private suspend fun performFirstTimeSync(userId: String) {
        val remoteQuests = Supabase.supabase
            .postgrest["quests"]
            .select()
            {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<CommonQuestInfo>()

        remoteQuests.forEach { quest ->
            questRepository.upsertQuest(quest.copy(synced = true))
        }

        Log.d("QuestSyncService", "First time sync completed, synced ${remoteQuests.size} quests")
    }

    private suspend fun performRegularSync() {
        val unSyncedQuests = questRepository.getUnSyncedQuests().first()

        unSyncedQuests.forEach { quest ->
            Supabase.supabase.postgrest["quests"].upsert(quest)
            questRepository.markAsSynced(quest.id)
        }

        Log.d("QuestSyncService", "Regular sync completed, synced ${unSyncedQuests.size} quests")
    }

    // Commented out advanced sync logic for future use
    /*
    private suspend fun performAdvancedSync(userId: String) {
        val localQuests = questRepository.getAllQuests().first() // not just unsynced
        val remoteQuests = Supabase.supabase
            .postgrest["quests"]
            .select()
            {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<CommonQuestInfo>()

        val localMap = localQuests.associateBy { it.id }
        val remoteMap = remoteQuests.associateBy { it.id }

        // Merge both directions
        val allIds = (localMap.keys + remoteMap.keys)

        for (id in allIds) {
            val local = localMap[id]
            val remote = remoteMap[id]

            when {
                local != null && remote == null -> {
                    // New local quest not on server yet
                    Supabase.supabase.postgrest["quests"].upsert(local)
                }

                local == null && remote != null -> {
                    // Remote quest not in local DB
                    questRepository.upsertQuest(remote)
                }

                local != null && remote != null -> {
                    // Compare timestamps
                    when {
                        local.last_updated > remote.last_updated -> {
                            Supabase.supabase.postgrest["quests"].upsert(local)
                        }

                        remote.last_updated > local.last_updated -> {
                            questRepository.upsertQuest(remote)
                        }
                    }
                }
            }

            if (local != null) questRepository.markAsSynced(id)
        }
    }
    */

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Quest Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows sync progress for quest data"
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    private fun createSyncNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Quest Sync")
        .setContentText("Your quests are syncing...")
        .setSmallIcon(R.drawable.baseline_info_24)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setAutoCancel(false)
        .build()

    private fun sendSyncBroadcast(status: SyncStatus) {
        val intent = Intent("launcher.launcher.quest_sync")
        intent.putExtra("status", status.ordinal)
        sendBroadcast(intent)
    }
}