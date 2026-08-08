package com.example.swasthya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swasthya.data.api.RetrofitClient
import com.example.swasthya.data.model.BloodStockItem
import com.example.swasthya.data.model.LocationItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BloodStockState {
    object Idle : BloodStockState()
    object Loading : BloodStockState()
    data class Success(val data: List<BloodStockItem>) : BloodStockState()
    data class Error(val message: String) : BloodStockState()
}

class BloodViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<BloodStockState>(BloodStockState.Idle)
    val uiState: StateFlow<BloodStockState> = _uiState

    private val _states = MutableStateFlow<List<LocationItem>>(emptyList())
    val states: StateFlow<List<LocationItem>> = _states

    private val _districts = MutableStateFlow<List<LocationItem>>(emptyList())
    val districts: StateFlow<List<LocationItem>> = _districts

    init {
        fetchStates()
    }

    private fun fetchStates() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getStates()
                if (response.status == "success" && response.data != null) {
                    _states.value = response.data
                }
            } catch (e: Exception) {
                // Ignore states error for now
            }
        }
    }

    fun fetchDistricts(stateCode: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getDistricts(stateCode)
                if (response.status == "success" && response.data != null) {
                    _districts.value = response.data
                } else {
                    _districts.value = emptyList()
                }
            } catch (e: Exception) {
                _districts.value = emptyList()
            }
        }
    }

    fun fetchBloodStock(stateCode: String, districtCode: String, bloodGroup: String, bloodComponent: String) {
        _uiState.value = BloodStockState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getBloodStock(stateCode, districtCode, bloodGroup, bloodComponent)
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
