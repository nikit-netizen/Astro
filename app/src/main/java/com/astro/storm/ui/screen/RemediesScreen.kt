package com.astro.storm.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.StringKeyMatch
import com.astro.storm.data.localization.StringKeyAnalysis
import com.astro.storm.data.localization.stringResource
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.RemediesCalculator
import com.astro.storm.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemediesScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var remediesResult by remember { mutableStateOf<RemediesCalculator.RemediesResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<RemediesCalculator.RemedyCategory?>(null) }
    var expandedRemedyId by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }

    val tabs = listOf(
        stringResource(StringKeyMatch.TAB_OVERVIEW),
        stringResource(StringKeyMatch.TAB_REMEDIES),
        stringResource(StringKeyMatch.TAB_PLANETS)
    )

    val overviewListState = rememberLazyListState()
    val remediesListState = rememberLazyListState()
    val planetsListState = rememberLazyListState()

    suspend fun calculateRemedies(chartData: VedicChart) {
        isLoading = true
        errorMessage = null
        try {
            withContext(Dispatchers.Default) {
                remediesResult = RemediesCalculator.calculateRemedies(chartData)
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(chart) {
        chart?.let { calculateRemedies(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchVisible,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        },
                        label = "SearchTransition"
                    ) { showSearch ->
                        if (showSearch) {
                            SearchTextField(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClear = {
                                    searchQuery = ""
                                    isSearchVisible = false
                                }
                            )
                        } else {
                            Text(
                                stringResource(StringKeyMatch.REMEDY_TITLE),
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.TextPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isSearchVisible) {
                                isSearchVisible = false
                                searchQuery = ""
                            } else {
                                onBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(StringKey.BTN_BACK),
                            tint = AppTheme.TextPrimary
                        )
                    }
                },
                actions = {
                    if (selectedTab == 1 && !isSearchVisible) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isSearchVisible = true
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(StringKeyMatch.REMEDY_SEARCH),
                                tint = AppTheme.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.ScreenBackground
                )
            )
        },
        containerColor = AppTheme.ScreenBackground
    ) { paddingValues ->
        when {
            chart == null -> {
                EmptyState(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            errorMessage != null -> {
                ErrorState(
                    message = errorMessage!!,
                    onRetry = { scope.launch { calculateRemedies(chart) } },
                    modifier = Modifier.padding(paddingValues)
                )
            }
            isLoading && remediesResult == null -> {
                LoadingState(
                    modifier = Modifier.padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = AppTheme.AccentPrimary,
                        divider = { HorizontalDivider(color = AppTheme.DividerColor.copy(alpha = 0.5f)) }
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedTab = index
                                },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selectedTab == index) AppTheme.AccentPrimary else AppTheme.TextMuted
                                    )
                                },
                                modifier = Modifier.semantics {
                                    contentDescription = "$title tab"
                                }
                            )
                        }
                    }

                    remediesResult?.let { result ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    val direction = if (targetState > initialState) {
                                        AnimatedContentTransitionScope.SlideDirection.Left
                                    } else {
                                        AnimatedContentTransitionScope.SlideDirection.Right
                                    }
                                    slideIntoContainer(direction, tween(300)) togetherWith
                                            slideOutOfContainer(direction, tween(300))
                                },
                                label = "TabContent"
                            ) { tab ->
                                when (tab) {
                                    0 -> OverviewTab(
                                        result = result,
                                        listState = overviewListState
                                    )
                                    1 -> RemediesTab(
                                        result = result,
                                        selectedCategory = selectedCategory,
                                        onCategoryChange = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedCategory = it
                                        },
                                        expandedId = expandedRemedyId,
                                        onExpandChange = { id ->
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            expandedRemedyId = if (expandedRemedyId == id) null else id
                                        },
                                        searchQuery = searchQuery,
                                        listState = remediesListState
                                    )
                                    2 -> PlanetsTab(
                                        result = result,
                                        listState = planetsListState
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                stringResource(StringKeyMatch.REMEDIES_SEARCH),
                color = AppTheme.TextMuted
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = AppTheme.CardBackground,
            unfocusedContainerColor = AppTheme.CardBackground,
            cursorColor = AppTheme.AccentPrimary
        ),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(StringKeyMatch.ACTION_CLEAR_SEARCH),
                        tint = AppTheme.TextMuted
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}

