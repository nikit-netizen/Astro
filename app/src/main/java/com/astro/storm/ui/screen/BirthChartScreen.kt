package com.astro.storm.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.DivisionalChartData
import com.astro.storm.ui.chart.ChartRenderer
import com.astro.storm.ui.components.FullScreenChartDialog
import com.astro.storm.ui.components.HouseDetailDialog
import com.astro.storm.ui.components.NakshatraDetailDialog
import com.astro.storm.ui.components.PlanetDetailDialog
import com.astro.storm.ui.screen.chartdetail.tabs.ChartTabContent
import com.astro.storm.ui.theme.AppTheme
import com.astro.storm.ui.viewmodel.ChartViewModel

/**
 * Birth Chart Screen - Standalone screen for viewing the complete Vedic birth chart
 *
 * Features:
 * - Multiple divisional chart views (D1-D60)
 * - Interactive chart with tap-to-expand functionality
 * - Planetary positions with detailed information
 * - House cusps and astronomical data
 * - Birth information display
 * - Full-screen chart viewing with export options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthChartScreen(
    chart: VedicChart?,
    viewModel: ChartViewModel,
    onBack: () -> Unit
) {
    if (chart == null) {
        EmptyChartScreen(
            title = "Birth Chart",
            message = "No chart data available. Please select or create a profile first.",
            onBack = onBack
        )
        return
    }

    val context = LocalContext.current
    val chartRenderer = remember { ChartRenderer() }

    // Dialog states
    var showFullScreenChart by remember { mutableStateOf(false) }
    var fullScreenChartTitle by remember { mutableStateOf("Lagna") }
    var fullScreenDivisionalData by remember { mutableStateOf<DivisionalChartData?>(null) }
    var selectedPlanetPosition by remember { mutableStateOf<PlanetPosition?>(null) }
    var selectedNakshatra by remember { mutableStateOf<Pair<Nakshatra, Int>?>(null) }
    var selectedHouse by remember { mutableStateOf<Int?>(null) }

    // Render dialogs
    if (showFullScreenChart) {
        FullScreenChartDialog(
            chart = chart,
            chartRenderer = chartRenderer,
            chartTitle = fullScreenChartTitle,
            divisionalChartData = fullScreenDivisionalData,
            onDismiss = { showFullScreenChart = false }
        )
    }

    selectedPlanetPosition?.let { position ->
        PlanetDetailDialog(
            planetPosition = position,
            chart = chart,
            onDismiss = { selectedPlanetPosition = null }
        )
    }

    selectedNakshatra?.let { (nakshatra, pada) ->
        NakshatraDetailDialog(
            nakshatra = nakshatra,
            pada = pada,
            onDismiss = { selectedNakshatra = null }
        )
    }

    selectedHouse?.let { houseNum ->
        val houseCusp = if (houseNum <= chart.houseCusps.size) chart.houseCusps[houseNum - 1] else 0.0
        val planetsInHouse = chart.planetPositions.filter { it.house == houseNum }
        HouseDetailDialog(
            houseNumber = houseNum,
            houseCusp = houseCusp,
            planetsInHouse = planetsInHouse,
            chart = chart,
            onDismiss = { selectedHouse = null }
        )
    }

    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            BirthChartTopBar(
                chartName = chart.birthData.name,
                onBack = onBack,
                onExport = {
                    viewModel.copyChartToClipboard(chart)
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
            ChartTabContent(
                chart = chart,
                chartRenderer = chartRenderer,
                context = context,
                onChartClick = { title, divisionalData ->
                    fullScreenChartTitle = title
                    fullScreenDivisionalData = divisionalData
                    showFullScreenChart = true
                },
                onPlanetClick = { selectedPlanetPosition = it },
                onHouseClick = { selectedHouse = it }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthChartTopBar(
    chartName: String,
    onBack: () -> Unit,
    onExport: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Birth Chart",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
                Text(
                    text = chartName,
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
            IconButton(onClick = onExport) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Export",
                    tint = AppTheme.TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.ScreenBackground
        )
    )
}

/**
 * Empty state screen for when no chart data is available
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmptyChartScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.ScreenBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.ScreenBackground),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = androidx.compose.ui.unit.dp.times(32))
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
