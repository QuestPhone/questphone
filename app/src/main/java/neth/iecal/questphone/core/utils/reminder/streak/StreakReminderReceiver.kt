package neth.iecal.questphone.core.utils.reminder.streak

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import neth.iecal.questphone.app.screens.game.handleStreakFreezers
import neth.iecal.questphone.core.utils.reminder.HiltBroadcastReceiver
import neth.iecal.questphone.core.utils.scheduleDailyNotification
import neth.iecal.questphone.backed.repositories.UserRepository
import javax.inject.Inject

@AndroidEntryPoint(BroadcastReceiver::class)
class StreakReminderReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var userRepository: UserRepository

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (userRepository.userInfo.streak.currentStreak != 0) {
            val daysSince = userRepository.checkIfStreakFailed()
            if(daysSince!=null){
                handleStreakFreezers(userRepository.tryUsingStreakFreezers(daysSince))
            }

        }
        generateStreakReminder(userRepository, context)

        // re-schedule for next day
        scheduleDailyNotification(context, 9, 0) // set your fixed time
    }
}
