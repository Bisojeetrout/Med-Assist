package com.example.swasthya.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val healthScore: Int = 100,
    val age: String = "",
    val weight: String = "",
    val height: String = "",
    val bloodGroup: String = "",
    val disease: String = "",
    val expectedGoals: String = "",
    val phone: String = "",
    val isProfileComplete: Boolean = false,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val sosContactPreference: String = "Ask",
    val sosActionPreference: String = "Ask",
    val customSosMessage: String = ""
)

@Entity(tableName = "vitals")
data class VitalsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val mood: String = "",
    val painLevel: Int = 0,
    val energyLevel: String = "",
    val sleepDuration: String = "",
    val symptoms: String = "",
    val notes: String = ""
)

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String? = null,
    val dosage: String = "",
    val schedule: String = "",
    val explanation: String = "",
    val photoUri: String? = null,
    val cloudImageUrl: String? = null,
    val hasImage: Boolean = false,
    val timeInMillis: Long,
    val timeLabel: String,
    val reminderType: String,
    val isTaken: Boolean = false
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val localUri: String,
    val uploadDate: String,
    val syncedToCloud: Boolean = false,
    val cloudUrl: String? = null,
    val reportSummary: String? = null
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val photoUri: String?,
    val cloudUrl: String? = null,
    val aiAnalysis: String? = null,
    val calories: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "physicians")
data class PhysicianEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val hospital: String,
    val phone: String
)

@Dao
interface SwasthyaDao {
    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM vitals ORDER BY id DESC")
    fun getAllVitals(): Flow<List<VitalsEntity>>

    @Insert
    suspend fun insertVitals(vitals: VitalsEntity)

    @Query("SELECT * FROM medicines ORDER BY id DESC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Insert
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Insert
    suspend fun insertReport(report: ReportEntity)

    @androidx.room.Delete
    suspend fun deleteReport(report: ReportEntity)

    @androidx.room.Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @androidx.room.Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM foods ORDER BY timestamp DESC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Insert
    suspend fun insertFood(food: FoodEntity)

    @androidx.room.Delete
    suspend fun deleteFood(food: FoodEntity)

    @Query("SELECT * FROM physicians ORDER BY id DESC")
    fun getAllPhysicians(): Flow<List<PhysicianEntity>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPhysician(physician: PhysicianEntity)

    @androidx.room.Delete
    suspend fun deletePhysician(physician: PhysicianEntity)
}

@Database(entities = [UserEntity::class, VitalsEntity::class, MedicineEntity::class, ReportEntity::class, FoodEntity::class, PhysicianEntity::class], version = 14, exportSchema = false)
abstract class SwasthyaDatabase : RoomDatabase() {
    abstract fun swasthyaDao(): SwasthyaDao

    companion object {
        @Volatile
        private var INSTANCE: SwasthyaDatabase? = null

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reports ADD COLUMN reportSummary TEXT")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN emergencyContactName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN emergencyContactPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN sosContactPreference TEXT NOT NULL DEFAULT 'Ask'")
                db.execSQL("ALTER TABLE users ADD COLUMN sosActionPreference TEXT NOT NULL DEFAULT 'Ask'")
                db.execSQL("ALTER TABLE users ADD COLUMN customSosMessage TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): SwasthyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwasthyaDatabase::class.java,
                    "swasthya_database"
                )
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
