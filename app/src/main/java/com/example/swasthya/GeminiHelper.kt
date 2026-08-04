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

    private fun isFallbackEligible(e: Throwable): Boolean {
        val msg = e.message ?: ""
        if (e is java.io.IOException || 
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException) {
            return true
        }
        if (msg.contains("429") || 
            msg.contains("RESOURCE_EXHAUSTED") || 
            msg.contains("quota") || 
            msg.contains("limit") || 
            msg.contains("503") || 
            msg.contains("overloaded") || 
            msg.contains("timeout") ||
            msg.contains("unavailable")) {
            return true
        }
        if (e.javaClass.name.contains("ServerException")) {
            return true
        }
        return false
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
    }

    private suspend fun callOpenRouter(
        systemInstruction: String?,
        userPrompt: String,
        base64Image: String? = null
    ): String {
        return withContext(Dispatchers.IO) {
            val url = java.net.URL("https://openrouter.ai/api/v1/chat/completions")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            connection.setRequestProperty("HTTP-Referer", "https://github.com/google/gemini")
            connection.setRequestProperty("X-Title", "Med Assist")
            connection.doOutput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000

            val messagesArray = org.json.JSONArray()
            
            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = org.json.JSONObject().apply {
                    put("role", "system")
                    put("content", systemInstruction)
                }
                messagesArray.put(sysObj)
            }

            val userContent = if (base64Image == null) {
                userPrompt
            } else {
                val contentArr = org.json.JSONArray()
                contentArr.put(org.json.JSONObject().apply {
                    put("type", "text")
                    put("text", userPrompt)
                })
                contentArr.put(org.json.JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", org.json.JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$base64Image")
                    })
                })
                contentArr
            }

            val userObj = org.json.JSONObject().apply {
                put("role", "user")
                put("content", userContent)
            }
            messagesArray.put(userObj)

            val payload = org.json.JSONObject().apply {
                put("model", "openrouter/free")
                put("messages", messagesArray)
            }

            val writer = java.io.OutputStreamWriter(connection.outputStream)
            writer.write(payload.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = org.json.JSONObject(response)
                val choices = responseJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return@withContext message.getString("content")
                }
                throw Exception("Empty choices from OpenRouter")
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw Exception("OpenRouter API returned error $responseCode: $errorStream")
            }
        }
    }

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

                val responseText = try {
                    val inputContent = content {
                        if (bitmap != null) {
                            image(bitmap)
                        }
                        text(prompt)
                    }
                    val response = generativeModel.generateContent(inputContent)
                    android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                    response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                } catch (e: Exception) {
                    if (isFallbackEligible(e)) {
                        android.util.Log.w("GeminiHelper", "Gemini failed, falling back to OpenRouter", e)
                        val base64Image = bitmap?.let { bitmapToBase64(it) }
                        val openRouterResponse = callOpenRouter(
                            systemInstruction = null,
                            userPrompt = prompt,
                            base64Image = base64Image
                        )
                        android.util.Log.d("GeminiHelper", "AI_PROVIDER = OPENROUTER_FALLBACK")
                        openRouterResponse.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                    } else {
                        throw e
                    }
                }

                val json = JSONObject(responseText)
                val summary = json.optString("summary", "Could not analyze the food.")
                val calories = json.optInt("calories", 0)
                FoodAnalysis(summary, calories)
            } catch (e: Exception) {
                e.printStackTrace()
                FoodAnalysis("AI service is temporarily unavailable. Please try again shortly.", 0)
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
                
                val responseText = try {
                    val response = generativeModel.generateContent(prompt)
                    android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                    response.text?.trim() ?: "Could not generate summary."
                } catch (e: Exception) {
                    if (isFallbackEligible(e)) {
                        android.util.Log.w("GeminiHelper", "Gemini failed, falling back to OpenRouter", e)
                        val openRouterResponse = callOpenRouter(
                            systemInstruction = null,
                            userPrompt = prompt,
                            base64Image = null
                        )
                        android.util.Log.d("GeminiHelper", "AI_PROVIDER = OPENROUTER_FALLBACK")
                        openRouterResponse.trim()
                    } else {
                        throw e
                    }
                }
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "AI service is temporarily unavailable. Please try again shortly."
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

                val responseText = try {
                    val inputContent = content {
                        image(bitmap)
                        text(prompt)
                    }
                    val response = generativeModel.generateContent(inputContent)
                    android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                    response.text?.trim()
                } catch (e: Exception) {
                    if (isFallbackEligible(e)) {
                        android.util.Log.w("GeminiHelper", "Gemini failed, falling back to OpenRouter", e)
                        val base64Image = bitmapToBase64(bitmap)
                        val openRouterResponse = callOpenRouter(
                            systemInstruction = null,
                            userPrompt = prompt,
                            base64Image = base64Image
                        )
                        android.util.Log.d("GeminiHelper", "AI_PROVIDER = OPENROUTER_FALLBACK")
                        openRouterResponse.trim()
                    } else {
                        throw e
                    }
                }
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "AI service is temporarily unavailable. Please try again shortly."
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

                val responseText = try {
                    val inputContent = content {
                        image(bitmap)
                        text(prompt)
                    }
                    val response = generativeModel.generateContent(inputContent)
                    android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                    response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                } catch (e: Exception) {
                    if (isFallbackEligible(e)) {
                        android.util.Log.w("GeminiHelper", "Gemini failed, falling back to OpenRouter", e)
                        val base64Image = bitmapToBase64(bitmap)
                        val openRouterResponse = callOpenRouter(
                            systemInstruction = null,
                            userPrompt = prompt,
                            base64Image = base64Image
                        )
                        android.util.Log.d("GeminiHelper", "AI_PROVIDER = OPENROUTER_FALLBACK")
                        openRouterResponse.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                    } else {
                        throw e
                    }
                }

                val json = JSONObject(responseText)
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

                val responseText = try {
                    val response = generativeModel.generateContent(prompt)
                    android.util.Log.d("GeminiHelper", "AI_PROVIDER = GEMINI")
                    response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                } catch (e: Exception) {
                    if (isFallbackEligible(e)) {
                        android.util.Log.w("GeminiHelper", "Gemini failed, falling back to OpenRouter", e)
                        val openRouterResponse = callOpenRouter(
                            systemInstruction = null,
                            userPrompt = prompt,
                            base64Image = null
                        )
                        android.util.Log.d("GeminiHelper", "AI_PROVIDER = OPENROUTER_FALLBACK")
                        openRouterResponse.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
                    } else {
                        throw e
                    }
                }

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
                    description = "AI service is temporarily unavailable. Please try again shortly.",
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
