package neth.iecal.questphone.app.screens.quest.setup

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.data.BaseIntegrationId

open class QuestSetupViewModel(
    protected val questRepository: QuestRepository,
    protected val userRepository: UserRepository
): ViewModel() {
    val isReviewDialogVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val questInfoState = MutableStateFlow(QuestInfoState())

    fun getBaseQuestInfo(): CommonQuestInfo{
        return questInfoState.value.toBaseQuest(null)
    }

    val userCreatedOn = userRepository.userInfo.getCreatedOnString()

    /**
     * Sets value to the questinfo state that doesnt contain the questjson field
     */
    suspend fun loadQuestUpperData(id:String?, integrationId: BaseIntegrationId, onQuestLoaded:(CommonQuestInfo) -> Unit = {}){
        val quest = loadQuestData(id.toString())
        questInfoState.value.fromBaseQuest(quest ?: CommonQuestInfo(integration_id = integrationId))
    }
    fun loadQuestUpperData(commonQuestInfo: CommonQuestInfo){
        questInfoState.value.fromBaseQuest(commonQuestInfo)
    }

    /**
     * loads the [CommonQuestInfo] object with all fields
     */
    suspend fun loadQuestData(id:String?): CommonQuestInfo? {
        return questRepository.getQuestById(id.toString())
    }
    fun addQuestToDb(json: String,reward: Int = 5, onSuccess: ()-> Unit){
        viewModelScope.launch {
            val baseQuest = getBaseQuestInfo()
            baseQuest.quest_json = json
            baseQuest.reward = reward
            Log.d("Setup Quest","Added quest ${nethical.questphone.data.json.encodeToString(baseQuest)} ")
            questRepository.upsertQuest(baseQuest)
            isReviewDialogVisible.value = false
            onSuccess()
        }
    }
}