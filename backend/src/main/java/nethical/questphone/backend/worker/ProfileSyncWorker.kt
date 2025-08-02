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
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.R
import nethical.questphone.data.SyncStatus
import nethical.questphone.data.game.UserInfo

@HiltWorker
class ProfileSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userRepository: UserRepository // this is injected!
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {

            val userId = Supabase.supabase.auth.currentUserOrNull()?.id ?: return Result.success()

            Log.d("ProfileSyncManager", "Starting sync for $userId")
            showSyncNotification(applicationContext)
            sendSyncBroadcast(applicationContext, SyncStatus.ONGOING)

            if(userRepository.userInfo.needsSync){
                val profileRemote = Supabase.supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<UserInfo>()

                if (profileRemote != null) {
                    if (profileRemote.last_updated  > userRepository.userInfo.last_updated){
                        userRepository.userInfo = profileRemote
                    }
                }
                Supabase.supabase.postgrest["profiles"].upsert(
                    userRepository.userInfo
                )
                userRepository.userInfo.needsSync = false
                userRepository.saveUserInfo(false)
            }


            val manager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(1041)
            return Result.success()
        } catch (e: Exception){
            Log.e("SyncError",e.stackTraceToString())
            return Result.failure()
        }
    }
}

private fun sendSyncBroadcast(context: Context,msg: SyncStatus) {
    val intent = Intent("launcher.launcher.profile_sync")
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
        .setContentText("Your profile is syncing...")
        .setSmallIcon(R.drawable.baseline_info_24)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    manager.notify(1041, notification)
}