@Composable
private fun OverviewTab(
    result: RemediesCalculator.RemediesResult,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "summary") {
            SummaryCard(result)
        }

        if (result.weakestPlanets.isNotEmpty()) {
            item(key = "weak_planets") {
                WeakPlanetsCard(result.weakestPlanets)
            }
        }

        item(key = "essential_remedies") {
            EssentialRemediesPreview(result)
        }

        if (result.lifeAreaFocus.isNotEmpty()) {
            item(key = "life_area") {
                LifeAreaFocusCard(result.lifeAreaFocus)
            }
        }

        if (result.generalRecommendations.isNotEmpty()) {
            item(key = "recommendations") {
                GeneralRecommendationsCard(result.generalRecommendations)
            }
        }

        item(key = "weekly_schedule") {
            WeeklyRemedyScheduleCard(result)
        }
    }
}

@Composable
private fun SummaryCard(result: RemediesCalculator.RemediesResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(StringKeyMatch.REMEDIES_ANALYSIS),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary
                    )
                    Text(
                        result.chart.birthData.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatItem(
                        value = "${result.totalRemediesCount}",
                        label = stringResource(StringKeyMatch.REMEDIES_TOTAL)
                    )
                    StatItem(
                        value = "${result.essentialRemediesCount}",
                        label = stringResource(StringKeyMatch.REMEDIES_ESSENTIAL_COUNT),
                        color = AppTheme.WarningColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = AppTheme.DividerColor)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                result.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OverallChartHealthIndicator(result)
        }
    }
}

@Composable
private fun OverallChartHealthIndicator(result: RemediesCalculator.RemediesResult) {
    val totalPlanets = result.planetaryAnalyses.size
    val strongPlanets = result.planetaryAnalyses.count {
        it.strength == RemediesCalculator.PlanetaryStrength.STRONG ||
                it.strength == RemediesCalculator.PlanetaryStrength.VERY_STRONG
    }
    val healthPercentage = if (totalPlanets > 0) (strongPlanets.toFloat() / totalPlanets) * 100 else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = healthPercentage / 100f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "HealthProgress"
    )

    Surface(
        color = AppTheme.AccentPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(StringKeyMatch.REMEDIES_CHART_STRENGTH),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
                Text(
                    stringResource(StringKeyMatch.REMEDIES_PLANETS_WELL_PLACED, strongPlanets, totalPlanets),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.TextMuted
                )
            }

            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                val progressColor = when {
                    healthPercentage >= 70 -> AppTheme.SuccessColor
                    healthPercentage >= 40 -> AppTheme.WarningColor
                    else -> AppTheme.ErrorColor
                }

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.2f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )

                Text(
                    "${healthPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color = AppTheme.AccentPrimary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.TextMuted
        )
    }
}

@Composable
private fun WeakPlanetsCard(weakPlanets: List<Planet>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = AppTheme.WarningColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(StringKeyMatch.REMEDIES_PLANETS_ATTENTION),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(weakPlanets, key = { it.name }) { planet ->
                    PlanetChip(planet)
                }
            }
        }
    }
}

@Composable
private fun PlanetChip(planet: Planet) {
    val planetColor = getPlanetColor(planet)

    Surface(
        color = planetColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.semantics {
            contentDescription = "${planet.displayName} requires attention"
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(planetColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    planet.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = planetColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                planet.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = planetColor
            )
        }
    }
}

@Composable
private fun EssentialRemediesPreview(result: RemediesCalculator.RemediesResult) {
    val essentialRemedies = remember(result) {
        result.prioritizedRemedies
            .filter { it.priority == RemediesCalculator.RemedyPriority.ESSENTIAL }
            .take(3)
    }

    if (essentialRemedies.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = AppTheme.AccentGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(StringKeyMatch.REMEDIES_ESSENTIAL),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            essentialRemedies.forEach { remedy ->
                EssentialRemedyRow(remedy)
            }
        }
    }
}

@Composable
private fun EssentialRemedyRow(remedy: RemediesCalculator.Remedy) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            getCategoryIcon(remedy.category),
            contentDescription = null,
            tint = AppTheme.AccentPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                remedy.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                remedy.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.TextMuted
            )
        }
        remedy.planet?.let { planet ->
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(getPlanetColor(planet).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    planet.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = getPlanetColor(planet)
                )
            }
        }
    }
}

