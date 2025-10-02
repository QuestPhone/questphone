package neth.iecal.questphone.backed.repositories

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import neth.iecal.questphone.data.StatsInfo
import neth.iecal.questphone.data.StatsInfoDao
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StatsRepositoryEntryPoint {
    fun statsRepository(): StatsRepository
}

@Singleton
class StatsRepository @Inject constructor(
    private val statsInfoDao: StatsInfoDao
) {
    suspend fun upsertStats(statsInfo: StatsInfo) {
        statsInfoDao.upsertStats(statsInfo)
    }

    suspend fun updateStats(statsInfo: StatsInfo) {
        statsInfoDao.updateStats(statsInfo)
    }

    suspend fun deleteAll() {
        statsInfoDao.deleteAll()
    }

    fun getStatsByQuestId(id: String): Flow<List<StatsInfo>> {
        return statsInfoDao.getStatsByQuestId(id)
    }

    suspend fun getStatsForUserOnDate(date: LocalDate): StatsInfo? {
        return statsInfoDao.getStatsForUserOnDate(date)
    }

    fun getAllStatsForUser(): Flow<List<StatsInfo>> {
        return statsInfoDao.getAllStatsForUser()
    }

    suspend fun deleteStatsById(id: String) {
        statsInfoDao.deleteStatsById(id)
    }

    suspend fun deleteAllStatsForUser(userId: String) {
        statsInfoDao.deleteAllStatsForUser(userId)
    }

    fun getAllUnSyncedStats(): Flow<List<StatsInfo>> {
        return statsInfoDao.getAllUnSyncedStats()
    }

    suspend fun markAsSynced(id: String) {
        statsInfoDao.markAsSynced(id)
    }
    suspend fun getLastStatDate(): LocalDate? {
        return getAllStatsForUser().first()
            .maxByOrNull { it.date }
            ?.date
    }
}
