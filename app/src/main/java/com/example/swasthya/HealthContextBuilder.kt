package com.example.swasthya

import com.example.swasthya.data.FoodEntity
import com.example.swasthya.data.MedicineEntity
import com.example.swasthya.data.ReportEntity
import com.example.swasthya.data.UserEntity
import com.example.swasthya.data.VitalsEntity
import com.google.firebase.auth.FirebaseAuth

/**
 * Step 5: Health Context Builder
 * Collects and formats local Room data into a compact AI context.
 */
data class HealthContext(
    val userProfile: String,
    val recentVitals: String,
    val currentMedicines: String,
    val recentNutrition: String,
    val recentReports: String,
    val dailyActivity: String
) {
    fun toPromptString(): String {
        return """
            --- USER PROFILE ---
            $userProfile
            
            --- RECENT VITALS ---
            $recentVitals
            
            --- CURRENT MEDICINES ---
            $currentMedicines
            
            --- RECENT NUTRITION ---
            $recentNutrition
            
            --- MEDICAL REPORTS ---
            $recentReports
            
            --- DAILY ACTIVITY ---
            $dailyActivity
        """.trimIndent()
    }
}

object HealthContextBuilder {

    /**
     * Step 6: Firestore / Auth User Isolation
     * Only builds context if the user is authenticated.
     */
    fun buildContext(
        user: UserEntity?,
        vitals: List<VitalsEntity>,
        medicines: List<MedicineEntity>,
        reports: List<ReportEntity>,
        foods: List<FoodEntity>,
        steps: String,
        hr: String,
        calories: String
    ): HealthContext? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return null // Do not build context if no user is authenticated
        }

        // 1. Profile Information
        val profileStr = user?.let {
            "Age: ${it.age}, Weight: ${it.weight}kg, Height: ${it.height}cm\n" +
            "Known Diseases: ${it.disease}\n" +
            "Goals: ${it.expectedGoals}"
        } ?: "No profile data available."

        // 2. Medical History & Vitals (Limit to last 5 to save tokens)
        val vitalsStr = if (vitals.isEmpty()) "No recent vitals logged." else {
            vitals.take(5).joinToString("\n") { 
                "Date: ${it.date}, Pain Level: ${it.painLevel}, Mood: ${it.mood}" 
            }
        }

        // 3. Medications
        val medsStr = if (medicines.isEmpty()) "No active medications." else {
            medicines.joinToString("\n") { 
                "${it.name} - Dosage: ${it.dosage}, Frequency: ${it.schedule}" 
            }
        }

        // 4. Nutrition (Limit to last 5 foods)
        val foodStr = if (foods.isEmpty()) "No recent food logged." else {
            foods.take(5).joinToString("\n") { 
                "Timestamp: ${it.timestamp}: ${it.description} (Calories: ${it.calories}, AI Estimate: ${it.aiAnalysis})" 
            }
        }

        // 5. Reports (Cloudinary metadata and AI Summary)
        val reportsStr = if (reports.isEmpty()) "No medical reports." else {
            reports.joinToString("\n") { 
                "Report Date: ${it.uploadDate}, File: ${it.fileName}\nSummary: ${it.reportSummary ?: "No summary available"}\n" 
            }
        }

        // 6. Activity
        val activityStr = "Steps: $steps, Heart Rate: $hr BPM, Calories Burned: $calories kcal"

        return HealthContext(
            userProfile = profileStr,
            recentVitals = vitalsStr,
            currentMedicines = medsStr,
            recentNutrition = foodStr,
            recentReports = reportsStr,
            dailyActivity = activityStr
        )
    }
}