@Composable
private fun WeeklyRemedyScheduleCard(result: RemediesCalculator.RemediesResult) {
    val weekDays = listOf(
        Triple(Planet.SUN, "Sunday", stringResource(StringKeyMatch.DAY_SUNDAY)),
        Triple(Planet.MOON, "Monday", stringResource(StringKeyMatch.DAY_MONDAY)),
        Triple(Planet.MARS, "Tuesday", stringResource(StringKeyMatch.DAY_TUESDAY)),
        Triple(Planet.MERCURY, "Wednesday", stringResource(StringKeyMatch.DAY_WEDNESDAY)),
        Triple(Planet.JUPITER, "Thursday", stringResource(StringKeyMatch.DAY_THURSDAY)),
        Triple(Planet.VENUS, "Friday", stringResource(StringKeyMatch.DAY_FRIDAY)),
        Triple(Planet.SATURN, "Saturday", stringResource(StringKeyMatch.DAY_SATURDAY))
    )

    val remediesByPlanet = remember(result) {
        weekDays.associate { (planet, _, _) ->
            planet to result.remedies.filter { it.planet == planet }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = AppTheme.AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(StringKeyMatch.REMEDIES_WEEKLY_SCHEDULE),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                stringResource(StringKeyMatch.REMEDIES_WEEKLY_SCHEDULE_DESC),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weekDays, key = { it.first }) { (planet, _, localizedDay) ->
                    val remedyCount = remediesByPlanet[planet]?.size ?: 0
                    WeekDayChip(
                        day = localizedDay.take(3),
                        planet = planet,
                        remedyCount = remedyCount
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekDayChip(
    day: String,
    planet: Planet,
    remedyCount: Int
) {
    val planetColor = getPlanetColor(planet)

    Surface(
        color = if (remedyCount > 0) planetColor.copy(alpha = 0.15f) else AppTheme.CardBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                day,
                style = MaterialTheme.typography.labelSmall,
                color = if (remedyCount > 0) planetColor else AppTheme.TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                planet.symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (remedyCount > 0) planetColor else AppTheme.TextSubtle
            )
            if (remedyCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    color = planetColor,
                    shape = CircleShape
                ) {
                    Text(
                        "$remedyCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LifeAreaFocusCard(lifeAreaFocus: Map<String, List<RemediesCalculator.Remedy>>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(StringKeyMatch.REMEDIES_LIFE_AREA_FOCUS),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            lifeAreaFocus.forEach { (area, remedies) ->
                LifeAreaRow(area = area, remedyCount = remedies.size)
            }
        }
    }
}

@Composable
private fun LifeAreaRow(area: String, remedyCount: Int) {
    val areaColor = getLifeAreaColor(area)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                getLifeAreaIcon(area),
                contentDescription = null,
                tint = areaColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                area,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextPrimary
            )
        }
        Surface(
            color = areaColor.copy(alpha = 0.15f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                "$remedyCount",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = areaColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun GeneralRecommendationsCard(recommendations: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.InfoColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = AppTheme.InfoColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(StringKeyMatch.REMEDIES_GENERAL_RECOMMENDATIONS),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            recommendations.forEach { rec ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = AppTheme.InfoColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        rec,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RemediesTab(
    result: RemediesCalculator.RemediesResult,
    selectedCategory: RemediesCalculator.RemedyCategory?,
    onCategoryChange: (RemediesCalculator.RemedyCategory?) -> Unit,
    expandedId: String?,
    onExpandChange: (String) -> Unit,
    searchQuery: String,
    listState: LazyListState
) {
    val filteredRemedies = remember(result, selectedCategory, searchQuery) {
        val baseList = if (selectedCategory == null) {
            result.prioritizedRemedies
        } else {
            result.prioritizedRemedies.filter { it.category == selectedCategory }
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { remedy ->
                remedy.title.contains(searchQuery, ignoreCase = true) ||
                        remedy.description.contains(searchQuery, ignoreCase = true) ||
                        remedy.planet?.displayName?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    val categoryCounts = remember(result) {
        result.remedies.groupBy { it.category }.mapValues { it.value.size }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item(key = "category_filter") {
            CategoryFilter(
                selectedCategory = selectedCategory,
                onCategoryChange = onCategoryChange,
                categoryCounts = categoryCounts
            )
        }

        if (filteredRemedies.isEmpty()) {
            item(key = "no_results") {
                NoResultsState(searchQuery = searchQuery)
            }
        } else {
            items(
                items = filteredRemedies,
                key = { it.id }
            ) { remedy ->
                RemedyCard(
                    remedy = remedy,
                    isExpanded = expandedId == remedy.id,
                    onExpand = { onExpandChange(remedy.id) }
                )
            }
        }
    }
}

@Composable
private fun NoResultsState(searchQuery: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = AppTheme.TextSubtle,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (searchQuery.isNotBlank()) stringResource(StringKeyMatch.REMEDIES_NO_RESULTS_SEARCH, searchQuery)
                else stringResource(StringKeyMatch.REMEDIES_NO_CATEGORY),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun CategoryFilter(
    selectedCategory: RemediesCalculator.RemedyCategory?,
    onCategoryChange: (RemediesCalculator.RemedyCategory?) -> Unit,
    categoryCounts: Map<RemediesCalculator.RemedyCategory, Int>
) {
    LazyRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategoryChange(null) },
                label = { Text(stringResource(StringKeyMatch.REMEDIES_FILTER_ALL)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.AccentPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = AppTheme.AccentPrimary,
                    containerColor = AppTheme.ChipBackground,
                    labelColor = AppTheme.TextSecondary
                )
            )
        }

        items(
            items = RemediesCalculator.RemedyCategory.entries.toList(),
            key = { it.name }
        ) { category ->
            val count = categoryCounts[category] ?: 0
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategoryChange(category) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(category.displayName)
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                color = if (selectedCategory == category)
                                    AppTheme.AccentPrimary
                                else
                                    AppTheme.TextMuted.copy(alpha = 0.3f),
                                shape = CircleShape
                            ) {
                                Text(
                                    "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selectedCategory == category) Color.White else AppTheme.TextMuted,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        getCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.AccentPrimary.copy(alpha = 0.2f),
                    selectedLabelColor = AppTheme.AccentPrimary,
                    selectedLeadingIconColor = AppTheme.AccentPrimary,
                    containerColor = AppTheme.ChipBackground,
                    labelColor = AppTheme.TextSecondary
                )
            )
        }
    }
}

@Composable
private fun RemedyCard(
    remedy: RemediesCalculator.Remedy,
    isExpanded: Boolean,
    onExpand: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "ChevronRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(getCategoryColor(remedy.category).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getCategoryIcon(remedy.category),
                            contentDescription = null,
                            tint = getCategoryColor(remedy.category),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            remedy.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                remedy.category.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.TextMuted
                            )
                            remedy.planet?.let { planet ->
                                Text(" • ", color = AppTheme.TextMuted)
                                Text(
                                    planet.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getPlanetColor(planet)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = getPriorityColor(remedy.priority).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getPriorityIcon(remedy.priority),
                            contentDescription = null,
                            tint = getPriorityColor(remedy.priority),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            remedy.priority.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = getPriorityColor(remedy.priority)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = AppTheme.DividerColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        remedy.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.TextSecondary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailSection(
                        icon = Icons.Outlined.MenuBook,
                        title = stringResource(StringKeyMatch.REMEDIES_METHOD),
                        content = remedy.method
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.weight(1f)) {
                            DetailSection(
                                icon = Icons.Outlined.Schedule,
                                title = stringResource(StringKeyMatch.REMEDIES_TIMING),
                                content = remedy.timing
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            DetailSection(
                                icon = Icons.Outlined.DateRange,
                                title = stringResource(StringKeyMatch.REMEDIES_DURATION),
                                content = remedy.duration
                            )
                        }
                    }

                    if (remedy.category == RemediesCalculator.RemedyCategory.MANTRA && remedy.mantraText != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MantraSection(
                            mantraText = remedy.mantraText,
                            onCopy = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(remedy.mantraText))
                            }
                        )
                    }

                    if (remedy.benefits.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BenefitsList(benefits = remedy.benefits)
                    }

                    if (remedy.cautions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        CautionsList(cautions = remedy.cautions)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(StringKeyMatch.MISC_COLLAPSE) else stringResource(StringKeyMatch.MISC_EXPAND),
                    tint = AppTheme.TextSubtle,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }
        }
    }
}

@Composable
private fun MantraSection(
    mantraText: String,
    onCopy: () -> Unit
) {
    Surface(
        color = AppTheme.AccentGold.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(StringKeyMatch.REMEDIES_MANTRA_SECTION),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.AccentGold
                )
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = stringResource(StringKeyMatch.REMEDIES_COPY_MANTRA),
                        tint = AppTheme.AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                mantraText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun BenefitsList(benefits: List<String>) {
    Column {
        Text(
            stringResource(StringKeyMatch.REMEDIES_BENEFITS),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        benefits.forEach { benefit ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = AppTheme.SuccessColor,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    benefit,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun CautionsList(cautions: List<String>) {
    Surface(
        color = AppTheme.WarningColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(StringKeyMatch.REMEDIES_CAUTIONS),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.WarningColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            cautions.forEach { caution ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = AppTheme.WarningColor,
                        modifier = Modifier
                            .size(14.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        caution,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    icon: ImageVector,
    title: String,
    content: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = AppTheme.AccentPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )
            Text(
                content,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PlanetsTab(
    result: RemediesCalculator.RemediesResult,
    listState: LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(
            items = result.planetaryAnalyses,
            key = { it.planet.name }
        ) { analysis ->
            PlanetAnalysisCard(analysis)
        }
    }
}

@Composable
private fun PlanetAnalysisCard(analysis: RemediesCalculator.PlanetaryAnalysis) {
    val planetColor = getPlanetColor(analysis.planet)

    val animatedStrength by animateFloatAsState(
        targetValue = analysis.strengthScore / 100f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "StrengthAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(planetColor.copy(alpha = 0.15f))
                            .border(
                                width = 2.dp,
                                color = planetColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            analysis.planet.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = planetColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            analysis.planet.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                analysis.sign.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.TextMuted
                            )
                            Text(" • ", color = AppTheme.TextMuted)
                            Text(
                                stringResource(StringKeyAnalysis.DIALOG_HOUSE) + " ${analysis.housePosition}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.TextMuted
                            )
                        }
                        // Fixed: Accessing displayName from the Nakshatra Enum, not the object itself
                        Text(
                            analysis.nakshatra.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.AccentPrimary
                        )
                    }
                }

                StrengthIndicator(
                    score = analysis.strengthScore,
                    strength = analysis.strength,
                    animatedProgress = animatedStrength
                )
            }

            val exaltedLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_EXALTED)
            val debilitatedLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_DEBILITATED)
            val retrogradeLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_RETROGRADE)
            val combustLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_COMBUST)
            val ownSignLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_OWN_SIGN)
            val friendlyLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_FRIENDLY)
            val enemySignLabel = stringResource(StringKeyMatch.PLANETARY_STATUS_ENEMY_SIGN)

            val statusIndicators = buildList {
                if (analysis.isExalted) add(exaltedLabel to AppTheme.SuccessColor)
                if (analysis.isDebilitated) add(debilitatedLabel to AppTheme.ErrorColor)
                if (analysis.isRetrograde) add(retrogradeLabel to AppTheme.WarningColor)
                if (analysis.isCombust) add(combustLabel to AppTheme.ErrorColor)
                if (analysis.isOwnSign) add(ownSignLabel to AppTheme.SuccessColor)
                if (analysis.isFriendlySign) add(friendlyLabel to Color(0xFF8BC34A))
                if (analysis.isEnemySign) add(enemySignLabel to AppTheme.ErrorColor)
            }

            if (statusIndicators.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(statusIndicators) { (text, color) ->
                        StatusChip(text = text, color = color)
                    }
                }
            }

            if (analysis.issues.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                analysis.issues.forEach { issue ->
                    IssueRow(text = issue)
                }
            }

            if (analysis.positives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                analysis.positives.forEach { positive ->
                    PositiveRow(text = positive)
                }
            }

            if (analysis.needsRemedy) {
                Spacer(modifier = Modifier.height(12.dp))
                RemedyRecommendationBanner(planet = analysis.planet)
            }
        }
    }
}

