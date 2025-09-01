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



    private val shortcutsSp = application.applicationContext.getSharedPreferences("shortcuts", MODE_PRIVATE)


    val showDonationsDialog = MutableStateFlow(false)
    val donationSp = application.getSharedPreferences("shows", MODE_PRIVATE)

    init {
        scheduleDailyNotification(application,9,0)
        viewModelScope.launch {
            loadSavedConfigs()
            // Keep updating time every minute
            while (true) {
                _time.value = getCurrentTime12Hr()
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

    private fun loadSavedConfigs() {
        // Load shortcuts
        shortcuts.addAll(shortcutsSp.getStringSet("shortcuts", setOf())?.toList() ?: listOf())
        tempShortcuts.addAll(shortcuts)

    }

    fun getHomeWidget(): @Composable ((Modifier) -> Unit)? {
        return homeWidgets[userRepository.userInfo.customization_info.equippedWidget]
    }

    suspend fun filterQuests(){
        Log.d("HomeScreenViewModel", "quest list state changed")

        // we reload the list from disk cause android triggers the function twice, once with an empty list initially.
        // we cannot ignore empty lists cuz some dates have no quests so in those cases, we cannot trigger
        // the function that checks streaks for those days
        var rawQuestsListLocal = questRepository.getAllQuests().first()
        val today = getCurrentDay()
        val filtered = rawQuestsListLocal.filter {
            !it.is_destroyed && it.selected_days.contains(today)
        }
        // Mark completed
        val tempCompletedList = mutableListOf<String>()
        filtered.forEach {
            if (it.last_completed_on == getCurrentDate()) {
                tempCompletedList.add(it.id)
            }
        }

        val uncompleted = filtered.filter { it.id !in tempCompletedList }
        val completed = filtered.filter { it.id in tempCompletedList }

        val merged =
            (uncompleted + completed).sortedBy { QuestHelper.isInTimeRange(it) }

        if (completed.size == filtered.size) {
            if (userRepository.continueStreak()) {
                showStreakUpDialog()
            }
        }
        questList.value = if (merged.size >= 4) merged.take(4) else merged
        completedQuests.value = tempCompletedList.toList()
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

}
