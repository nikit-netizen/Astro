package com.astro.storm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DashaCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

sealed class DashaUiState {
    data object Loading : DashaUiState()
    data class Success(val timeline: DashaCalculator.DashaTimeline) : DashaUiState()
    data class Error(val message: String) : DashaUiState()
    data object Idle : DashaUiState()
}

class DashaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DashaUiState>(DashaUiState.Idle)
    val uiState: StateFlow<DashaUiState> = _uiState.asStateFlow()

    private val _scrollToTodayEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTodayEvent: SharedFlow<Unit> = _scrollToTodayEvent.asSharedFlow()

    private var calculationJob: Job? = null

    private val cache = AtomicReference<CachedTimeline?>(null)

    private data class CachedTimeline(
        val chartKey: String,
        val timeline: DashaCalculator.DashaTimeline
    )

    fun loadDashaTimeline(chart: VedicChart?) {
        if (chart == null) {
            _uiState.value = DashaUiState.Idle
            return
        }

        val chartKey = generateChartKey(chart)

        cache.get()?.let { cached ->
            if (cached.chartKey == chartKey) {
                _uiState.value = DashaUiState.Success(cached.timeline)
                return
            }
        }

        val currentState = _uiState.value
        if (currentState is DashaUiState.Loading) {
            return
        }

        calculationJob?.cancel()
        _uiState.value = DashaUiState.Loading

        calculationJob = viewModelScope.launch {
            try {
                val timeline = withContext(Dispatchers.Default) {
                    DashaCalculator.calculateDashaTimeline(chart)
                }

                cache.set(CachedTimeline(chartKey, timeline))
                _uiState.value = DashaUiState.Success(timeline)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Moon", ignoreCase = true) == true ->
                        "Unable to determine Moon's Nakshatra position. Please verify birth data."
                    e.message?.contains("birth", ignoreCase = true) == true ->
                        "Invalid birth data provided. Please check date, time, and location."
                    else ->
                        e.message ?: "Failed to calculate Dasha timeline. Please try again."
                }
                _uiState.value = DashaUiState.Error(errorMessage)
            }
        }
    }

    fun requestScrollToToday() {
        _scrollToTodayEvent.tryEmit(Unit)
    }

    fun clearCache() {
        cache.set(null)
    }

    private fun generateChartKey(chart: VedicChart): String {
        val birthData = chart.birthData
        return buildString {
            append(birthData.dateTime.toEpochSecond(java.time.ZoneOffset.UTC))
            append('|')
            append((birthData.latitude * 1_000_000).toLong())
            append('|')
            append((birthData.longitude * 1_000_000).toLong())
            append('|')
            append(chart.ayanamsaName)
        }
    }

    override fun onCleared() {
        super.onCleared()
        calculationJob?.cancel()
    }
}