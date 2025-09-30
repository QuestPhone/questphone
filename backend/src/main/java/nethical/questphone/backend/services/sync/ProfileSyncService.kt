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
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.R
import nethical.questphone.data.SyncStatus
import nethical.questphone.data.UserInfo
import javax.inject.Inject

//Todo: Convert to worker
@AndroidEntryPoint(Service::class)
class ProfileSyncService : Service() {

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var syncJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1041
        private const val CHANNEL_ID = "sync_channel"
        private const val EXTRA_IS_FIRST_TIME = "is_first_time"

        fun start(context: Context,isFirstLoginPull : Boolean ) {
            val intent = Intent(context, ProfileSyncService::class.java)
                .apply {
                    putExtra(EXTRA_IS_FIRST_TIME, isFirstLoginPull)
                }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ProfileSyncService::class.java)
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
                Log.e("ProfileSyncService", "Sync failed", e)
            } finally {
                stopSelf()
            }
        }

        return START_NOT_STICKY // Don't restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
    }


    private suspend fun performSync(isFirstTimeSync: Boolean) {
        try {

            val sp = getSharedPreferences("authtoken", Context.MODE_PRIVATE)
            var userId = sp.getString("key",null)
            if (userId == null) {
                Log.w("ProfileSyncService", "No user logged in, stopping sync")
                return
            }
            Log.d("ProfileSyncService", "Starting sync for $userId")

            sendSyncBroadcast(SyncStatus.ONGOING)

            if (userRepository.userInfo.needsSync) {
                val profileRemote = Supabase.supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserInfo>()

                if (profileRemote != null) {
                    if (profileRemote.last_updated > userRepository.userInfo.last_updated || isFirstTimeSync) {
                        userRepository.userInfo = profileRemote
                    }else{
                        Supabase.supabase.postgrest["profiles"].upsert(
                            userRepository.userInfo
                        )

                        userRepository.userInfo.needsSync = false
                        userRepository.saveUserInfo(false)
                    }
                }
            }

            sendSyncBroadcast(SyncStatus.OVER)
            Log.d("ProfileSyncService", "Sync completed successfully")

        } catch (e: Exception) {
            Log.e("ProfileSyncService", "Sync error: ${e.stackTraceToString()}")
            sendSyncBroadcast(SyncStatus.OVER)
            throw e
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Profile Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows sync progress for user profile"
            setShowBadge(false)
        }

        manager.createNotificationChannel(channel)
    }

    private fun createSyncNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Profile Sync")
        .setContentText("Syncing your profile data...")
        .setSmallIcon(R.drawable.baseline_info_24)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setAutoCancel(false)
        .build()

    private fun sendSyncBroadcast(status: SyncStatus) {
        val intent = Intent("launcher.launcher.profile_sync")
        intent.putExtra("status", status.ordinal)
        sendBroadcast(intent)
    }
}