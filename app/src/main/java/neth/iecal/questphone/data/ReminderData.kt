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
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class ReminderData(
    @PrimaryKey val quest_id: String,
    val timeMillis: Long,
    val title: String,
    val description: String,

    val date: String,   // format: yyyyMMdd
    val count: Int      // how many reminders have been sent today
)


@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(reminders: List<ReminderData>)

    @Query("SELECT * FROM ReminderData WHERE timeMillis <= :time AND quest_id = :questId")
    suspend fun getTriggeredReminders(time: Long, questId: String): List<ReminderData>

    @Query("SELECT * FROM ReminderData WHERE quest_id = :questId")
    suspend fun getRemindersByQuestId(questId: String): ReminderData?

    @Query("DELETE FROM ReminderData WHERE quest_id = :questId")
    suspend fun deleteByQuestId(questId: String)

    @Delete
    suspend fun delete(reminder: ReminderData)

    @Query("DELETE FROM ReminderData")
    suspend fun deleteAll()

    @Query("SELECT * FROM REMINDERDATA")
    fun getAll(): List<ReminderData>

    @Query("SELECT * FROM ReminderData WHERE timeMillis >= :time")
    suspend fun getAllUpcoming(time: Long = System.currentTimeMillis()): List<ReminderData>

}


@Database(
    entities = [ReminderData::class],
    version = 1,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}


object ReminderDatabaseProvider {
    @Volatile
    private var INSTANCE: ReminderDatabase? = null

    fun getInstance(context: Context): ReminderDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ReminderDatabase::class.java,
                "reminder_db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}


