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
    val customSosMessage: String = "",
    val dailyCalorieLimit: Int = 2000,
    val emergencyContactsJson: String = "[]",
    val autoCallAfterSos: Boolean = false,
    val sosCountdownEnabled: Boolean = true,
    val sosCountdownSeconds: Int = 5
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
    val reportSummary: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val photoUri: String?,
    val cloudUrl: String? = null,
    val aiAnalysis: String? = null,
    val calories: Int? = null,
    val carbs: Int? = null,
    val protein: Int? = null,
    val fat: Int? = null,
    val dishName: String? = null,
    val weightGrams: Int? = null,
    val micronutrients: String? = null,
    val deficiencyWarnings: String? = null,
    val isConsumed: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "physicians")
data class PhysicianEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val hospital: String,
    val phone: String
)

@Entity(tableName = "indian_medicines")
data class IndianMedicineEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val price: String,
    val isDiscontinued: Boolean,
    val manufacturerName: String,
    val type: String,
    val packSizeLabel: String,
    val shortComposition1: String,
    val shortComposition2: String,
    val substitute0: String,
    val substitute1: String,
    val substitute2: String,
    val substitute3: String,
    val substitute4: String,
    val sideEffects: String,
    val use0: String,
    val use1: String,
    val use2: String,
    val use3: String,
    val use4: String,
    val chemicalClass: String,
    val habitForming: String,
    val therapeuticClass: String,
    val actionClass: String
)

@Entity(tableName = "indian_products")
data class IndianProductEntity(
    @PrimaryKey val productId: Int,
    val brandName: String,
    val manufacturer: String,
    val priceInr: String,
    val isDiscontinued: String,
    val dosageForm: String,
    val packSize: String,
    val packUnit: String,
    val numActiveIngredients: String,
    val primaryIngredient: String,
    val primaryStrength: String,
    val activeIngredients: String,
    val therapeuticClass: String,
    val packagingRaw: String,
    val manufacturerRaw: String
)

@Entity(tableName = "jan_aushadhi_medicines")
data class JanAushadhiEntity(
    @PrimaryKey val srNo: Int,
    val drugCode: String,
    val genericName: String,
    val unitSize: String,
    val mrp: String,
    val groupName: String
)

@Entity(tableName = "one_mg_medicines")
data class OneMgMedicineEntity(
    @PrimaryKey val index: Int,
    val name: String,
    val mrp: String,
    val quantity: String,
    val manufacturer: String,
    val saltComposition: String,
    val imageUrl: String
)

@Entity(tableName = "prescription_history")
data class PrescriptionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val extractedText: String,
    val medicinesJson: String,
    val analysisContext: String,
    val localUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "medicine_scans")
data class MedicineScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineName: String,
    val composition: String,
    val strength: String,
    val dosageForm: String,
    val manufacturer: String,
    val price: String,
    val dosage: String,
    val frequency: String,
    val duration: String,
    val source: String, // e.g., "Prescription", "Medicine Box"
    val localUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sos_events")
data class SosEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactsMessagedJson: String,
    val locationLink: String,
    val timestamp: Long = System.currentTimeMillis()
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

    // Medicine Intelligence Queries
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertIndianMedicines(medicines: List<IndianMedicineEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertIndianProducts(products: List<IndianProductEntity>)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertJanAushadhi(medicines: List<JanAushadhiEntity>)

    @Query("SELECT COUNT(*) FROM indian_medicines")
    suspend fun getIndianMedicinesCount(): Int

    @Query("SELECT * FROM indian_products WHERE brandName LIKE '%' || :name || '%' LIMIT 10")
    suspend fun searchProductsByName(name: String): List<IndianProductEntity>

    @Query("SELECT * FROM indian_medicines WHERE name LIKE '%' || :name || '%' LIMIT 10")
    suspend fun searchMedicinesByName(name: String): List<IndianMedicineEntity>

    @Query("SELECT * FROM indian_products WHERE primaryIngredient LIKE '%' || :composition || '%' AND dosageForm LIKE '%' || :dosageForm || '%' ORDER BY CAST(priceInr AS REAL) ASC LIMIT 20")
    suspend fun searchAlternatives(composition: String, dosageForm: String): List<IndianProductEntity>

    @Query("SELECT * FROM one_mg_medicines WHERE name LIKE '%' || :name || '%' LIMIT 10")
    suspend fun searchOneMgByName(name: String): List<OneMgMedicineEntity>

    @Query("SELECT * FROM one_mg_medicines WHERE saltComposition LIKE '%' || :composition || '%' ORDER BY CAST(mrp AS REAL) ASC LIMIT 20")
    suspend fun searchOneMgAlternatives(composition: String): List<OneMgMedicineEntity>

    @Query("SELECT * FROM jan_aushadhi_medicines WHERE genericName LIKE '%' || :composition || '%' ORDER BY CAST(mrp AS REAL) ASC LIMIT 5")
    suspend fun searchJanAushadhiAlternatives(composition: String): List<JanAushadhiEntity>

    // Medicine Scans
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertMedicineScan(scan: MedicineScanEntity)

    @Query("SELECT * FROM medicine_scans ORDER BY timestamp DESC")
    fun getAllMedicineScans(): Flow<List<MedicineScanEntity>>

    @androidx.room.Delete
    suspend fun deleteMedicineScan(scan: MedicineScanEntity)
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertOneMgMedicines(medicines: List<OneMgMedicineEntity>)

    @Query("SELECT COUNT(*) FROM one_mg_medicines")
    suspend fun getOneMgMedicinesCount(): Int

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertPrescriptionHistory(history: PrescriptionHistoryEntity)

    @Query("SELECT * FROM prescription_history ORDER BY timestamp DESC")
    fun getAllPrescriptionHistory(): Flow<List<PrescriptionHistoryEntity>>

    @androidx.room.Delete
    suspend fun deletePrescriptionHistory(history: PrescriptionHistoryEntity)

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertSosEvent(event: SosEventEntity)

    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllSosEvents(): Flow<List<SosEventEntity>>
}

