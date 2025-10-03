package neth.iecal.questphone.backed.repositories

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.QuestDao
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface QuestRepositoryEntryPoint {
    fun questRepository(): QuestRepository
}

@Singleton
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

    fun getHardLockedQuests(): Flow<List<CommonQuestInfo>> {
        return questDao.getHardLockQuests()
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
