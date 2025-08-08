package neth.iecal.questphone.app.screens.quest.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.BaseIntegrationId

open class SetupViewModel(
    protected val questRepository: QuestRepository,
    protected val userRepository: UserRepository
): ViewModel() {
    val isReviewDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val questInfoState = MutableStateFlow(QuestInfoState())

    fun getBaseQuestInfo(): CommonQuestInfo{
        return questInfoState.value.toBaseQuest(null)
    }

    val userCreatedOn = userRepository.userInfo.getCreatedOnString()

    suspend fun loadQuestData(id:String?,integrationId: BaseIntegrationId,onQuestLoaded:(CommonQuestInfo) -> Unit = {}){
        val quest = questRepository.getQuestById(id.toString())
        questInfoState.value.fromBaseQuest(quest ?: CommonQuestInfo(integration_id = integrationId))
    }
    fun addQuestToDb(json: String, onSuccess: ()-> Unit){
        viewModelScope.launch {
            val baseQuest = getBaseQuestInfo()
            baseQuest.quest_json = json
            Log.d("Setup Quest","Added quest ${nethical.questphone.data.json.encodeToString(baseQuest)} ")
            questRepository.upsertQuest(baseQuest)
            isReviewDialogVisible.value = false
            onSuccess()
        }
    }
}