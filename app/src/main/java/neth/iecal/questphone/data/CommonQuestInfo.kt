package neth.iecal.questphone.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.core.Supabase
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.DayOfWeek
import nethical.questphone.data.json
import java.util.UUID
import javax.inject.Singleton

/**
 * Stores information about quests which are common to all integration types
 *
 * @property title this should be unique as it also acts as a primary key
 * @property reward the coins rewarded for that quest
 * @property integration_id id
 * @property selected_days the days on which it can be performed
 * @property auto_destruct format yyyy-mm-dd
 * @property time_range format startHour,endHour, says when between what time range the quest is to be completed
 * @property created_on
 * @property quest_json stores additional integration specific information here
 */
@Entity
@Serializable
@TypeConverters(BaseQuestConverter::class)
data class CommonQuestInfo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = Supabase.supabase.auth.currentUserOrNull()?.id.toString(),
    var title: String = "",
    var reward: Int = 5,
    var integration_id : BaseIntegrationId = BaseIntegrationId.DEEP_FOCUS,
    var selected_days: Set<DayOfWeek> = emptySet(),
    var auto_destruct: String = "9999-12-31",
    var time_range: List<Int> = listOf(0,24),
    var created_on : String = getCurrentDate(),
    var last_completed_on: String = "0001-01-01",
    var instructions: String = "",
    var quest_json: String = "",
    var is_destroyed : Boolean = false,
    @Transient
    var synced: Boolean = false,
    var last_updated: Long = System.currentTimeMillis(),  // Epoch millis
    var isHardLock: Boolean = false
)


object BaseQuestConverter {

    @TypeConverter
    fun fromDayOfWeekSet(set: Set<DayOfWeek>): String = json.encodeToString(set.toList())

    @TypeConverter
    fun toDayOfWeekSet(jsonStr: String): Set<DayOfWeek> =
        json.decodeFromString<List<DayOfWeek>>(jsonStr).toSet()

    @TypeConverter
    fun fromTimeRange(range: List<Int>): String = json.encodeToString(range)

    @TypeConverter
    fun toTimeRange(jsonStr: String): List<Int> = json.decodeFromString(jsonStr)

    @TypeConverter
    fun fromIntegrationId(id: BaseIntegrationId): String = id.name

    @TypeConverter
    fun toIntegrationId(name: String): BaseIntegrationId = BaseIntegrationId.valueOf(name)
}

@Dao
interface QuestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuest(quest: CommonQuestInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quests: List<CommonQuestInfo>)

    @Query("DELETE FROM CommonQuestInfo")
    suspend fun deleteAll()

    @Query("SELECT * FROM CommonQuestInfo WHERE title = :title")
    suspend fun getQuest(title: String): CommonQuestInfo?


    @Query("SELECT * FROM CommonQuestInfo WHERE id = :id")
    suspend fun getQuestById(id: String): CommonQuestInfo?

    @Query("SELECT * FROM CommonQuestInfo")
    fun getAllQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo WHERE synced = 0")
    fun getUnSyncedQuests(): Flow<List<CommonQuestInfo>>

    @Query("SELECT * FROM CommonQuestInfo WHERE isHardLock = 1")
    fun getHardLockQuests(): Flow<List<CommonQuestInfo>>

    @Delete
    suspend fun deleteQuest(quest: CommonQuestInfo)

    @Query("DELETE FROM CommonQuestInfo WHERE title = :title")
    suspend fun deleteQuestByTitle(title: String)

    @Query("UPDATE CommonQuestInfo SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM CommonQuestInfo")
    suspend fun clearAll()


    @Query("SELECT COUNT(*) FROM CommonQuestInfo")
    suspend fun getRowCount(): Int

}

@Database(
    entities = [CommonQuestInfo::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(BaseQuestConverter::class)
abstract class QuestDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
}


@Module
@InstallIn(SingletonComponent::class)
object QuestModule {

    @Provides
    @Singleton
    fun provideQuestDatabase(@ApplicationContext context: Context): QuestDatabase {
        return Room.databaseBuilder(
                context,
                QuestDatabase::class.java,
                "quest_database"
            ).fallbackToDestructiveMigration(true).build()
    }

    @Singleton
    @Provides
    fun provideQuestDao(db: QuestDatabase): QuestDao {
        return db.questDao()
    }

    @Provides
    @Singleton
    fun provideQuestRepository(dao: QuestDao): QuestRepository {
        return QuestRepository(dao)
    }
}

