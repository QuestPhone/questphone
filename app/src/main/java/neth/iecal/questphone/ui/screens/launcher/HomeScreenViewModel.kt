package neth.iecal.questphone.ui.screens.launcher

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.ui.screens.game.handleStreakFreezers
import neth.iecal.questphone.ui.screens.game.showStreakUpDialog
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.StatsInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getCurrentDay
import nethical.questphone.core.core.utils.getCurrentTime12Hr
import nethical.questphone.data.MeshStyles
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    application: Application,
    questRepository: QuestRepository,
    private val statsRepository: StatsRepository,
    private val userRepository: UserRepository,
) : AndroidViewModel(application){

    val coins = userRepository.coins
    val currentStreak = userRepository.currentStreak

    val rawQuestList = questRepository.getAllQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), emptyList())


    val questList = MutableStateFlow<List<CommonQuestInfo>>(emptyList())
    val completedQuests = MutableStateFlow<List<String>>(emptyList())

    val shortcuts = mutableStateListOf<String>()
    val tempShortcuts = mutableStateListOf<String>()

    private val _time = mutableStateOf(getCurrentTime12Hr())
    val time = _time


    private val _meshStyle = MutableStateFlow(MeshStyles.ASYMMETRICAL)
    val meshStyle: StateFlow<MeshStyles> = _meshStyle

    val successfulDates = mutableStateMapOf<LocalDate, List<String>>()

    private val meshStylesp = application.applicationContext.getSharedPreferences("mesh_style", MODE_PRIVATE)
    private val shortcutsSp = application.applicationContext.getSharedPreferences("shortcuts", MODE_PRIVATE)


    init {

        viewModelScope.launch {
            if (userRepository.userInfo.streak.currentStreak != 0) {
                val daysSince = userRepository.checkIfStreakFailed()
                if(daysSince!=null){
                    handleStreakFreezers(userRepository.tryUsingStreakFreezers(daysSince))
                }

            }

            questRepository.getAllQuests()
                .onEach { rawQuestList ->
                   filterQuests()
                }
                .launchIn(viewModelScope)

            meshStyle.collect {
                if(it== MeshStyles.USER_STATS_HEATMAP){
                    loadStats()
                }
            }
            loadSavedConfigs()
            // Keep updating time every minute
            while (true) {
                _time.value = getCurrentTime12Hr()
                val delayMillis = 60_000 - (System.currentTimeMillis() % 60_000)
                delay(delayMillis)
            }
        }
    }

    private fun loadSavedConfigs() {
        // Load mesh style from SharedPreferences
        val meshStyleOrd = meshStylesp.getInt("mesh_style", meshStyle.value.ordinal)
        _meshStyle.value = MeshStyles.entries.toTypedArray()[meshStyleOrd]

        // Load shortcuts
        shortcuts.addAll(shortcutsSp.getStringSet("shortcuts", setOf())?.toList() ?: listOf())
        tempShortcuts.addAll(shortcuts)

    }

    fun filterQuests(){
        Log.d("HomeScreenViewModel", "quest list state changed")
        val today = getCurrentDay()
        val filtered = rawQuestList.value.filter {
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

        if (completed.size == rawQuestList.value.size) {
            if (userRepository.continueStreak()) {
                showStreakUpDialog()
            }
        }
        questList.value = if (merged.size >= 4) merged.take(4) else merged
        completedQuests.value = tempCompletedList.toList()
    }

    private suspend fun loadStats() {

        val statsList: List<StatsInfo> =
            statsRepository.getAllUnSyncedStats()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList<StatsInfo>()
                ).first()


        statsList.forEach {
            val prevList = (successfulDates[it.date]?: emptyList()).toMutableList()
            prevList.add(it.quest_id)
            successfulDates[it.date] = prevList
        }
    }

    fun toggleMeshStyle() {
        val options = MeshStyles.entries.filter { it != meshStyle.value }
        _meshStyle.value = options.random()
        meshStylesp.edit { putInt("mesh_style", meshStyle.value.ordinal) }
    }

    fun saveShortcuts() {
        shortcutsSp.edit(commit = true) {
            putStringSet("shortcuts", tempShortcuts.toSet())
        }
        shortcuts.clear()
        shortcuts.addAll(tempShortcuts)
    }

}
