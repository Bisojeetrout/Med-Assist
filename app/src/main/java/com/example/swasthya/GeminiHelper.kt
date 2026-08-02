package com.example.swasthya

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.swasthya.data.FoodEntity
import com.example.swasthya.data.MedicineEntity
import com.example.swasthya.data.ReportEntity
import com.example.swasthya.data.UserEntity
import com.example.swasthya.data.VitalsEntity
import com.example.swasthya.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class FoodAnalysis(val summary: String, val calories: Int)

data class ComprehensiveAnalysis(
    val healthStatus: String,
    val nutrientRecommendations: String,
    val exerciseRecommendations: String,
    val medicineAnalysis: String
)

object GeminiHelper {
    
    // Using Firebase AI Logic (Gemini Developer API backend)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun analyzeFood(bitmap: Bitmap?, description: String): FoodAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze this food based on the image and description.
                    Description: $description
                    Estimate the macronutrients (Carbohydrates, Protein, Fat) and total calories.
                    You MUST return ONLY a valid JSON object in the following format, with no markdown formatting or backticks:
                    {
                        "summary": "Estimated: 40g Carbs, 20g Protein, 15g Fat (375 Calories)",
                        "calories": 375
                    }
                """.trimIndent()

                val inputContent = content {
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                val json = JSONObject(responseText)
                val summary = json.optString("summary", "Could not analyze the food.")
                val calories = json.optInt("calories", 0)
                FoodAnalysis(summary, calories)
            } catch (e: Exception) {
                e.printStackTrace()
                FoodAnalysis("Analysis failed: ${e.message}", 0)
            }
        }
    }

    suspend fun getQuickSummary(steps: String, hr: String, calories: String, mealsCount: Int, reports: List<ReportEntity>, medicines: List<MedicineEntity>): String {
        return withContext(Dispatchers.IO) {
            try {
                val reportsInfo = if (reports.isEmpty()) "None" else reports.take(3).joinToString("; ") { it.fileName + " summary: " + (it.reportSummary ?: "No summary") }
                val medsInfo = if (medicines.isEmpty()) "None" else medicines.joinToString(", ") { it.name ?: "Unknown" }
                val prompt = """
                    You are an AI Health Assistant. Provide a short, 2-sentence friendly summary of the user's current day.
                    Their data:
                    Steps: $steps
                    Heart Rate: $hr bpm
                    Calories Burned: $calories kcal
                    Meals Logged Today: $mealsCount
                    Recent Reports: $reportsInfo
                    Current Medications: $medsInfo
                    
                    If they haven't logged meals, gently remind them. Also, if there's anything notable in their recent reports or medications they should take, briefly mention it. Keep it very concise and friendly. No markdown formatting.
                """.trimIndent()
                
                val response = generativeModel.generateContent(prompt)
                response.text?.trim() ?: "Could not generate summary."
            } catch (e: Exception) {
                e.printStackTrace()
                "Summary unavailable: ${e.message}"
            }
        }
    }

    suspend fun analyzeMedicalReport(context: Context, localUri: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(localUri)
                if (!file.exists()) return@withContext null

                var bitmap: Bitmap? = null
                if (file.name.endsWith(".pdf", ignoreCase = true)) {
                    val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val pdfRenderer = PdfRenderer(parcelFileDescriptor)
                    if (pdfRenderer.pageCount > 0) {
                        val page = pdfRenderer.openPage(0)
                        bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                    }
                    pdfRenderer.close()
                    parcelFileDescriptor.close()
                } else {
                    bitmap = BitmapFactory.decodeFile(file.absolutePath)
                }

                if (bitmap == null) return@withContext null

                val prompt = """
                    You are an expert AI medical assistant. Analyze this medical report carefully.
                    Extract the key findings, out-of-range metrics (if any), and provide a brief summary of the report's conclusion.
                    Keep the summary concise and strictly based on the information provided in the report.
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                response.text?.trim()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
