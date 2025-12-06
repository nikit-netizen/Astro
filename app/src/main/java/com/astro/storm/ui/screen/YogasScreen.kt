package com.astro.storm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.font.FontWeight
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.YogaCalculator
import com.astro.storm.ui.screen.chartdetail.tabs.YogasTabContent
import com.astro.storm.ui.theme.AppTheme

/**
 * Yogas Screen - Standalone screen for planetary yogas analysis
 *
 * Features:
 * - Complete yoga detection and analysis
 * - Category filtering (Raja, Dhana, Mahapurusha, Nabhasa, Chandra, Solar, Special, Negative)
 * - Yoga strength indicators and percentages
 * - Detailed yoga descriptions and effects
 * - Sanskrit names and activation periods
 * - Cancellation/mitigation factors for negative yogas
 * - Overall yoga strength summary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YogasScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    if (chart == null) {
        EmptyChartScreen(
            title = "Yogas",
            message = "No chart data available. Please select or create a profile first.",
            onBack = onBack
        )
        return
    }

    val yogaAnalysis = remember(chart) {
        YogaCalculator.calculateYogas(chart)
    }

    var showInfoDialog by remember { mutableStateOf(false) }

    // Yoga info dialog
    if (showInfoDialog) {
        YogaInfoDialog(
            onDismiss = { showInfoDialog = false }
        )
    }

    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            YogasTopBar(
                chartName = chart.birthData.name,
                yogaCount = yogaAnalysis.allYogas.size,
                onBack = onBack,
                onInfoClick = { showInfoDialog = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.ScreenBackground)
        ) {
            YogasTabContent(chart = chart)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YogasTopBar(
    chartName: String,
    yogaCount: Int,
    onBack: () -> Unit,
    onInfoClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Yogas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
                Text(
                    text = "$yogaCount yogas detected • $chartName",
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
            IconButton(onClick = onInfoClick) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Yoga Information",
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
 * Information dialog explaining yogas
 */
@Composable
private fun YogaInfoDialog(
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "About Vedic Yogas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Yogas are special planetary combinations in Vedic astrology that indicate specific life outcomes and characteristics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.TextSecondary
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(vertical = androidx.compose.ui.unit.dp.times(8))
                )
                Text(
                    text = "Yoga Categories:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.AccentGold
                )
                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier.padding(vertical = androidx.compose.ui.unit.dp.times(4))
                )
                YogaCategoryInfo("Raja Yoga", "Power, authority, leadership")
                YogaCategoryInfo("Dhana Yoga", "Wealth and prosperity")
                YogaCategoryInfo("Mahapurusha", "Exceptional personality traits")
                YogaCategoryInfo("Nabhasa", "Chart patterns and configurations")
                YogaCategoryInfo("Chandra", "Moon-based combinations")
                YogaCategoryInfo("Solar", "Sun-based combinations")
                YogaCategoryInfo("Special", "Unique beneficial combinations")
                YogaCategoryInfo("Negative", "Challenging combinations")
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(
                    text = "Got it",
                    color = AppTheme.AccentPrimary
                )
            }
        },
        containerColor = AppTheme.CardBackground,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(androidx.compose.ui.unit.dp.times(16))
    )
}

@Composable
private fun YogaCategoryInfo(
    name: String,
    description: String
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = androidx.compose.ui.unit.dp.times(2))
    ) {
        Text(
            text = "• $name: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.TextPrimary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.TextMuted
        )
    }
}
