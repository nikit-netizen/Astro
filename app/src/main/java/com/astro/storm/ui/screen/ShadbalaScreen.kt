package com.astro.storm.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.StringKeyDosha
import com.astro.storm.data.localization.StringKeyMatch
import com.astro.storm.data.localization.StringResources
import com.astro.storm.data.localization.currentLanguage
import com.astro.storm.data.localization.stringResource
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.PlanetaryShadbala
import com.astro.storm.ephemeris.ShadbalaAnalysis
import com.astro.storm.ephemeris.ShadbalaCalculator
import com.astro.storm.ephemeris.StrengthRating
import com.astro.storm.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Shadbala Analysis Screen
 *
 * Comprehensive six-fold planetary strength analysis based on BPHS (Brihat Parasara Hora Shastra):
 * 1. Sthana Bala - Positional strength
 * 2. Dig Bala - Directional strength
 * 3. Kala Bala - Temporal strength
 * 4. Chesta Bala - Motional strength
 * 5. Naisargika Bala - Natural strength
 * 6. Drik Bala - Aspectual strength
 *
 * Each planet requires a minimum strength (in Rupas) to be considered functionally strong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadbalaScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    if (chart == null) {
        EmptyChartScreen(
            title = stringResource(StringKeyDosha.SHADBALA_TITLE),
            message = stringResource(StringKey.NO_PROFILE_MESSAGE),
            onBack = onBack
        )
        return
    }

    val language = currentLanguage()
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlanet by remember { mutableStateOf<Planet?>(null) }
    var isCalculating by remember { mutableStateOf(true) }
    var shadbalaAnalysis by remember { mutableStateOf<ShadbalaAnalysis?>(null) }

    val tabs = listOf(
        stringResource(StringKeyDosha.SHADBALA_OVERVIEW),
        stringResource(StringKeyDosha.SHADBALA_DETAILS),
        stringResource(StringKeyDosha.SHADBALA_COMPARISON)
    )

    // Calculate Shadbala
    LaunchedEffect(chart) {
        isCalculating = true
        delay(300) // Brief delay for smooth UI
        try {
            shadbalaAnalysis = withContext(Dispatchers.Default) {
                ShadbalaCalculator.calculateShadbala(chart)
            }
        } catch (e: Exception) {
            // Handle error silently, analysis will be null
        }
        isCalculating = false
    }

    if (showInfoDialog) {
        ShadbalaInfoDialog(onDismiss = { showInfoDialog = false })
    }

    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(StringKeyDosha.SHADBALA_TITLE),
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary,
                            fontSize = 18.sp
                        )
                        Text(
                            chart.birthData.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(StringKey.BTN_BACK),
                            tint = AppTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(StringKeyDosha.SHADBALA_INFO_TITLE),
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
        if (isCalculating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = AppTheme.AccentPrimary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(StringKeyDosha.SHADBALA_CALCULATING),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.TextMuted
                    )
                }
            }
        } else if (shadbalaAnalysis == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Failed to calculate Shadbala",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.ErrorColor
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(AppTheme.ScreenBackground),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Tab selector
                item {
                    ShadbalaTabSelector(
                        tabs = tabs,
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                // Tab content
                when (selectedTab) {
                    0 -> item {
                        ShadbalaOverviewTab(
                            analysis = shadbalaAnalysis!!,
                            language = language
                        )
                    }
                    1 -> item {
                        ShadbalaDetailsTab(
                            analysis = shadbalaAnalysis!!,
                            selectedPlanet = selectedPlanet,
                            onSelectPlanet = { selectedPlanet = if (selectedPlanet == it) null else it },
                            language = language
                        )
                    }
                    2 -> item {
                        ShadbalaComparisonTab(
                            analysis = shadbalaAnalysis!!,
                            language = language
                        )
                    }
                }
            }
        }
    }
}

// ============================================
// UI Components
// ============================================

