package com.astro.storm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DashaCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class DashaUiState {
    data object Loading : DashaUiState()
    data class Success(val timeline: DashaCalculator.DashaTimeline) : DashaUiState()
    data class Error(val message: String) : DashaUiState()
    data object Idle : DashaUiState()
}

class DashaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashaUiState>(DashaUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun loadDashaTimeline(chart: VedicChart?) {
        if (chart == null) {
            _uiState.value = DashaUiState.Idle
            return
        }

        _uiState.value = DashaUiState.Loading
        viewModelScope.launch {
            try {
                val timeline = withContext(Dispatchers.Default) {
                    DashaCalculator.calculateDashaTimeline(chart)
                }
                _uiState.value = DashaUiState.Success(timeline)
            } catch (e: Exception) {
                _uiState.value = DashaUiState.Error(e.message ?: "Failed to calculate Dasha timeline.")
            }
        }
    }
}
