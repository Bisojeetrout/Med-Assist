package com.example.swasthya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthya.data.api.RetrofitClient
import com.example.swasthya.data.model.BloodStockItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BloodStockState {
    object Loading : BloodStockState()
    data class Success(val data: List<BloodStockItem>) : BloodStockState()
    data class Error(val message: String) : BloodStockState()
}

class BloodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<BloodStockState>(BloodStockState.Loading)
    val uiState: StateFlow<BloodStockState> = _uiState

    init {
        fetchBloodStock()
    }

    fun fetchBloodStock() {
        _uiState.value = BloodStockState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getBloodStock()
                if (response.status == "success" && response.data != null) {
                    _uiState.value = BloodStockState.Success(response.data)
                } else {
                    _uiState.value = BloodStockState.Error(response.message ?: "Unknown error from server")
                }
            } catch (e: Exception) {
                _uiState.value = BloodStockState.Error(e.localizedMessage ?: "Failed to connect to server")
            }
        }
    }
}
