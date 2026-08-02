package com.example.swasthya

import android.util.Log
import com.example.swasthya.data.FoodEntity
import com.example.swasthya.data.MedicineEntity
import com.example.swasthya.data.ReportEntity
import com.example.swasthya.data.UserEntity
import com.example.swasthya.data.VitalsEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirestoreSync {
    private const val TAG = "FirestoreSync"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Safely gets the user ID to use as the document ID.
     * First checks Firebase Auth for a logged-in Gmail.
     * If not found, falls back to the phone number.
     * If both empty, defaults to "anonymous_user".
     */
    private fun getUserId(phone: String): String {
        val authEmail = FirebaseAuth.getInstance().currentUser?.email
        if (!authEmail.isNullOrBlank()) {
            return authEmail.trim().lowercase()
        }
        val sanitized = phone.trim().lowercase()
        return if (sanitized.isNotEmpty()) sanitized else "anonymous_user"
    }

    fun syncUser(user: UserEntity) {
        val userId = getUserId(user.phone)
        val userData = hashMapOf(
            "name" to user.name,
            "healthScore" to user.healthScore,
            "age" to user.age,
            "weight" to user.weight,
            "height" to user.height,
            "bloodGroup" to user.bloodGroup,
            "disease" to user.disease,
            "expectedGoals" to user.expectedGoals,
            "phone" to user.phone,
            "isProfileComplete" to user.isProfileComplete,
            "lastSynced" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener { Log.d(TAG, "User profile synced successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error syncing user profile", e) }
    }

    fun syncFood(userIdPhone: String, food: FoodEntity) {
        val userId = getUserId(userIdPhone)
        val foodData = hashMapOf(
            "description" to food.description,
            "cloudUrl" to food.cloudUrl,
            "aiAnalysis" to food.aiAnalysis,
            "calories" to (food.calories ?: 0),
            "timestamp" to food.timestamp
        )

        db.collection("users").document(userId)
            .collection("foods")
            .add(foodData)
            .addOnSuccessListener { Log.d(TAG, "Food logged successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error logging food", e) }
    }

    fun syncVitals(userIdPhone: String, vital: VitalsEntity) {
        val userId = getUserId(userIdPhone)
        val vitalData = hashMapOf(
            "date" to vital.date,
            "mood" to vital.mood,
            "painLevel" to vital.painLevel,
            "energyLevel" to vital.energyLevel,
            "sleepDuration" to vital.sleepDuration,
            "symptoms" to vital.symptoms,
            "notes" to vital.notes
        )

        db.collection("users").document(userId)
            .collection("vitals")
            .add(vitalData)
            .addOnSuccessListener { Log.d(TAG, "Vitals synced successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error syncing vitals", e) }
    }

    fun syncMedicine(userIdPhone: String, med: MedicineEntity) {
        val userId = getUserId(userIdPhone)
        val medData = hashMapOf(
            "name" to med.name,
            "dosage" to med.dosage,
            "schedule" to med.schedule,
            "explanation" to med.explanation,
            "hasImage" to med.hasImage,
            "cloudUrl" to med.cloudImageUrl,
            "timeLabel" to med.timeLabel,
            "reminderType" to med.reminderType,
            "isTaken" to med.isTaken
        )

        db.collection("users").document(userId)
            .collection("medicines")
            .document(med.id.toString()) // Using Room ID as doc ID to allow updates
            .set(medData)
            .addOnSuccessListener { Log.d(TAG, "Medicine synced successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error syncing medicine", e) }
    }

    fun syncReport(userIdPhone: String, report: ReportEntity) {
        val userId = getUserId(userIdPhone)
        val reportData = hashMapOf(
            "fileName" to report.fileName,
            "uploadDate" to report.uploadDate,
            "cloudUrl" to report.cloudUrl,
            "syncedToCloud" to report.syncedToCloud,
            "reportSummary" to report.reportSummary
        )

        db.collection("users").document(userId)
            .collection("reports")
            .add(reportData)
            .addOnSuccessListener { Log.d(TAG, "Report synced successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error syncing report", e) }
    }

    fun deleteReport(userIdPhone: String, report: ReportEntity) {
        val userId = getUserId(userIdPhone)
        val reportsRef = db.collection("users").document(userId).collection("reports")
        
        // Find the document by cloudUrl or fileName
        val query = if (report.cloudUrl != null) {
            reportsRef.whereEqualTo("cloudUrl", report.cloudUrl)
        } else {
            reportsRef.whereEqualTo("fileName", report.fileName).whereEqualTo("uploadDate", report.uploadDate)
        }
        
        query.get().addOnSuccessListener { snapshot ->
            for (document in snapshot.documents) {
                document.reference.delete()
                    .addOnSuccessListener { Log.d(TAG, "Report deleted from Firestore") }
                    .addOnFailureListener { e -> Log.e(TAG, "Error deleting report from Firestore", e) }
            }
        }.addOnFailureListener { e -> Log.e(TAG, "Error querying report to delete", e) }
    }

    /**
     * Sync Daily Activity from Watch/Health Connect (e.g. Steps, Calories Burned).
     * Uses the Date string (e.g. "2026-08-01") as the document ID to prevent duplicates.
     */
    fun syncDailyActivity(userIdPhone: String, steps: String, heartRate: String, caloriesBurned: String) {
        val userId = getUserId(userIdPhone)
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        
        // Parse numbers safely
        val totalSteps = steps.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        val avgHr = heartRate.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        val totalCalories = caloriesBurned.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

        val activityData = hashMapOf(
            "date" to dateString,
            "totalSteps" to totalSteps,
            "avgHeartRate" to avgHr,
            "totalCaloriesBurned" to totalCalories,
            "lastSynced" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .collection("daily_activity")
            .document(dateString)
            .set(activityData)
            .addOnSuccessListener { Log.d(TAG, "Daily activity synced successfully") }
            .addOnFailureListener { e -> Log.e(TAG, "Error syncing daily activity", e) }
    }
}
