package neth.iecal.questphone.core.utils.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import neth.iecal.questphone.app.screens.game.handleStreakFreezers
import neth.iecal.questphone.backed.repositories.UserRepository
import javax.inject.Inject

class NewDayReceiver : BroadcastReceiver() {
    @Inject lateinit var userRepository: UserRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_DATE_CHANGED) {
            Log.d("New Day","Date changed")
            // Todo : fix
//            CoroutineScope(Dispatchers.Default).launch {
//                val lastStreak = userRepository.userInfo.streak.currentStreak
//                handleDayChange()
//                if(RewardDialogInfo.currentDialog == DialogState.STREAK_FAILED){
//                    sendDateChangedNotification(context,"You lost your $lastStreak day streak","Don't worry, we know you can rise again")
//                }
//                if(RewardDialogInfo.currentDialog == DialogState.STREAK_FREEZER_USED){
//                    sendDateChangedNotification(context,"Streak Freezers Used","We used ${RewardDialogInfo.streakFreezerReturn?.streakFreezersUsed} to save you $lastStreak day streak!")
//                }
//            }

        }
    }

    fun handleDayChange(){
        if (userRepository.userInfo.streak.currentStreak != 0) {
            val daysSince = userRepository.checkIfStreakFailed()
            if(daysSince!=null){
                handleStreakFreezers(userRepository.tryUsingStreakFreezers(daysSince))
            }

        }
    }

    private fun sendDateChangedNotification(context: Context,msg: String,title: String) {
        val channelId = "streakInfo"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Streak Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifies about streaks"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(neth.iecal.questphone.R.drawable.streak)
            .setContentTitle(title)
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }
}
