package neth.iecal.questphone.app.screens.quest.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.screens.game.rewardUserForQuestCompl
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.StatsInfo
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.data.game.InventoryItem
import java.util.UUID

open class ViewQuestVM(
    protected val questRepository: QuestRepository,
    protected val userRepository: UserRepository,
    protected val statsRepository: StatsRepository, application: Application,
): AndroidViewModel(application) {
    lateinit var commonQuestInfo: CommonQuestInfo

    val progress = MutableStateFlow(0f)
    val isInTimeRange = MutableStateFlow(false)
    val isTimeOver = MutableStateFlow(false)
    val isQuestComplete = MutableStateFlow(false)
    val coins = userRepository.coinsState
    val level = userRepository.userInfo.level
    val activeBoosts = userRepository.activeBoostsState
    val isQuestSkippedDialogVisible = MutableStateFlow(false)
    fun setCommonQuest(commonQuestInfo: CommonQuestInfo){
        this.commonQuestInfo = commonQuestInfo
        isInTimeRange.value = QuestHelper.isInTimeRange(commonQuestInfo)
        isTimeOver.value = QuestHelper.isTimeOver(commonQuestInfo)
        isQuestComplete.value = commonQuestInfo.last_completed_on == getCurrentDate()
        progress.value = if(isQuestComplete.value) 1f else 0f
    }

    fun saveMarkedQuestToDb(){
        progress.value = 1f
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()
        updateQuestInDb(commonQuestInfo)
        rewardUserForQuestCompl(commonQuestInfo)
        isQuestComplete.value = true
    }
    private fun updateQuestInDb(commonQuestInfo: CommonQuestInfo){
        viewModelScope.launch {
            questRepository.upsertQuest(commonQuestInfo)
            statsRepository.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = userRepository.getUserId()
                )
            )

        }
    }
    fun useItem(inventoryItem: InventoryItem,onUsed:()->Unit){
        userRepository.deductFromInventory(inventoryItem)
        onUsed()
    }
    fun getInventoryItemCount(item: InventoryItem): Int {
        return userRepository.getInventoryItemCount(item)
    }
    fun isBoosterActive(item: InventoryItem): Boolean{
        return userRepository.isBoosterActive(item)
    }

}