@Composable
private fun StrengthIndicator(
    score: Int,
    strength: RemediesCalculator.PlanetaryStrength,
    animatedProgress: Float
) {
    val strengthColor = getStrengthColor(strength)

    Surface(
        color = strengthColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = strengthColor,
                    trackColor = strengthColor.copy(alpha = 0.2f),
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    "$score",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = strengthColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                strength.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = strengthColor
            )
        }
    }
}

@Composable
private fun IssueRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = AppTheme.WarningColor,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.WarningColor
        )
    }
}

@Composable
private fun PositiveRow(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = AppTheme.SuccessColor,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.SuccessColor
        )
    }
}

@Composable
private fun RemedyRecommendationBanner(planet: Planet) {
    val weekday = getWeekdayForPlanet(planet)

    Surface(
        color = AppTheme.AccentPrimary.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Spa,
                contentDescription = null,
                tint = AppTheme.AccentPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(StringKeyMatch.REMEDIES_RECOMMENDED, 1),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.AccentPrimary
                )
                Text(
                    stringResource(StringKeyMatch.REMEDIES_BEST_DAY, weekday),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.TextMuted
                )
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Spa,
                contentDescription = null,
                tint = AppTheme.TextSubtle,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(StringKeyMatch.REMEDIES_NO_CHART),
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.TextMuted
            )
            Text(
                stringResource(StringKeyMatch.REMEDIES_SELECT_CHART),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextSubtle
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = AppTheme.AccentPrimary,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(StringKeyMatch.REMEDIES_ANALYZING),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(StringKeyMatch.REMEDIES_PREPARING),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextSubtle
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = AppTheme.ErrorColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(StringKey.ERROR_SOMETHING_WRONG),
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.AccentPrimary
                )
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(StringKey.BTN_RETRY))
            }
        }
    }
}

