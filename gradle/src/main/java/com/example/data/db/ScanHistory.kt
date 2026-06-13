package com.example.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.DatabaseView
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val skinType: String,
    val issue: String,
    val description: String,
    val metricsHydration: Int,
    val metricsRedness: Int,
    val metricsTexture: Int,
    val metricsPores: Int,
    val ingredientsJson: String, // Comma separated or JSON
    val routineMorningJson: String, // Comma separated or JSON
    val routineEveningJson: String // Comma separated or JSON
)

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScan(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Database(entities = [ScanHistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : androidx.room.RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glowlogic_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
