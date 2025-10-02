package neth.iecal.questphone.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import javax.inject.Singleton

@OptIn(kotlin.time.ExperimentalTime::class)
@Entity
@Serializable
@TypeConverters(StatsConverter::class)
data class StatsInfo(
    @PrimaryKey
    val id: String,
    val quest_id: String,
    val user_id: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    @Transient
    val isSynced: Boolean = false
)

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}

object StatsConverter {
    @TypeConverter
    fun fromDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toDate(dateString: String): LocalDate = LocalDate.parse(dateString)
}


@Dao
interface StatsInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(statsInfo: StatsInfo)
    @Update
    suspend fun updateStats(statsInfo: StatsInfo)

    @Query("DELETE FROM StatsInfo")
    suspend fun deleteAll()

    @Query("SELECT * FROM StatsInfo WHERE quest_id = :id")
    fun getStatsByQuestId(id: String): Flow<List<StatsInfo>>

    @Query("SELECT * FROM StatsInfo WHERE date = :date LIMIT 1")
    suspend fun getStatsForUserOnDate( date: LocalDate): StatsInfo?

    @Query("SELECT * FROM StatsInfo")
    fun getAllStatsForUser(): Flow<List<StatsInfo>>

    @Query("DELETE FROM StatsInfo WHERE id = :id")
    suspend fun deleteStatsById(id: String)

    @Query("DELETE FROM StatsInfo WHERE user_id = :userId")
    suspend fun deleteAllStatsForUser(userId: String)

    @Query("SELECT * FROM StatsInfo WHERE isSynced = 0 ORDER BY date DESC")
    fun getAllUnSyncedStats() : Flow<List<StatsInfo>>


    @Query("UPDATE StatsInfo SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}



@Database(
    entities = [StatsInfo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StatsConverter::class)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsInfoDao
}

@Module
@InstallIn(SingletonComponent::class)
object StatsDatabaseModule {

    @Provides
    @Singleton
    fun provideStatsDatabase(@ApplicationContext context: Context): StatsDatabase {
        return Room.databaseBuilder(
            context,
            StatsDatabase::class.java,
            "stats_info_database"
        ).build()
    }

    @Provides
    fun provideStatsInfoDao(db: StatsDatabase): StatsInfoDao {
        return db.statsDao()
    }
}
