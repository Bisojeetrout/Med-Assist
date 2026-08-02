package com.example.swasthya.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthya.HealthAiRepository
import com.example.swasthya.HealthAiResponse
import com.example.swasthya.HealthContextBuilder
import com.example.swasthya.data.FoodEntity
import com.example.swasthya.data.MedicineEntity
import com.example.swasthya.data.ReportEntity
import com.example.swasthya.data.UserEntity
import com.example.swasthya.data.VitalsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI States for the AI Insights Screen
 */
sealed class InsightsUiState {
    object Idle : InsightsUiState()
    object Loading : InsightsUiState()
    data class Success(val response: HealthAiResponse) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
}

/**
 * Step 11: ViewModel to manage AI request state and prevent direct UI-to-AI calls.
 */
class InsightsViewModel : ViewModel() {

    private val repository = HealthAiRepository()

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Idle)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private var currentContextStr: String? = null

    fun generateInsights(
        user: UserEntity?,
        vitals: List<VitalsEntity>,
        medicines: List<MedicineEntity>,
        reports: List<ReportEntity>,
        foods: List<FoodEntity>,
        steps: String,
        hr: String,
        calories: String
    ) {
        // Prevent multiple simultaneous requests
        if (_uiState.value is InsightsUiState.Loading) return

        _uiState.value = InsightsUiState.Loading

        viewModelScope.launch {
            try {
                // 1. Build Context
                val context = HealthContextBuilder.buildContext(
                    user, vitals, medicines, reports, foods, steps, hr, calories
                )

                if (context == null) {
                    _uiState.value = InsightsUiState.Error("Please log in to generate insights.")
                    return@launch
                }

                currentContextStr = context.toPromptString()

                // 2. Fetch from AI Repository
                val result = repository.analyzeHealthContext(context.toPromptString())

                // 3. Update State
                if (result.summary.startsWith("Analysis failed")) {
                    _uiState.value = InsightsUiState.Error(result.summary)
                } else {
                    _uiState.value = InsightsUiState.Success(result)
                }
            } catch (e: Exception) {
                _uiState.value = InsightsUiState.Error(e.message ?: "Unknown error occurred.")
            }
        }
    }

    suspend fun sendChatMessage(history: List<Pair<String, String>>, newMessage: String): String {
        return repository.sendChatMessage(history, newMessage, currentContextStr)
    }
}
