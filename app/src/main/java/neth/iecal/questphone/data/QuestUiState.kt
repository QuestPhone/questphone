package neth.iecal.questphone.data

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.DayOfWeek
import nethical.questphone.data.json
import java.util.UUID

/**
 * Holds [CommonQuestInfo] as a state object
 */
@Stable
class QuestInfoState(
    initialTitle: String = "",
    initialInstructions: String = "",
    initialReward: Int = 5,
    initialIntegrationId: BaseIntegrationId = BaseIntegrationId.DEEP_FOCUS,
    initialSelectedDays: Set<DayOfWeek> = emptySet(),
    initialAutoDestruct: String = "9999-12-31",
    initialTimeRange: List<Int> = listOf(0,24),
    isHardLock: Boolean = false
) {
    var id = UUID.randomUUID().toString()
    var title by mutableStateOf(initialTitle)
    var reward by mutableIntStateOf(initialReward)
    var integrationId by mutableStateOf(initialIntegrationId)
    var selectedDays by mutableStateOf(initialSelectedDays)
    var instructions by mutableStateOf(initialInstructions)
    var initialAutoDestruct by mutableStateOf(initialAutoDestruct)
    var initialTimeRange by mutableStateOf(initialTimeRange)
    var isHardLock by mutableStateOf(isHardLock)
    inline fun < reified T : Any> toBaseQuest(questInfo: T? = null) = CommonQuestInfo(
        id = id,
        title = title,
        reward = reward,
        integration_id = integrationId,
        selected_days = selectedDays,
        auto_destruct = initialAutoDestruct,
        time_range = initialTimeRange,
        instructions = instructions,
        quest_json = if(questInfo!=null) json.encodeToString(questInfo) else "",
        isHardLock = isHardLock
    )
    fun fromBaseQuest(commonQuestInfo: CommonQuestInfo){
        id = commonQuestInfo.id
        title = commonQuestInfo.title
        reward = commonQuestInfo.reward
        integrationId = commonQuestInfo.integration_id
        selectedDays = commonQuestInfo.selected_days
        initialAutoDestruct = commonQuestInfo.auto_destruct
        instructions = commonQuestInfo.instructions
        initialTimeRange = commonQuestInfo.time_range
        isHardLock = commonQuestInfo.isHardLock
    }
}