@Composable
private fun ShadbalaTabSelector(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs.size) { index ->
            val isSelected = selectedTab == index
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                label = {
                    Text(
                        tabs[index],
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.AccentPrimary.copy(alpha = 0.15f),
                    selectedLabelColor = AppTheme.AccentPrimary,
                    containerColor = AppTheme.ChipBackground,
                    labelColor = AppTheme.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun ShadbalaOverviewTab(
    analysis: ShadbalaAnalysis,
    language: Language
) {
    val overallProgress by animateFloatAsState(
        targetValue = (analysis.overallStrengthScore / 150.0).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "overall_progress"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        // Overall Strength Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(StringKeyDosha.SHADBALA_OVERALL_STRENGTH),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        color = AppTheme.ChipBackground,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 14.dp,
                        color = getStrengthColor(analysis.overallStrengthScore),
                        strokeCap = StrokeCap.Round
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", analysis.overallStrengthScore),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.TextPrimary
                        )
                        Text(
                            text = "%",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Strong/Weak count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StrengthCountChip(
                        label = stringResource(StringKeyDosha.SHADBALA_STRONG_COUNT, analysis.getStrongPlanets().size),
                        count = analysis.getStrongPlanets().size,
                        color = AppTheme.SuccessColor
                    )
                    StrengthCountChip(
                        label = stringResource(StringKeyDosha.SHADBALA_WEAK_COUNT, analysis.getWeakPlanets().size),
                        count = analysis.getWeakPlanets().size,
                        color = AppTheme.WarningColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strongest and Weakest Planet
        Row(modifier = Modifier.fillMaxWidth()) {
            StrongestWeakestCard(
                title = stringResource(StringKeyDosha.SHADBALA_STRONGEST_PLANET),
                planet = analysis.strongestPlanet,
                strength = analysis.planetaryStrengths[analysis.strongestPlanet],
                isStrong = true,
                modifier = Modifier.weight(1f),
                language = language
            )
            Spacer(modifier = Modifier.width(12.dp))
            StrongestWeakestCard(
                title = stringResource(StringKeyDosha.SHADBALA_WEAKEST_PLANET),
                planet = analysis.weakestPlanet,
                strength = analysis.planetaryStrengths[analysis.weakestPlanet],
                isStrong = false,
                modifier = Modifier.weight(1f),
                language = language
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Summary of all planets
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.SHADBALA_CHART_ANALYSIS),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                analysis.getPlanetsByStrength().forEach { shadbala ->
                    PlanetStrengthRow(shadbala = shadbala, language = language)
                    if (shadbala != analysis.getPlanetsByStrength().last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthCountChip(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (count == 1) "planet" else "planets",
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StrongestWeakestCard(
    title: String,
    planet: Planet,
    strength: PlanetaryShadbala?,
    isStrong: Boolean,
    modifier: Modifier = Modifier,
    language: Language
) {
    val accentColor = if (isStrong) AppTheme.SuccessColor else AppTheme.WarningColor

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    planet.symbol,
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                planet.getLocalizedName(language),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )

            strength?.let {
                Text(
                    String.format("%.2f R", it.totalRupas),
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
private fun PlanetStrengthRow(
    shadbala: PlanetaryShadbala,
    language: Language
) {
    val progress by animateFloatAsState(
        targetValue = (shadbala.percentageOfRequired / 150.0).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(800),
        label = "progress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppTheme.getPlanetColor(shadbala.planet).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                shadbala.planet.symbol,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.getPlanetColor(shadbala.planet)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    shadbala.planet.getLocalizedName(language),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.TextPrimary
                )
                Text(
                    String.format("%.1f%%", shadbala.percentageOfRequired),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = getStrengthColor(shadbala.percentageOfRequired)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = getStrengthColor(shadbala.percentageOfRequired),
                trackColor = AppTheme.ChipBackground
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            if (shadbala.isStrong) Icons.Filled.CheckCircle else Icons.Outlined.Warning,
            contentDescription = null,
            tint = if (shadbala.isStrong) AppTheme.SuccessColor else AppTheme.WarningColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ShadbalaDetailsTab(
    analysis: ShadbalaAnalysis,
    selectedPlanet: Planet?,
    onSelectPlanet: (Planet) -> Unit,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Planet selector chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(analysis.planetaryStrengths.keys.toList()) { planet ->
                val isSelected = selectedPlanet == planet
                val shadbala = analysis.planetaryStrengths[planet]

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPlanet(planet) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(planet.symbol)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(planet.getLocalizedName(language))
                        }
                    },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                if (shadbala?.isStrong == true) Icons.Filled.CheckCircle else Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = if (shadbala?.isStrong == true) AppTheme.SuccessColor else AppTheme.WarningColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.getPlanetColor(planet).copy(alpha = 0.15f),
                        selectedLabelColor = AppTheme.getPlanetColor(planet),
                        containerColor = AppTheme.ChipBackground,
                        labelColor = AppTheme.TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Planet details
        val shadbala = if (selectedPlanet != null) {
            analysis.planetaryStrengths[selectedPlanet]
        } else {
            analysis.getPlanetsByStrength().firstOrNull()
        }

        shadbala?.let {
            PlanetShadbalaDetailCard(shadbala = it, language = language)
        }
    }
}

@Composable
private fun PlanetShadbalaDetailCard(
    shadbala: PlanetaryShadbala,
    language: Language
) {
    val planet = shadbala.planet
    val accentColor = AppTheme.getPlanetColor(planet)

    Column {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = accentColor.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            planet.getLocalizedName(language),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.TextPrimary
                        )
                        Text(
                            stringResource(StringKeyDosha.SHADBALA_PLANET_ANALYSIS, planet.displayName),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            planet.symbol,
                            style = MaterialTheme.typography.headlineMedium,
                            color = accentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Strength summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%.2f", shadbala.totalRupas),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.TextPrimary
                        )
                        Text(
                            stringResource(StringKeyDosha.SHADBALA_RUPAS),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.TextMuted
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = AppTheme.BorderColor
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%.2f", shadbala.requiredRupas),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.TextMuted
                        )
                        Text(
                            stringResource(StringKeyDosha.SHADBALA_REQUIRED),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.TextMuted
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.height(40.dp),
                        color = AppTheme.BorderColor
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%.1f%%", shadbala.percentageOfRequired),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = getStrengthColor(shadbala.percentageOfRequired)
                        )
                        Text(
                            stringResource(StringKeyDosha.SHADBALA_PERCENTAGE),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Rating chip
                Surface(
                    color = getStrengthColor(shadbala.percentageOfRequired).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (shadbala.isStrong) Icons.Filled.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = getStrengthColor(shadbala.percentageOfRequired),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            getLocalizedStrengthRating(shadbala.strengthRating, language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = getStrengthColor(shadbala.percentageOfRequired)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Six-fold Breakdown
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.SHADBALA_BREAKDOWN),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Sthana Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_STHANA_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_STHANA_BALA_DESC),
                    value = shadbala.sthanaBala.total,
                    icon = Icons.Filled.Place
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Dig Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_DIG_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_DIG_BALA_DESC),
                    value = shadbala.digBala,
                    icon = Icons.Filled.Explore
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Kala Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_KALA_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_KALA_BALA_DESC),
                    value = shadbala.kalaBala.total,
                    icon = Icons.Filled.Schedule
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Chesta Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_CHESTA_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_CHESTA_BALA_DESC),
                    value = shadbala.chestaBala,
                    icon = Icons.Filled.TrendingUp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Naisargika Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_NAISARGIKA_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_NAISARGIKA_BALA_DESC),
                    value = shadbala.naisargikaBala,
                    icon = Icons.Filled.Nature
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Drik Bala
                BalaBreakdownItem(
                    title = stringResource(StringKeyDosha.SHADBALA_DRIK_BALA),
                    subtitle = stringResource(StringKeyDosha.SHADBALA_DRIK_BALA_DESC),
                    value = shadbala.drikBala,
                    icon = Icons.Filled.Visibility
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed sub-components for Sthana Bala
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${stringResource(StringKeyDosha.SHADBALA_STHANA_BALA)} ${stringResource(StringKeyDosha.SHADBALA_BREAKDOWN)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_UCCHA_BALA), shadbala.sthanaBala.ucchaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_SAPTAVARGAJA_BALA), shadbala.sthanaBala.saptavargajaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_OJHAYUGMA_BALA), shadbala.sthanaBala.ojhayugmarasyamsaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_KENDRADI_BALA), shadbala.sthanaBala.kendradiBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_DREKKANA_BALA), shadbala.sthanaBala.drekkanaBala)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed sub-components for Kala Bala
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${stringResource(StringKeyDosha.SHADBALA_KALA_BALA)} ${stringResource(StringKeyDosha.SHADBALA_BREAKDOWN)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_NATHONNATHA_BALA), shadbala.kalaBala.nathonnathaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_PAKSHA_BALA), shadbala.kalaBala.pakshaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_TRIBHAGA_BALA), shadbala.kalaBala.tribhagaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_HORA_BALA), shadbala.kalaBala.horaAdiBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_AYANA_BALA), shadbala.kalaBala.ayanaBala)
                SubBalaRow(stringResource(StringKeyDosha.SHADBALA_YUDDHA_BALA), shadbala.kalaBala.yuddhaBala)
            }
        }
    }
}