private fun getPlanetColor(planet: Planet): Color {
    return when (planet) {
        Planet.SUN -> Color(0xFFFF9800)
        Planet.MOON -> Color(0xFFE0E0E0)
        Planet.MARS -> Color(0xFFF44336)
        Planet.MERCURY -> Color(0xFF4CAF50)
        Planet.JUPITER -> Color(0xFFFFEB3B)
        Planet.VENUS -> Color(0xFFE91E63)
        Planet.SATURN -> Color(0xFF607D8B)
        Planet.RAHU -> Color(0xFF9C27B0)
        Planet.KETU -> Color(0xFF795548)
        else -> Color(0xFF2196F3)
    }
}

private fun getCategoryIcon(category: RemediesCalculator.RemedyCategory): ImageVector {
    return when (category) {
        RemediesCalculator.RemedyCategory.GEMSTONE -> Icons.Outlined.Diamond
        RemediesCalculator.RemedyCategory.MANTRA -> Icons.Outlined.MusicNote
        RemediesCalculator.RemedyCategory.YANTRA -> Icons.Outlined.GridView
        RemediesCalculator.RemedyCategory.CHARITY -> Icons.Outlined.VolunteerActivism
        RemediesCalculator.RemedyCategory.FASTING -> Icons.Outlined.Restaurant
        RemediesCalculator.RemedyCategory.COLOR -> Icons.Outlined.Palette
        RemediesCalculator.RemedyCategory.METAL -> Icons.Outlined.Circle
        RemediesCalculator.RemedyCategory.RUDRAKSHA -> Icons.Outlined.Spa
        RemediesCalculator.RemedyCategory.DEITY -> Icons.Outlined.TempleHindu
        RemediesCalculator.RemedyCategory.LIFESTYLE -> Icons.Outlined.FavoriteBorder
    }
}

