package neth.iecal.questphone.app.screens.launcher

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import neth.iecal.questphone.app.screens.game.handleStreakFreezers
import neth.iecal.questphone.app.screens.game.showStreakUpDialog
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.core.utils.scheduleDailyNotification
import neth.iecal.questphone.homeWidgets
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getCurrentDay
import nethical.questphone.core.core.utils.getCurrentTime12Hr
import nethical.questphone.core.core.utils.getCurrentTime24Hr
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    application: Application,
    private val questRepository: QuestRepository,
    private val statsRepository: StatsRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application){

    val coins = userRepository.coinsState
    val currentStreak = userRepository.currentStreakState

    val questList = MutableStateFlow<List<CommonQuestInfo>>(emptyList())
    val completedQuests = MutableStateFlow<List<String>>(emptyList())

    val shortcuts = mutableStateListOf<String>()
    val tempShortcuts = mutableStateListOf<String>()

    private val _time = mutableStateOf(getCurrentTime12Hr())
    val time = _time

    private var is12HourClock = true
    private val shortcutsSp = application.applicationContext.getSharedPreferences("shortcuts", MODE_PRIVATE)


    val showDonationsDialog = MutableStateFlow(false)
    val donationSp = application.getSharedPreferences("shows", MODE_PRIVATE)
    val sp = application.getSharedPreferences("timeFormat",MODE_PRIVATE)

    init {
        scheduleDailyNotification(application,9,0)
        viewModelScope.launch {
            loadSavedConfigs()
            // Keep updating time every minute
            val sp = application.getSharedPreferences("timeFormat",MODE_PRIVATE)
            is12HourClock = sp.getBoolean("12hr",true)
            while (true) {
                reloadTime()
                val delayMillis = 60_000 - (System.currentTimeMillis() % 60_000)
                delay(delayMillis)
            }
        }
        val daysBeforeDonation = 3

        val createdOn: Instant = userRepository.userInfo.created_on
        val now = Clock.System.now()

        val createdDate = createdOn.toLocalDateTime(TimeZone.UTC).date
        val today = now.toLocalDateTime(TimeZone.UTC).date

        val daysSinceCreation = createdDate.until(today, DateTimeUnit.DAY)

        if (daysSinceCreation >= daysBeforeDonation) {
            showDonationsDialog.value = !donationSp.contains("shown")
        }

    }

    init {
        getFCMToken {
            if(it!=null && !userRepository.userInfo.fcm_tokens.contains(it)){
                userRepository.saveFcmToken(it)
            }
        }
    }
    private fun loadSavedConfigs() {
        // Load shortcuts
        shortcuts.addAll(shortcutsSp.getStringSet("shortcuts", setOf())?.toList() ?: listOf())
        tempShortcuts.addAll(shortcuts)

    }

    fun toggleTimeCLock(){
        is12HourClock = !is12HourClock
        sp.edit(commit = true) { putBoolean("12hr", is12HourClock) }
        reloadTime()
    }

    private fun reloadTime(){
        if(is12HourClock) {
            _time.value = getCurrentTime12Hr()
        }else{
            _time.value = getCurrentTime24Hr()
        }
    }
    fun getHomeWidget(): @Composable ((Modifier) -> Unit)? {
        return homeWidgets[userRepository.userInfo.customization_info.equippedWidget]
    }

    suspend fun filterQuests() {
        Log.d("HomeScreenViewModel", "quest list state changed")

        // Reload full list from repo
        val rawQuests = questRepository.getAllQuests().first()
        val today = getCurrentDay()
        val todayDate = getCurrentDate()

        // Step 1: filter valid quests for today
        val filtered = rawQuests.filter { quest ->
            !quest.is_destroyed && quest.selected_days.contains(today)
        }

        // Step 2: split into completed & uncompleted
        val completedIds = filtered
            .filter { it.last_completed_on == todayDate }
            .map { it.id }

        val uncompleted = filtered.filter { it.id !in completedIds }
        val completed = filtered.filter { it.id in completedIds }

        // Step 3: sort — uncompleted first, then completed
        val merged = (uncompleted + completed).sortedWith(
            compareByDescending<CommonQuestInfo> { QuestHelper.isInTimeRange(it) } // in-range first
                .thenBy { it.title }
        )

        // Step 4: handle streak continuation
        if (completed.size == filtered.size) {
            if (userRepository.continueStreak()) {
                showStreakUpDialog()
            }
        }

        // Step 5: update state
        questList.value = merged.take(4)
        completedQuests.value = completedIds
    }

    fun handleCheckStreakFailure(){
        if (userRepository.userInfo.streak.currentStreak != 0) {
            val daysSince = userRepository.checkIfStreakFailed()
            if(daysSince!=null){
                handleStreakFreezers(userRepository.tryUsingStreakFreezers(daysSince))
            }

        }
    }

    fun saveShortcuts() {
        shortcutsSp.edit(commit = true) {
            putStringSet("shortcuts", tempShortcuts.toSet())
        }
        shortcuts.clear()
        shortcuts.addAll(tempShortcuts)
    }

    fun hideDonationDialog(){
        showDonationsDialog.value = false
        donationSp.edit(commit = true) { putBoolean("shown", true) }
    }

    fun getFCMToken(onTokenReceived: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token: $token")
                onTokenReceived(token)
            } else {
                Log.e("FCM", "Failed to get token", task.exception)
                onTokenReceived(null)
            }
        }
    }

}
