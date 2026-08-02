package com.example.swasthya

/**
 * Step 8 & 9: Structured AI Response Model
 */
data class HealthAiResponse(
    val summary: String,
    val importantFindings: String,
    val activityInsights: String,
    val nutritionInsights: String,
    val healthTrends: String,
    val suggestions: String,
    val warnings: String,
    val needsMedicalAttention: Boolean
)
