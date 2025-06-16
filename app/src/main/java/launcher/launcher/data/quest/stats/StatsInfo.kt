package launcher.launcher.data.quest.stats

import android.content.Context
import androidx.room.Dao
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
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
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

@Entity
@Serializable
@TypeConverters(StatsConverter::class)
data class StatsInfo(
    @PrimaryKey
    val id: String,
    val quest_id: String,
    val user_id: String,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
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

    @Query("SELECT * FROM StatsInfo WHERE id = :id LIMIT 1")
    suspend fun getStatsById(id: String): StatsInfo?

    @Query("SELECT * FROM StatsInfo WHERE date = :date LIMIT 1")
    suspend fun getStatsForUserOnDate( date: LocalDate): StatsInfo?

    @Query("SELECT * FROM StatsInfo WHERE user_id = :userId ORDER BY date DESC")
    fun getAllStatsForUser(userId: String): Flow<List<StatsInfo>>

    @Query("DELETE FROM StatsInfo WHERE id = :id")
    suspend fun deleteStatsById(id: String)

    @Query("DELETE FROM StatsInfo WHERE user_id = :userId")
    suspend fun deleteAllStatsForUser(userId: String)

    @Query("SELECT * FROM StatsInfo WHERE isSynced = 0 ORDER BY date DESC")
    fun getAllUnSyncedStats() : Flow<List<StatsInfo>>


    @Query("UPDATE StatsInfo SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}



@androidx.room.Database(
    entities = [StatsInfo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StatsConverter::class)
abstract class StatsDatabase : RoomDatabase() {
    abstract fun statsDao(): StatsInfoDao
}


object StatsDatabaseProvider {
    @Volatile
    private var INSTANCE: StatsDatabase? = null

    fun getInstance(context: Context): StatsDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                StatsDatabase::class.java,
                "stats_info_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
