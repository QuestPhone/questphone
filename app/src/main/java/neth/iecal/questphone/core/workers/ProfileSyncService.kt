package neth.iecal.questphone.core.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import neth.iecal.questphone.backed.repositories.UserRepositoryEntryPoint
import neth.iecal.questphone.core.Supabase
import nethical.questphone.data.SyncStatus
import nethical.questphone.data.UserInfo

class ProfileSyncWorker(val appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext,
    params
) {
    override suspend fun doWork(): Result {
        setForegroundAsync(createForegroundInfo(appContext))
        val isFirstTimeSync = inputData.getBoolean(EXTRA_IS_FIRST_TIME, false)

        return try {
            performSync(isFirstTimeSync)
            Log.d("ProfileSyncWorker", "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("ProfileSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(appContext)
    }
    private suspend fun performSync(isFirstTimeSync: Boolean) {

        val entryPoint = EntryPointAccessors.fromApplication(applicationContext, UserRepositoryEntryPoint::class.java)
        val userRepository = entryPoint.userRepository()

        val userId = userRepository.getUserId()
        Log.d("ProfileSyncWorker", "Starting sync for $userId")

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
                } else {
                    Supabase.supabase.postgrest["profiles"].upsert(userRepository.userInfo)
                    userRepository.userInfo.needsSync = false
                    userRepository.saveUserInfo(false)
                }
            }
        }

        sendSyncBroadcast(SyncStatus.OVER)
    }

    private fun sendSyncBroadcast(status: SyncStatus) {
        val intent = Intent("launcher.launcher.profile_sync")
        intent.putExtra("status", status.ordinal)
        applicationContext.sendBroadcast(intent)
    }

    companion object {
        const val EXTRA_IS_FIRST_TIME = "is_first_time"

        fun buildInputData(isFirstLoginPull: Boolean): Data {
            return Data.Builder()
                .putBoolean(EXTRA_IS_FIRST_TIME, isFirstLoginPull)
                .build()
        }
    }

}

private fun createForegroundInfo(context: Context): ForegroundInfo {
    val channelId = "profile_sync_channel"
    val channelName = "Profile Sync"

    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_LOW
    )
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Syncing profile")
        .setContentText("Profile data is being synced with server…")
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ForegroundInfo(69, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    } else {
        ForegroundInfo(69, notification)
    }
}