private fun getCategoryColor(category: RemediesCalculator.RemedyCategory): Color {
    return when (category) {
        RemediesCalculator.RemedyCategory.GEMSTONE -> Color(0xFF9C27B0)
        RemediesCalculator.RemedyCategory.MANTRA -> Color(0xFFFF9800)
        RemediesCalculator.RemedyCategory.YANTRA -> Color(0xFF2196F3)
        RemediesCalculator.RemedyCategory.CHARITY -> Color(0xFF4CAF50)
        RemediesCalculator.RemedyCategory.FASTING -> Color(0xFFE91E63)
        RemediesCalculator.RemedyCategory.COLOR -> Color(0xFF00BCD4)
        RemediesCalculator.RemedyCategory.METAL -> Color(0xFF607D8B)
        RemediesCalculator.RemedyCategory.RUDRAKSHA -> Color(0xFF795548)
        RemediesCalculator.RemedyCategory.DEITY -> Color(0xFFFF5722)
        RemediesCalculator.RemedyCategory.LIFESTYLE -> Color(0xFF8BC34A)
    }
}

private fun getPriorityColor(priority: RemediesCalculator.RemedyPriority): Color {
    return when (priority) {
        RemediesCalculator.RemedyPriority.ESSENTIAL -> Color(0xFFF44336)
        RemediesCalculator.RemedyPriority.HIGHLY_RECOMMENDED -> Color(0xFFFF9800)
        RemediesCalculator.RemedyPriority.RECOMMENDED -> Color(0xFF4CAF50)
        RemediesCalculator.RemedyPriority.OPTIONAL -> Color(0xFF9E9E9E)
    }
}