@Composable
private fun BalaBreakdownItem(
    title: String,
    subtitle: String,
    value: Double,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AppTheme.AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppTheme.AccentPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextMuted
            )
        }

        Text(
            String.format("%.2f", value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.AccentPrimary
        )
    }
}

@Composable
private fun SubBalaRow(label: String, value: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.TextMuted
        )
        Text(
            String.format("%.2f", value),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.TextSecondary
        )
    }
}

@Composable
private fun ShadbalaComparisonTab(
    analysis: ShadbalaAnalysis,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Bar chart comparison
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.SHADBALA_TOTAL_STRENGTH),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val sortedPlanets = analysis.getPlanetsByStrength()
                val maxRupas = sortedPlanets.maxOfOrNull { it.totalRupas } ?: 1.0

                sortedPlanets.forEach { shadbala ->
                    ComparisonBarRow(
                        shadbala = shadbala,
                        maxValue = maxRupas,
                        language = language
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Six Bala comparison
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Six-fold Strength Comparison",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Planet",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextMuted,
                        modifier = Modifier.width(60.dp)
                    )
                    Text("Sthana", style = MaterialTheme.typography.labelSmall, color = AppTheme.TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Dig", style = MaterialTheme.typography.labelSmall, color = AppTheme.TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Kala", style = MaterialTheme.typography.labelSmall, color = AppTheme.TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Chesta", style = MaterialTheme.typography.labelSmall, color = AppTheme.TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = AppTheme.TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                // Data rows
                analysis.getPlanetsByStrength().forEach { shadbala ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            shadbala.planet.symbol,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.getPlanetColor(shadbala.planet),
                            modifier = Modifier.width(60.dp)
                        )
                        Text(
                            String.format("%.0f", shadbala.sthanaBala.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            String.format("%.0f", shadbala.digBala),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            String.format("%.0f", shadbala.kalaBala.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            String.format("%.0f", shadbala.chestaBala),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            String.format("%.1f", shadbala.totalRupas),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (shadbala.isStrong) AppTheme.SuccessColor else AppTheme.WarningColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonBarRow(
    shadbala: PlanetaryShadbala,
    maxValue: Double,
    language: Language
) {
    val progress by animateFloatAsState(
        targetValue = (shadbala.totalRupas / maxValue).toFloat(),
        animationSpec = tween(800),
        label = "bar_progress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Planet symbol
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppTheme.getPlanetColor(shadbala.planet).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                shadbala.planet.symbol,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.getPlanetColor(shadbala.planet)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AppTheme.ChipBackground)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AppTheme.getPlanetColor(shadbala.planet).copy(alpha = 0.6f),
                                AppTheme.getPlanetColor(shadbala.planet)
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Value
        Text(
            String.format("%.2f R", shadbala.totalRupas),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (shadbala.isStrong) AppTheme.SuccessColor else AppTheme.WarningColor,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ShadbalaInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(StringKeyDosha.SHADBALA_INFO_TITLE),
                fontWeight = FontWeight.Bold,
                color = AppTheme.TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    stringResource(StringKeyDosha.SHADBALA_INFO_DESC),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "The six sources of strength:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                listOf(
                    "1. Sthana Bala - Positional strength based on exaltation, own sign, etc.",
                    "2. Dig Bala - Directional strength based on house placement",
                    "3. Kala Bala - Temporal strength from time of birth",
                    "4. Chesta Bala - Motional strength from planetary motion",
                    "5. Naisargika Bala - Natural inherent strength",
                    "6. Drik Bala - Strength from aspects received"
                ).forEach { item ->
                    Text(
                        item,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.TextMuted,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(StringKey.BTN_CLOSE), color = AppTheme.AccentGold)
            }
        },
        containerColor = AppTheme.CardBackground
    )
}

// ============================================
// Helper Functions
// ============================================

private fun getStrengthColor(percentage: Double): Color {
    return when {
        percentage >= 130 -> AppTheme.SuccessColor
        percentage >= 100 -> Color(0xFF8BC34A)
        percentage >= 85 -> AppTheme.AccentPrimary
        percentage >= 70 -> AppTheme.WarningColor
        else -> AppTheme.ErrorColor
    }
}

private fun getLocalizedStrengthRating(rating: StrengthRating, language: Language): String {
    return when (rating) {
        StrengthRating.EXTREMELY_WEAK -> StringResources.get(StringKeyMatch.SHADBALA_EXTREMELY_WEAK, language)
        StrengthRating.WEAK -> StringResources.get(StringKeyMatch.SHADBALA_WEAK, language)
        StrengthRating.BELOW_AVERAGE -> StringResources.get(StringKeyMatch.SHADBALA_BELOW_AVERAGE, language)
        StrengthRating.AVERAGE -> StringResources.get(StringKeyMatch.SHADBALA_AVERAGE, language)
        StrengthRating.ABOVE_AVERAGE -> StringResources.get(StringKeyMatch.SHADBALA_ABOVE_AVERAGE, language)
        StrengthRating.STRONG -> StringResources.get(StringKeyMatch.SHADBALA_STRONG, language)
        StrengthRating.VERY_STRONG -> StringResources.get(StringKeyMatch.SHADBALA_VERY_STRONG, language)
        StrengthRating.EXTREMELY_STRONG -> StringResources.get(StringKeyMatch.SHADBALA_EXTREMELY_STRONG, language)
    }
}
