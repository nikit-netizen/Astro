package com.astro.storm.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DashaCalculator
import com.astro.storm.ephemeris.HoroscopeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.concurrent.ConcurrentLinkedQueue

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
    TODAY_HOROSCOPE,
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

    private val horoscopeCalculator: HoroscopeCalculator by lazy {
        HoroscopeCalculator(getApplication())
    }

    private var currentLoadJob: Job? = null
    private var cachedData: InsightsData? = null
    private var cachedChartId: String? = null
    private var cachedDate: LocalDate? = null

    private fun getChartId(chart: VedicChart): String {
        val birthData = chart.birthData
        return buildString {
            append(birthData.name)
            append('_')
            append(birthData.dateTime)
            append('_')
            append((birthData.latitude * 1000).toInt())
            append('_')
            append((birthData.longitude * 1000).toInt())
            append('_')
            append(birthData.timezone)
        }
    }

    private fun isCacheValid(chart: VedicChart, today: LocalDate): Boolean {
        val chartId = getChartId(chart)
        val cached = cachedData ?: return false
        return cachedChartId == chartId &&
               cachedDate == today &&
               cached.todayHoroscope != null &&
               cached.weeklyHoroscope != null
    }

    fun loadInsights(chart: VedicChart?) {
        if (chart == null) {
            _uiState.value = InsightsUiState.Idle
            return
        }

        val today = LocalDate.now()

        if (isCacheValid(chart, today)) {
            cachedData?.let {
                _uiState.value = InsightsUiState.Success(it)
                return
            }
        }

        currentLoadJob?.cancel()

        currentLoadJob = viewModelScope.launch {
            _uiState.value = InsightsUiState.Loading

            try {
                val errors = ConcurrentLinkedQueue<InsightError>()
                val chartId = getChartId(chart)

                val (dashaTimeline, todayHoroscope) = loadEssentialData(chart, today, errors)

                ensureActive()

                if (todayHoroscope == null && dashaTimeline == null) {
                    _uiState.value = InsightsUiState.Error("Failed to load essential insights. Please try again.")
                    return@launch
                }

                val initialData = InsightsData(
                    chart = chart,
                    dashaTimeline = dashaTimeline,
                    planetaryInfluences = todayHoroscope?.planetaryInfluences ?: emptyList(),
                    todayHoroscope = todayHoroscope,
                    tomorrowHoroscope = null,
                    weeklyHoroscope = null,
                    errors = errors.toList()
                )
                _uiState.value = InsightsUiState.Success(initialData)

                val (tomorrowHoroscope, weeklyHoroscope) = loadSecondaryData(chart, today, errors)

                ensureActive()

                val finalData = initialData.copy(
                    tomorrowHoroscope = tomorrowHoroscope,
                    weeklyHoroscope = weeklyHoroscope,
                    errors = errors.toList()
                )

                cachedData = finalData
                cachedChartId = chartId
                cachedDate = today

                _uiState.value = InsightsUiState.Success(finalData)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading insights", e)
                _uiState.value = InsightsUiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private suspend fun loadEssentialData(
        chart: VedicChart,
        today: LocalDate,
        errors: ConcurrentLinkedQueue<InsightError>
    ): Pair<DashaCalculator.DashaTimeline?, HoroscopeCalculator.DailyHoroscope?> {
        return withContext(Dispatchers.Default) {
            coroutineScope {
                val dashaDeferred = async {
                    try {
                        DashaCalculator.calculateDashaTimeline(chart)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Dasha calculation failed", e)
                        errors.add(InsightError(InsightErrorType.DASHA, "Dasha calculation failed: ${e.message}"))
                        null
                    }
                }

                val todayDeferred = async {
                    try {
                        horoscopeCalculator.calculateDailyHoroscope(chart, today)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Today's horoscope calculation failed", e)
                        errors.add(InsightError(InsightErrorType.TODAY_HOROSCOPE, "Today's horoscope failed: ${e.message}"))
                        null
                    }
                }

                Pair(dashaDeferred.await(), todayDeferred.await())
            }
        }
    }

    private suspend fun loadSecondaryData(
        chart: VedicChart,
        today: LocalDate,
        errors: ConcurrentLinkedQueue<InsightError>
    ): Pair<HoroscopeCalculator.DailyHoroscope?, HoroscopeCalculator.WeeklyHoroscope?> {
        return withContext(Dispatchers.Default) {
            supervisorScope {
                val tomorrowDeferred = async {
                    try {
                        horoscopeCalculator.calculateDailyHoroscope(chart, today.plusDays(1))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Tomorrow's horoscope calculation failed", e)
                        errors.add(InsightError(InsightErrorType.TOMORROW_HOROSCOPE, "Tomorrow's horoscope failed"))
                        null
                    }
                }

                val weeklyDeferred = async {
                    try {
                        horoscopeCalculator.calculateWeeklyHoroscope(chart, today)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Weekly horoscope calculation failed", e)
                        errors.add(InsightError(InsightErrorType.WEEKLY_HOROSCOPE, "Weekly horoscope failed"))
                        null
                    }
                }

                Pair(tomorrowDeferred.await(), weeklyDeferred.await())
            }
        }
    }

    fun refreshInsights(chart: VedicChart?) {
        clearCache()
        loadInsights(chart)
    }

    fun clearCache() {
        cachedData = null
        cachedChartId = null
        cachedDate = null
        horoscopeCalculator.clearCache()
    }

    override fun onCleared() {
        super.onCleared()
        currentLoadJob?.cancel()
        try {
            horoscopeCalculator.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing horoscope calculator", e)
        }
    }

    companion object {
        private const val TAG = "InsightsViewModel"
    }
}