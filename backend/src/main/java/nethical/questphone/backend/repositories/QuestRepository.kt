package nethical.questphone.backend.repositories

import kotlinx.coroutines.flow.Flow
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.QuestDao
import javax.inject.Inject

class QuestRepository @Inject constructor(
    private val questDao: QuestDao
){
    suspend fun upsertQuest(quest: CommonQuestInfo) {
        questDao.upsertQuest(quest)
    }

    suspend fun upsertAll(quests: List<CommonQuestInfo>) {
        questDao.upsertAll(quests)
    }

    suspend fun deleteAll() {
        questDao.deleteAll()
    }

    suspend fun clearAll() {
        questDao.clearAll()
    }

    suspend fun getQuest(title: String): CommonQuestInfo? {
        return questDao.getQuest(title)
    }

    suspend fun getQuestById(id: String): CommonQuestInfo? {
        return questDao.getQuestById(id)
    }

    fun getAllQuests(): Flow<List<CommonQuestInfo>> {
        return questDao.getAllQuests()
    }

    fun getUnSyncedQuests(): Flow<List<CommonQuestInfo>> {
        return questDao.getUnSyncedQuests()
    }

    suspend fun deleteQuest(quest: CommonQuestInfo) {
        questDao.deleteQuest(quest)
    }

    suspend fun deleteQuestByTitle(title: String) {
        questDao.deleteQuestByTitle(title)
    }

    suspend fun markAsSynced(id: String) {
        questDao.markAsSynced(id)
    }

    suspend fun getRowCount(): Int {
        return questDao.getRowCount()
    }
}