@Database(entities = [UserEntity::class, VitalsEntity::class, MedicineEntity::class, ReportEntity::class, FoodEntity::class, PhysicianEntity::class, IndianMedicineEntity::class, IndianProductEntity::class, JanAushadhiEntity::class, MedicineScanEntity::class, OneMgMedicineEntity::class, PrescriptionHistoryEntity::class, SosEventEntity::class], version = 20, exportSchema = false)
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

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `indian_medicines` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `price` TEXT NOT NULL, `isDiscontinued` INTEGER NOT NULL, `manufacturerName` TEXT NOT NULL, `type` TEXT NOT NULL, `packSizeLabel` TEXT NOT NULL, `shortComposition1` TEXT NOT NULL, `shortComposition2` TEXT NOT NULL, `substitute0` TEXT NOT NULL, `substitute1` TEXT NOT NULL, `substitute2` TEXT NOT NULL, `substitute3` TEXT NOT NULL, `substitute4` TEXT NOT NULL, `sideEffects` TEXT NOT NULL, `use0` TEXT NOT NULL, `use1` TEXT NOT NULL, `use2` TEXT NOT NULL, `use3` TEXT NOT NULL, `use4` TEXT NOT NULL, `chemicalClass` TEXT NOT NULL, `habitForming` TEXT NOT NULL, `therapeuticClass` TEXT NOT NULL, `actionClass` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `indian_products` (`productId` INTEGER NOT NULL, `brandName` TEXT NOT NULL, `manufacturer` TEXT NOT NULL, `priceInr` TEXT NOT NULL, `isDiscontinued` TEXT NOT NULL, `dosageForm` TEXT NOT NULL, `packSize` TEXT NOT NULL, `packUnit` TEXT NOT NULL, `numActiveIngredients` TEXT NOT NULL, `primaryIngredient` TEXT NOT NULL, `primaryStrength` TEXT NOT NULL, `activeIngredients` TEXT NOT NULL, `therapeuticClass` TEXT NOT NULL, `packagingRaw` TEXT NOT NULL, `manufacturerRaw` TEXT NOT NULL, PRIMARY KEY(`productId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `jan_aushadhi_medicines` (`srNo` INTEGER NOT NULL, `drugCode` TEXT NOT NULL, `genericName` TEXT NOT NULL, `unitSize` TEXT NOT NULL, `mrp` TEXT NOT NULL, `groupName` TEXT NOT NULL, PRIMARY KEY(`srNo`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `medicine_scans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `medicineName` TEXT NOT NULL, `composition` TEXT NOT NULL, `strength` TEXT NOT NULL, `dosageForm` TEXT NOT NULL, `manufacturer` TEXT NOT NULL, `price` TEXT NOT NULL, `dosage` TEXT NOT NULL, `frequency` TEXT NOT NULL, `duration` TEXT NOT NULL, `source` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `one_mg_medicines` (`index` INTEGER NOT NULL, `name` TEXT NOT NULL, `mrp` TEXT NOT NULL, `quantity` TEXT NOT NULL, `manufacturer` TEXT NOT NULL, `saltComposition` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, PRIMARY KEY(`index`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `prescription_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `extractedText` TEXT NOT NULL, `medicinesJson` TEXT NOT NULL, `analysisContext` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN emergencyContactsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE users ADD COLUMN autoCallAfterSos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN sosCountdownEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE users ADD COLUMN sosCountdownSeconds INTEGER NOT NULL DEFAULT 5")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sos_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contactsMessagedJson` TEXT NOT NULL, `locationLink` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE prescription_history ADD COLUMN localUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicine_scans ADD COLUMN localUri TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): SwasthyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SwasthyaDatabase::class.java,
                    "swasthya_database"
                )
                .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
