package neth.iecal.questphone.backed.repositories

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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Entity(tableName = "app_widget_config")
data class AppWidgetConfig(
    @PrimaryKey val id: String,
    val widgetId: Int,
    val height: Int,
    val width: Int? = null,
    val borderless: Boolean = false,
    val background: Boolean = true,
    val themeColors: Boolean = true,
    val order: Int
)


@Dao
interface AppWidgetConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AppWidgetConfig)

    @Query("SELECT * FROM app_widget_config WHERE widgetId = :id LIMIT 1")
    suspend fun getConfigById(id: Int): AppWidgetConfig?

    @Query("SELECT * FROM app_widget_config ORDER BY `order` ASC")
    suspend fun getAllConfigs(): List<AppWidgetConfig>

    @Delete
    suspend fun deleteConfig(config: AppWidgetConfig)

    @Query("DELETE FROM app_widget_config")
    suspend fun deleteAllConfigs()
}


@Database(
    entities = [AppWidgetConfig::class],
    version = 1,
    exportSchema = false
)
abstract class WidgetDatabase : RoomDatabase() {
    abstract fun appWidgetConfigDao(): AppWidgetConfigDao
}

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {

    @Provides
    @Singleton
    fun provideDatabase(context: Context): WidgetDatabase =
        Room.databaseBuilder(
            context,
            WidgetDatabase::class.java,
            "questphone_database"
        ).build()

    @Provides
    fun provideAppWidgetConfigDao(db: WidgetDatabase): AppWidgetConfigDao =
        db.appWidgetConfigDao()
}

