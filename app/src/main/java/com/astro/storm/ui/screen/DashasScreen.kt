package com.astro.storm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DashaCalculator
import com.astro.storm.ui.screen.chartdetail.tabs.DashasTabContent
import com.astro.storm.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dashas Screen - Standalone screen for Vimshottari Dasha planetary periods
 *
 * Features:
 * - Current period display (Mahadasha, Antardasha, Pratyantardasha, Sookshmadasha)
 * - Birth nakshatra and lord information
 * - Dasha sandhi (transition) alerts with upcoming 90-day warnings
 * - Complete 120-year Vimshottari timeline
 * - Expandable Mahadasha cards with Antardasha sub-periods
 * - Progress indicators with remaining time calculations
 * - Period insights and interpretations
 * - Educational information about Vimshottari Dasha system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashasScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    if (chart == null) {
        EmptyChartScreen(
            title = "Dashas",
            message = "No chart data available. Please select or create a profile first.",
            onBack = onBack
        )
        return
    }

    val dashaTimeline = remember(chart) {
        DashaCalculator.calculateDashaTimeline(chart)
    }

    val currentPeriodSummary = remember(dashaTimeline) {
        buildString {
            dashaTimeline.currentMahadasha?.let { md ->
                append(md.planet.displayName)
                dashaTimeline.currentAntardasha?.let { ad ->
                    append(" → ${ad.planet.displayName}")
                }
            } ?: append("Calculating...")
        }
    }

    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            DashasTopBar(
                chartName = chart.birthData.name,
                currentPeriod = currentPeriodSummary,
                onBack = onBack,
                onJumpToToday = {
                    // Scroll to current period (handled by content)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.ScreenBackground)
        ) {
            DashasTabContent(chart = chart)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashasTopBar(
    chartName: String,
    currentPeriod: String,
    onBack: () -> Unit,
    onJumpToToday: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Dashas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
                Text(
                    text = "$currentPeriod • $chartName",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.TextMuted
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTheme.TextPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onJumpToToday) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Jump to Today",
                    tint = AppTheme.TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.ScreenBackground
        )
    )
}
