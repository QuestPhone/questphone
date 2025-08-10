package nethical.questphone.backend.services.sync

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import nethical.questphone.backend.StatsInfo
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.R
import nethical.questphone.core.core.utils.calculateMonthsPassedAndRoundedStart
import nethical.questphone.data.SyncStatus
import javax.inject.Inject

//Todo: Convert to worker
@AndroidEntryPoint(Service::class)
class StatsSyncService : Service() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var statsRepository: StatsRepository

    @Inject
    lateinit var questRepository: QuestRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val receiverScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null


    private var syncReceiver: BroadcastReceiver? = null
    private var syncCompletionCallback: (() -> Unit)? = null

    private fun waitForSyncCompletion(callback: () -> Unit) {
        syncCompletionCallback = callback
    }

    private fun cleanup() {
        syncReceiver?.let {
            unregisterReceiver(it)
            syncReceiver = null
        }
        syncCompletionCallback = null
    }

    companion object {
        private const val NOTIFICATION_ID = 1044
        private const val CHANNEL_ID = "stats_sync_channel"
        private const val EXTRA_IS_FIRST_TIME = "is_first_time"

        fun start(context: Context, isFirstTime: Boolean = false) {
            val intent = Intent(context, StatsSyncService::class.java).apply {
                putExtra(EXTRA_IS_FIRST_TIME, isFirstTime)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, StatsSyncService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        syncReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getIntExtra("status", -1) ?: -1
                val syncStatus = SyncStatus.entries.getOrNull(status)

                when (syncStatus) {
                    SyncStatus.ONGOING -> {
                        Log.d("WaitingService", "Sync completed successfully")
                        cleanup()
                    }
                    SyncStatus.OVER -> {
                        Log.e("WaitingService", "Sync Over")
                        cleanup()
                        syncCompletionCallback?.invoke()
                    }
                    null -> {}
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction("launcher.launcher.quest_sync")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(syncReceiver, filter)
        }
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
                Log.e("StatsSyncService", "Sync failed", e)
                sendSyncBroadcast(SyncStatus.ONGOING)
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
        cleanup()

    }

    private suspend fun performSync(isFirstTimeSync: Boolean) {
        try {
            val userId = Supabase.supabase.auth.currentUserOrNull()?.id
            if (userId == null) {
                Log.w("StatsSyncService", "No user logged in, stopping sync")
                return
            }

            Log.d("StatsSyncService", "Starting stats sync for $userId, firstTime: $isFirstTimeSync")
            sendSyncBroadcast(SyncStatus.ONGOING)

            if (isFirstTimeSync) {
                performFirstTimeSync(userId)
            } else {
                performRegularSync()
            }

            sendSyncBroadcast(SyncStatus.OVER)
            Log.d("StatsSyncService", "Stats sync completed successfully")

        } catch (e: Exception) {
            Log.e("StatsSyncService", "Stats sync error: ${e.stackTraceToString()}")
            throw e
        }
    }

    private suspend fun performFirstTimeSync(userId: String) {
        waitForSyncCompletion {
            receiverScope.launch {
                val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
                val startDate = calculateMonthsPassedAndRoundedStart(userRepository.userInfo.created_on)

                val start = startDate.toString()  // e.g., "2023-01-01"
                val end = today.toString()        // e.g., "2025-06-16"

                val stats = Supabase.supabase
                    .postgrest["quest_stats"]
                    .select {
                        filter {
                            eq("user_id", userId)
                            gte("date", start)
                            lte("date", end)
                        }
                    }
                    .decodeList<StatsInfo>()

                stats.forEach {
                    statsRepository.upsertStats(it.copy(isSynced = true))
                }

                Log.d("StatsSyncService", "First time sync completed, synced ${stats.size} stats")
            }
        }
    }

    private suspend fun performRegularSync() {
        val unSyncedStats = statsRepository.getAllUnSyncedStats().first()

        unSyncedStats.forEach { stat ->
            Supabase.supabase.postgrest["quest_stats"].upsert(stat)
            questRepository.markAsSynced(stat.id)
        }

        Log.d("StatsSyncService", "Regular sync completed, synced ${unSyncedStats.size} stats")
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stats Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows sync progress for quest statistics"
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    private fun createSyncNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Stats Sync")
        .setContentText("Your stats are syncing...")
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