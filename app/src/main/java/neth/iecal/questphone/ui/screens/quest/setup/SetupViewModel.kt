package neth.iecal.questphone.ui.screens.quest.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.data.BaseIntegrationId

open class SetupViewModel(
    protected val questRepository: QuestRepository
): ViewModel() {
    val isReviewDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val questInfoState = MutableStateFlow(QuestInfoState())

    fun getBaseQuestInfo(): CommonQuestInfo{
        return questInfoState.value.toBaseQuest(null)
    }

    suspend fun loadQuestData(id:String?,integrationId: BaseIntegrationId,onQuestLoaded:(CommonQuestInfo) -> Unit = {}){
        val quest = questRepository.getQuestById(id.toString())
        questInfoState.value.fromBaseQuest(quest ?: CommonQuestInfo(integration_id = integrationId))
    }
    fun addQuestToDb(onSuccess: ()-> Unit){
        viewModelScope.launch {
            val baseQuest = getBaseQuestInfo()
            questRepository.upsertQuest(baseQuest)
            isReviewDialogVisible.value = false
            onSuccess()
        }
    }
}