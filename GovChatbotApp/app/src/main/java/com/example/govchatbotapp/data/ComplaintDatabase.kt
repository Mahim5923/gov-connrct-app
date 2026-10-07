package com.example.govchatbotapp.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey
    var id: String = "",
    var userId: String = "",
    var department: String = "",
    var description: String = "",
    var status: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ComplaintDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Query("SELECT * FROM complaints WHERE id = :id")
    suspend fun getComplaintById(id: String): ComplaintEntity?
    
    @Query("SELECT * FROM complaints ORDER BY timestamp DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>
}

@Database(entities = [ComplaintEntity::class], version = 1, exportSchema = false)
abstract class ComplaintDatabase : RoomDatabase() {
    abstract fun complaintDao(): ComplaintDao

    companion object {
        @Volatile
        private var INSTANCE: ComplaintDatabase? = null

        fun getDatabase(context: Context): ComplaintDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ComplaintDatabase::class.java,
                    "complaint_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
