package com.astro.storm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DashaCalculator
import com.astro.storm.ephemeris.HoroscopeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class InsightsData(
    val chart: VedicChart,
    val dashaTimeline: DashaCalculator.DashaTimeline?,
    val planetaryInfluences: List<HoroscopeCalculator.PlanetaryInfluence>,
    val todayHoroscope: HoroscopeCalculator.DailyHoroscope?,
    val tomorrowHoroscope: HoroscopeCalculator.DailyHoroscope?,
    val weeklyHoroscope: HoroscopeCalculator.WeeklyHoroscope?,
    val errors: List<InsightError> = emptyList()
)

data class InsightError(
    val type: InsightErrorType,
    val message: String
)

enum class InsightErrorType {
    TODAY_HOROSCOIPE,
    TOMORROW_HOROSCOPE,
    WEEKLY_HOROSCOPE,
    DASHA,
    GENERAL
}

sealed class InsightsUiState {
    data object Loading : InsightsUiState()
    data class Success(val data: InsightsData) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
    data object Idle : InsightsUiState()
}

class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var cachedData: InsightsData? = null

    fun loadInsights(chart: VedicChart?) {
        if (chart == null) {
            _uiState.value = InsightsUiState.Idle
            return
        }

        val today = LocalDate.now()
        if (cachedData?.chart == chart && cachedData?.todayHoroscope?.date == today && cachedData?.weeklyHoroscope != null) {
            _uiState.value = InsightsUiState.Success(cachedData!!)
            return
        }

        _uiState.value = InsightsUiState.Loading
        viewModelScope.launch {
            val calculator = HoroscopeCalculator(getApplication())
            try {
                val errors = mutableListOf<InsightError>()

                // Stage 1: Load essential data concurrently in the background
                val (dashaTimeline, todayHoroscope) = withContext(Dispatchers.Default) {
                    val dashaAsync = async {
                        try { DashaCalculator.calculateDashaTimeline(chart) }
                        catch (e: Exception) {
                            errors.add(InsightError(InsightErrorType.DASHA, "Dasha calculation failed"))
                            null
                        }
                    }
                    val todayAsync = async {
                        try { calculator.calculateDailyHoroscope(chart, today) }
                        catch (e: Exception) {
                            errors.add(InsightError(InsightErrorType.TODAY_HOROSCOIPE, "Today's horoscope failed"))
                            null
                        }
                    }
                    dashaAsync.await() to todayAsync.await()
                }

                if (todayHoroscope == null || dashaTimeline == null) {
                    _uiState.value = InsightsUiState.Error("Failed to load essential insights.")
                    return@launch
                }

                val initialData = InsightsData(
                    chart = chart,
                    dashaTimeline = dashaTimeline,
                    planetaryInfluences = todayHoroscope.planetaryInfluences,
                    todayHoroscope = todayHoroscope,
                    tomorrowHoroscope = null,
                    weeklyHoroscope = null,
                    errors = errors
                )
                _uiState.value = InsightsUiState.Success(initialData)

                // Stage 2: Load secondary data concurrently and update UI
                supervisorScope {
                    val tomorrowDeferred = async(Dispatchers.Default) {
                        try {
                            calculator.calculateDailyHoroscope(chart, today.plusDays(1))
                        } catch (e: Exception) {
                            errors.add(InsightError(InsightErrorType.TOMORROW_HOROSCOPE, "Failed to load tomorrow's horoscope."))
                            null
                        }
                    }

                    val weeklyDeferred = async(Dispatchers.Default) {
                        try {
                            calculator.calculateWeeklyHoroscope(chart, today)
                        } catch (e: Exception) {
                            errors.add(InsightError(InsightErrorType.WEEKLY_HOROSCOPE, "Failed to load weekly horoscope."))
                            null
                        }
                    }

                    val tomorrowHoroscope = tomorrowDeferred.await()
                    val weeklyHoroscope = weeklyDeferred.await()

                    val finalData = initialData.copy(
                        tomorrowHoroscope = tomorrowHoroscope,
                        weeklyHoroscope = weeklyHoroscope,
                        errors = errors
                    )

                    cachedData = finalData
                    _uiState.value = InsightsUiState.Success(finalData)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = InsightsUiState.Error(e.message ?: "An unexpected error occurred")
            } finally {
                calculator.close()
            }
        }
    }
}
