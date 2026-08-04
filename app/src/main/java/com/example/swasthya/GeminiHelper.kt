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
import org.json.JSONArray
import java.io.File
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

data class FoodAnalysis(
    val dishName: String,
    val weightGrams: Int,
    val calories: Int,
    val carbohydrates: Int,
    val protein: Int,
    val fats: Int,
    val vitaminsAndMinerals: String,
    val deficiencyWarnings: String,
    val success: Boolean = true,
    val errorMessage: String? = null
)

data class ComprehensiveAnalysis(
    val healthStatus: String,
    val nutrientRecommendations: String,
    val exerciseRecommendations: String,
    val medicineAnalysis: String
)

object GeminiHelper {
    
    // Using Firebase AI Logic (Gemini Developer API backend)
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val foodGenerativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyAqeh0FqGW-QNzYFFJR9iyG_FUveBLlxro",
        generationConfig = com.google.ai.client.generativeai.type.generationConfig {
            temperature = 0.2f
            responseMimeType = "application/json"
        }
    )




    suspend fun analyzeFood(bitmap: Bitmap?, description: String): FoodAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Analyze this food based on the image and description.
                    Description: $description
                    Analyze this food. Provide an estimate for: 1. Dish name, 2. Weight in grams, 3. Total calories, 4. Carbohydrates (g), Protein (g), Fats (g), 5. Key vitamins & minerals (Vitamin A, Vitamin B12, Vitamin C, Vitamin D, Iron, Calcium, Zinc), 6. Deficiency warnings (highlight any missing essential micronutrients and the deficiency risks they pose).
                    You MUST return ONLY a valid JSON object in the following format, with no markdown formatting or backticks:
                    {
                        "dishName": "Name of dish",
                        "weightGrams": 250,
                        "calories": 350,
                        "carbohydrates": 40,
                        "protein": 20,
                        "fats": 10,
                        "vitaminsAndMinerals": "Vitamin A (10%), Iron...",
                        "deficiencyWarnings": "Missing Vitamin B12 which poses a risk for..."
                    }
                """.trimIndent()

                val inputContent = content {
                    if (bitmap != null) {
                        image(bitmap)
                    }
                    text(prompt)
                }
                val response = foodGenerativeModel.generateContent(inputContent)
                android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                var jsonString = response.text ?: "{}"
                val startIndex = jsonString.indexOf('{')
                val endIndex = jsonString.lastIndexOf('}')
                if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                    jsonString = jsonString.substring(startIndex, endIndex + 1)
                }
                val json = JSONObject(jsonString)
                FoodAnalysis(
                    dishName = json.optString("dishName", "Unknown"),
                    weightGrams = json.optInt("weightGrams", 0),
                    calories = json.optInt("calories", 0),
                    carbohydrates = json.optInt("carbohydrates", 0),
                    protein = json.optInt("protein", 0),
                    fats = json.optInt("fats", 0),
                    vitaminsAndMinerals = json.optString("vitaminsAndMinerals", "N/A"),
                    deficiencyWarnings = json.optString("deficiencyWarnings", "None detected"),
                    success = true
                )
            } catch (e: Exception) {
                e.printStackTrace()
                FoodAnalysis("", 0, 0, 0, 0, 0, "", "", false, e.message ?: "Unknown error")
            }
        }
    }

    suspend fun getQuickSummary(steps: String, hr: String, calories: String, mealsCount: Int, reports: List<ReportEntity>, medicines: List<MedicineEntity>): String {
        return withContext(Dispatchers.IO) {
            try {
                val tenDaysInMillis = 10L * 24 * 60 * 60 * 1000
                val now = System.currentTimeMillis()
                val recent10DayReports = reports.filter { (now - it.timestamp) <= tenDaysInMillis }
                val reportsInfo = if (recent10DayReports.isEmpty()) "No medical reports uploaded in the last 10 days." else recent10DayReports.take(3).joinToString("; ") { "${it.fileName} (${it.uploadDate}): ${it.reportSummary ?: "No summary"}" }
                val medsInfo = if (medicines.isEmpty()) "None" else medicines.joinToString(", ") { it.name ?: "Unknown" }
                val prompt = """
                    You are an AI Health Assistant. Provide a short, 2-sentence friendly summary of the user's current day.
                    Their data:
                    Steps: $steps
                    Heart Rate: $hr bpm
                    Calories Burned: $calories kcal
                    Meals Logged Today: $mealsCount
                    Recent 10-Day Medical Reports: $reportsInfo
                    Current Medications: $medsInfo
                    
                    If they haven't logged meals, gently remind them. Also, if there's anything notable in their recent 10-day medical reports or medications they should take, briefly mention it. Keep it very concise and friendly. No markdown formatting.
                """.trimIndent()
                
                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                val responseText = response.text?.trim() ?: "Could not generate summary."
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
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
                android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                val responseText = response.text?.trim()
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }

    suspend fun analyzeMedicine(bitmap: Bitmap): MedicineAnalysis? {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    You are an expert pharmacist AI. Analyze this image of a medicine/drug packaging or pill bottle.
                    Provide detailed information about the medicine. You MUST return ONLY a valid JSON object in the exact following format, with no markdown formatting or backticks around it:
                    {
                        "brand": "Brand Name",
                        "genericName": "Generic Name",
                        "use": "What it is used for",
                        "dosage": "Typical dosage found on the package",
                        "manufacturer": "Manufacturer name",
                        "sideEffects": "Common side effects",
                        "interactions": "Potential drug interactions",
                        "isGenuine": "Does this look like a genuine manufacturer product based on standard packaging? (Yes/No/Unsure)"
                    }
                """.trimIndent()

                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = generativeModel.generateContent(inputContent)
                android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                var jsonString = response.text ?: "{}"
                val startIndex = jsonString.indexOf('{')
                val endIndex = jsonString.lastIndexOf('}')
                if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
                    jsonString = jsonString.substring(startIndex, endIndex + 1)
                }
                val json = JSONObject(jsonString)
                MedicineAnalysis(
                    brand = json.optString("brand", "Unknown"),
                    genericName = json.optString("genericName", "Unknown"),
                    use = json.optString("use", "Unknown"),
                    dosage = json.optString("dosage", "Unknown"),
                    manufacturer = json.optString("manufacturer", "Unknown"),
                    sideEffects = json.optString("sideEffects", "Unknown"),
                    interactions = json.optString("interactions", "Unknown"),
                    isGenuine = json.optString("isGenuine", "Unknown")
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun checkDrugInteractions(medicinesList: List<String>): DrugInteractionResult {
        return withContext(Dispatchers.IO) {
            try {
                if (medicinesList.size <= 1) {
                    return@withContext DrugInteractionResult(
                        hasInteraction = false,
                        description = "No interactions possible with fewer than two medications.",
                        interactedDrugs = emptyList()
                    )
                }

                val medicinesJoined = medicinesList.joinToString(", ")
                val prompt = """
                    You are a clinical pharmacologist AI. Analyze the following list of medications for potential adverse drug-drug interactions:
                    Medications: $medicinesJoined

                    You MUST return ONLY a valid JSON object in the exact following format, with no markdown formatting or backticks around it:
                    {
                        "hasInteraction": true/false,
                        "description": "A clear, clinical explanation of the interactions found, or a positive confirmation statement that no adverse interactions were found among these drugs.",
                        "interactedDrugs": ["DrugName1", "DrugName2"]
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                val responseText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"

                val json = JSONObject(responseText)
                val hasInteraction = json.optBoolean("hasInteraction", false)
                val description = json.optString("description", "No adverse interactions found.")
                val interactedDrugsJson = json.optJSONArray("interactedDrugs")
                val interactedDrugs = mutableListOf<String>()
                if (interactedDrugsJson != null) {
                    for (i in 0 until interactedDrugsJson.length()) {
                        interactedDrugs.add(interactedDrugsJson.getString(i))
                    }
                }

                DrugInteractionResult(hasInteraction, description, interactedDrugs)
            } catch (e: Exception) {
                e.printStackTrace()
                DrugInteractionResult(
                    hasInteraction = false,
                    description = "Error: ${e.message}",
                    interactedDrugs = emptyList()
                )
            }
        }
    }
}

data class MedicineAnalysis(
    val brand: String,
    val genericName: String,
    val use: String,
    val dosage: String,
    val manufacturer: String,
    val sideEffects: String,
    val interactions: String,
    val isGenuine: String
)

data class DrugInteractionResult(
    val hasInteraction: Boolean,
    val description: String,
    val interactedDrugs: List<String>
)
