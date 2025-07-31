package neth.iecal.questphone.ui.screens.launcher

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import neth.iecal.questphone.core.utils.getCurrentDate
import neth.iecal.questphone.core.utils.getCurrentDay
import neth.iecal.questphone.core.utils.getCurrentTime12Hr
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.data.MeshStyles
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.data.repositories.QuestRepository
import neth.iecal.questphone.data.repositories.StatsRepository
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    application: Application,
    private val questRepository: QuestRepository,
    private val statsRepository: StatsRepository
) : AndroidViewModel(application) {

    private val rawQuestList: StateFlow<List<CommonQuestInfo>> =
        questRepository.getAllQuests()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    private val _questList = mutableStateListOf<CommonQuestInfo>()
    val questList: List<CommonQuestInfo> = _questList

    val completedQuests = SnapshotStateList<String>()
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
            rawQuestList.collect {
                filterQuests()
            }
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


    private suspend fun loadStats() {

        val statsList: List<StatsInfo> =
            statsRepository.getAllUnSyncedStats()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                ).first()


        statsList.forEach {
            val prevList = (successfulDates[it.date]?: emptyList()).toMutableList()
            prevList.add(it.quest_id)
            successfulDates[it.date] = prevList
        }
    }

    fun filterQuests() {
        val today = getCurrentDay()
        val filtered = rawQuestList.value.filter { !it.is_destroyed && it.selected_days.contains(today) }

        // Mark completed
        filtered.forEach {
            if (it.last_completed_on == getCurrentDate()) {
                completedQuests.add(it.id)
            }
        }

        val uncompleted = filtered.filter { it.id !in completedQuests }
        val completed = filtered.filter { it.id in completedQuests }

        val merged = (uncompleted + completed).sortedBy { QuestHelper.isInTimeRange(it) }

        _questList.clear()
        _questList.addAll(if (merged.size >= 4) merged.take(4) else merged)
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