private fun getPriorityIcon(priority: RemediesCalculator.RemedyPriority): ImageVector {
    return when (priority) {
        RemediesCalculator.RemedyPriority.ESSENTIAL -> Icons.Filled.PriorityHigh
        RemediesCalculator.RemedyPriority.HIGHLY_RECOMMENDED -> Icons.Filled.Star
        RemediesCalculator.RemedyPriority.RECOMMENDED -> Icons.Filled.ThumbUp
        RemediesCalculator.RemedyPriority.OPTIONAL -> Icons.Outlined.Info
    }
}

private fun getStrengthColor(strength: RemediesCalculator.PlanetaryStrength): Color {
    return when (strength) {
        RemediesCalculator.PlanetaryStrength.VERY_STRONG -> Color(0xFF4CAF50)
        RemediesCalculator.PlanetaryStrength.STRONG -> Color(0xFF8BC34A)
        RemediesCalculator.PlanetaryStrength.MODERATE -> Color(0xFFFFC107)
        RemediesCalculator.PlanetaryStrength.WEAK -> Color(0xFFFF9800)
        RemediesCalculator.PlanetaryStrength.VERY_WEAK -> Color(0xFFF44336)
        RemediesCalculator.PlanetaryStrength.AFFLICTED -> Color(0xFF9C27B0)
    }
}

private fun getLifeAreaIcon(area: String): ImageVector {
    return when (area.lowercase()) {
        "career" -> Icons.Outlined.Work
        "relationships", "marriage" -> Icons.Outlined.Favorite
        "health" -> Icons.Outlined.Healing
        "wealth", "finance" -> Icons.Outlined.AttachMoney
        "spiritual", "spirituality" -> Icons.Outlined.SelfImprovement
        "education" -> Icons.Outlined.School
        "family" -> Icons.Outlined.FamilyRestroom
        "children" -> Icons.Outlined.ChildCare
        else -> Icons.Outlined.Star
    }
}

private fun getLifeAreaColor(area: String): Color {
    return when (area.lowercase()) {
        "career" -> Color(0xFF2196F3)
        "relationships", "marriage" -> Color(0xFFE91E63)
        "health" -> Color(0xFF4CAF50)
        "wealth", "finance" -> Color(0xFFFF9800)
        "spiritual", "spirituality" -> Color(0xFF9C27B0)
        "education" -> Color(0xFF00BCD4)
        "family" -> Color(0xFF795548)
        "children" -> Color(0xFFFFEB3B)
        else -> Color(0xFF607D8B)
    }
}

@Composable
private fun getWeekdayForPlanet(planet: Planet): String {
    return when (planet) {
        Planet.SUN -> stringResource(StringKeyMatch.DAY_SUNDAY)
        Planet.MOON -> stringResource(StringKeyMatch.DAY_MONDAY)
        Planet.MARS -> stringResource(StringKeyMatch.DAY_TUESDAY)
        Planet.MERCURY -> stringResource(StringKeyMatch.DAY_WEDNESDAY)
        Planet.JUPITER -> stringResource(StringKeyMatch.DAY_THURSDAY)
        Planet.VENUS -> stringResource(StringKeyMatch.DAY_FRIDAY)
        Planet.SATURN -> stringResource(StringKeyMatch.DAY_SATURDAY)
        Planet.RAHU -> stringResource(StringKeyMatch.DAY_SATURDAY)
        Planet.KETU -> stringResource(StringKeyMatch.DAY_TUESDAY)
        else -> stringResource(StringKeyMatch.DAY_ANY)
    }
}