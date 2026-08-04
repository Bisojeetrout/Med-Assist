package com.example.swasthya

import com.example.swasthya.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Step 3 & 4: Centralized AI service/repository for Gemini.
 */
class HealthAiRepository {

    private val systemInstructionsText = """
        You are the AI health intelligence assistant inside this application.

        Your job is to analyze the health information explicitly provided to you and explain useful patterns to the user in clear language.

        Possible information may include:
        - profile information
        - medical history
        - medications
        - allergies
        - activity information
        - wearable measurements
        - sleep
        - nutrition
        - laboratory/report information
        - user questions

        IMPORTANT SAFETY RULES:

        Never claim that wearable, nutrition, or AI-generated information establishes a medical diagnosis.

        Clearly distinguish:
        1. measured data
        2. user-entered data
        3. estimated data
        4. AI interpretation

        Do not invent missing medical information.

        If there is insufficient information, explicitly say that more information is required.

        When potentially concerning patterns are present, explain them calmly and recommend appropriate professional medical evaluation rather than diagnosing the user.

        Do not tell users to start, stop, or change prescription medication without professional medical guidance.

        Prioritize useful, concise, understandable explanations.
    """.trimIndent()

    private val systemInstructions = content {
        text(systemInstructionsText)
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY,
        systemInstruction = systemInstructions
    )




    /**
     * Step 10: Test AI Connection
     */
    suspend fun testSimpleRequest(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("HealthAiRepository", "AI_PROVIDER = GEMINI")
                val responseText = response.text ?: "No response from Gemini."
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }

    /**
     * Main function for analyzing context
     */
    suspend fun analyzeHealthContext(contextString: String): HealthAiResponse {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Review the following user health context:
                    
                    $contextString
                    
                    Provide a comprehensive health analysis based on the safety rules provided in your system instructions.
                    
                    You MUST return ONLY a valid JSON object in the following format, with no markdown formatting or backticks around the JSON string.
                    IMPORTANT FORMATTING RULES for the JSON values:
                    - Do NOT use markdown (no **, no *, no `). The UI cannot render markdown.
                    - Use standard plain text. If you need a list, use dashes (-) or emojis.
                    - Keep sentences short, friendly, and highly readable.
                    
                    {
                        "summary": "Overall health summary...",
                        "importantFindings": "Any critical patterns...",
                        "activityInsights": "Insights on activity/exercise...",
                        "nutritionInsights": "Insights on diet...",
                        "healthTrends": "Any observed trends...",
                        "suggestions": "Actionable non-medical advice...",
                        "warnings": "Any concerning patterns...",
                        "needsMedicalAttention": false
                    }
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("HealthAiRepository", "AI_PROVIDER = GEMINI")
                val responseText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"

                val json = JSONObject(responseText)
                HealthAiResponse(
                    summary = json.optString("summary", "Data unavailable."),
                    importantFindings = json.optString("importantFindings", ""),
                    activityInsights = json.optString("activityInsights", ""),
                    nutritionInsights = json.optString("nutritionInsights", ""),
                    healthTrends = json.optString("healthTrends", ""),
                    suggestions = json.optString("suggestions", ""),
                    warnings = json.optString("warnings", ""),
                    needsMedicalAttention = json.optBoolean("needsMedicalAttention", false)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                HealthAiResponse(
                    summary = "Error: ${e.message}",
                    importantFindings = "",
                    activityInsights = "",
                    nutritionInsights = "",
                    healthTrends = "",
                    suggestions = "",
                    warnings = "",
                    needsMedicalAttention = false
                )
            }
        }
    }

    suspend fun sendChatMessage(history: List<Pair<String, String>>, newMessage: String, contextData: String?): String {
        return withContext(Dispatchers.IO) {
            try {
                val promptBuilder = StringBuilder()
                promptBuilder.append("You are a helpful health AI assistant. Always use the following user context to answer their questions:\n")
                if (contextData != null) {
                    promptBuilder.append(contextData).append("\n\n")
                }
                promptBuilder.append("Here is the conversation history:\n")
                history.forEach { (role, msg) ->
                    promptBuilder.append("$role: $msg\n")
                }
                promptBuilder.append("user: $newMessage\n")
                promptBuilder.append("model: ")
                
                val prompt = promptBuilder.toString()
                val response = generativeModel.generateContent(prompt)
                android.util.Log.d("HealthAiRepository", "AI_PROVIDER = GEMINI")
                val responseText = response.text ?: "I'm sorry, I couldn't generate a response."
                responseText
            } catch (e: Exception) {
                e.printStackTrace()
                "Error: ${e.message}"
            }
        }
    }
}
