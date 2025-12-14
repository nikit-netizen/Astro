package com.astro.storm.ui.screen.chartdetail.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.LocalLanguage
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.getLocalizedName
import com.astro.storm.data.localization.stringResource
import com.astro.storm.ephemeris.YogaCalculator
import com.astro.storm.ui.screen.chartdetail.ChartDetailColors
import com.astro.storm.ui.screen.chartdetail.components.StatusBadge

/**
 * Yogas tab content displaying all detected yogas with filtering and analysis.
 */
@Composable
fun YogasTabContent(chart: VedicChart) {
    val yogaAnalysis = remember(chart) {
        YogaCalculator.calculateYogas(chart)
    }

    var selectedCategory by remember { mutableStateOf<YogaCalculator.YogaCategory?>(null) }

    val filteredYogas = remember(yogaAnalysis, selectedCategory) {
        if (selectedCategory == null) {
            yogaAnalysis.allYogas
        } else {
            yogaAnalysis.allYogas.filter { it.category == selectedCategory }
        }
    }

    val expandedYogaKeys = remember { mutableStateListOf<String>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            YogaSummaryCard(yogaAnalysis)
        }

        item {
            YogaCategoryFilter(
                categories = YogaCalculator.YogaCategory.entries,
                selectedCategory = selectedCategory,
                yogaCounts = yogaAnalysis.allYogas.groupingBy { it.category }.eachCount(),
                onCategorySelected = { selectedCategory = it }
            )
        }

        if (filteredYogas.isEmpty()) {
            item {
                EmptyYogasMessage(selectedCategory)
            }
        } else {
            items(
                items = filteredYogas,
                key = { yoga -> yoga.stableKey() }
            ) { yoga ->
                val yogaKey = remember(yoga) { yoga.stableKey() }
                YogaCard(
                    yoga = yoga,
                    isExpanded = yogaKey in expandedYogaKeys,
                    onToggleExpand = { expanded ->
                        if (expanded) {
                            expandedYogaKeys.add(yogaKey)
                        } else {
                            expandedYogaKeys.remove(yogaKey)
                        }
                    }
                )
            }
        }
    }
}

private fun YogaCalculator.Yoga.stableKey(): String {
    return buildString {
        append(category.name)
        append('|')
        append(name)
        append('|')
        append(sanskritName)
        append('|')
        append(planets.joinToString(",") { it.name })
        append('|')
        append(houses.sorted().joinToString(","))
    }
}


@Composable
private fun YogaSummaryCard(analysis: YogaCalculator.YogaAnalysis) {
    val language = LocalLanguage.current
    val positiveCount = analysis.allYogas.count { it.isAuspicious }
    val negativeCount = analysis.allYogas.count { !it.isAuspicious }
    val topYogas = analysis.allYogas.sortedByDescending { it.strengthPercentage }.take(3)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChartDetailColors.CardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = ChartDetailColors.AccentGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(StringKey.YOGA_ANALYSIS_SUMMARY),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartDetailColors.TextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusBadge(
                    count = analysis.allYogas.size,
                    label = stringResource(StringKey.YOGA_TOTAL),
                    color = ChartDetailColors.AccentGold
                )
                StatusBadge(
                    count = positiveCount,
                    label = stringResource(StringKey.YOGA_AUSPICIOUS),
                    color = ChartDetailColors.SuccessColor
                )
                StatusBadge(
                    count = negativeCount,
                    label = stringResource(StringKey.YOGA_CHALLENGING),
                    color = ChartDetailColors.WarningColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OverallStrengthBar(analysis.overallYogaStrength, language)

            if (topYogas.isNotEmpty()) {
                HorizontalDivider(
                    color = ChartDetailColors.DividerColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                TopYogasSection(topYogas, language)
            }
        }
    }
}

@Composable
private fun OverallStrengthBar(strength: Double, language: Language) {
    val progress = (strength / 100.0).coerceIn(0.0, 1.0).toFloat()
    val color = when {
        strength >= 75 -> ChartDetailColors.SuccessColor
        strength >= 50 -> ChartDetailColors.AccentTeal
        strength >= 25 -> ChartDetailColors.WarningColor
        else -> ChartDetailColors.ErrorColor
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(StringKey.YOGA_OVERALL_STRENGTH),
                fontSize = 13.sp,
                color = ChartDetailColors.TextSecondary
            )
            Text(
                text = formatPercentage(strength, language),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = ChartDetailColors.DividerColor
        )
    }
}

@Composable
private fun TopYogasSection(topYogas: List<YogaCalculator.Yoga>, language: Language) {
    Column {
        Text(
            text = stringResource(StringKey.YOGA_MOST_SIGNIFICANT),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChartDetailColors.AccentGold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        topYogas.forEach { yoga ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (yoga.isAuspicious) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = if (yoga.isAuspicious) ChartDetailColors.SuccessColor else ChartDetailColors.WarningColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = yoga.name,
                        fontSize = 13.sp,
                        color = ChartDetailColors.TextPrimary
                    )
                }
                Text(
                    text = getLocalizedStrength(yoga.strength, language),
                    fontSize = 11.sp,
                    color = ChartDetailColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun YogaCategoryFilter(
    categories: List<YogaCalculator.YogaCategory>,
    selectedCategory: YogaCalculator.YogaCategory?,
    yogaCounts: Map<YogaCalculator.YogaCategory, Int>,
    onCategorySelected: (YogaCalculator.YogaCategory?) -> Unit
) {
    val language = LocalLanguage.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(StringKey.YOGA_ALL), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChartDetailColors.AccentGold.copy(alpha = 0.2f),
                    selectedLabelColor = ChartDetailColors.AccentGold,
                    containerColor = ChartDetailColors.CardBackground,
                    labelColor = ChartDetailColors.TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ChartDetailColors.DividerColor,
                    selectedBorderColor = ChartDetailColors.AccentGold,
                    enabled = true,
                    selected = selectedCategory == null
                )
            )
        }

        items(categories) { category ->
            val count = yogaCounts[category] ?: 0
            if (count > 0) {
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text("${getCategoryDisplayName(category, language)} (${formatNumber(count, language)})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f),
                        selectedLabelColor = getCategoryColor(category),
                        containerColor = ChartDetailColors.CardBackground,
                        labelColor = ChartDetailColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = ChartDetailColors.DividerColor,
                        selectedBorderColor = getCategoryColor(category),
                        enabled = true,
                        selected = selectedCategory == category
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YogaCard(
    yoga: YogaCalculator.Yoga,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand(!isExpanded) },
        shape = RoundedCornerShape(16.dp),
        color = ChartDetailColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
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
                            .size(36.dp)
                            .background(
                                getCategoryColor(yoga.category).copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (yoga.isAuspicious) Icons.Outlined.Star else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = getCategoryColor(yoga.category),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = yoga.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChartDetailColors.TextPrimary
                        )
                        Text(
                            text = getCategoryDisplayName(yoga.category, language),
                            fontSize = 11.sp,
                            color = ChartDetailColors.TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    YogaStrengthBadge(strength = yoga.strengthPercentage, language = language)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ChartDetailColors.TextMuted,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = ChartDetailColors.DividerColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (yoga.sanskritName.isNotEmpty() && yoga.sanskritName != yoga.name) {
                        Text(
                            text = "${stringResource(StringKey.YOGA_SANSKRIT)}: ${yoga.sanskritName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ChartDetailColors.AccentPurple,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Text(
                        text = yoga.description,
                        fontSize = 13.sp,
                        color = ChartDetailColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        yoga.planets.forEach { planet ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ChartDetailColors.getPlanetColor(planet).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = planet.getLocalizedName(language),
                                    fontSize = 11.sp,
                                    color = ChartDetailColors.getPlanetColor(planet),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        yoga.houses.forEach { house ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ChartDetailColors.AccentPurple.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${stringResource(StringKey.YOGA_HOUSE_PREFIX)}${formatNumber(house, language)}",
                                    fontSize = 11.sp,
                                    color = ChartDetailColors.AccentPurple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = if (yoga.isAuspicious) {
                            ChartDetailColors.SuccessColor.copy(alpha = 0.1f)
                        } else {
                            ChartDetailColors.WarningColor.copy(alpha = 0.1f)
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(StringKey.YOGA_EFFECTS),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (yoga.isAuspicious) ChartDetailColors.SuccessColor else ChartDetailColors.WarningColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = yoga.effects,
                                fontSize = 13.sp,
                                color = ChartDetailColors.TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (yoga.activationPeriod.isNotEmpty()) {
                        Text(
                            text = "${stringResource(StringKey.YOGA_ACTIVATION)}: ${yoga.activationPeriod}",
                            fontSize = 11.sp,
                            color = ChartDetailColors.AccentTeal,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (!yoga.isAuspicious && yoga.cancellationFactors.isNotEmpty()) {
                        HorizontalDivider(
                            color = ChartDetailColors.DividerColor,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = "${stringResource(StringKey.YOGA_CANCELLATION_FACTORS)}:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChartDetailColors.SuccessColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        yoga.cancellationFactors.forEach { factor ->
                            Text(
                                text = "• $factor",
                                fontSize = 12.sp,
                                color = ChartDetailColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YogaStrengthBadge(strength: Double, language: Language) {
    val color = when {
        strength >= 80 -> ChartDetailColors.SuccessColor
        strength >= 60 -> ChartDetailColors.AccentTeal
        strength >= 40 -> ChartDetailColors.WarningColor
        else -> ChartDetailColors.TextMuted
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = formatPercentage(strength, language),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyYogasMessage(category: YogaCalculator.YogaCategory?) {
    val language = LocalLanguage.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ChartDetailColors.CardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.Star,
                contentDescription = null,
                tint = ChartDetailColors.TextMuted,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (category != null) {
                    stringResource(StringKey.YOGA_NO_CATEGORY_FOUND, getCategoryDisplayName(category, language))
                } else {
                    stringResource(StringKey.YOGA_NONE_DETECTED)
                },
                fontSize = 16.sp,
                color = ChartDetailColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================
// LOCALIZATION HELPER FUNCTIONS
// ============================================

private fun getCategoryDisplayName(category: YogaCalculator.YogaCategory, language: Language): String = when (category) {
    YogaCalculator.YogaCategory.RAJA_YOGA -> when (language) {
        Language.ENGLISH -> "Raja Yoga"
        Language.NEPALI -> "राज योग"
    }
    YogaCalculator.YogaCategory.DHANA_YOGA -> when (language) {
        Language.ENGLISH -> "Dhana Yoga"
        Language.NEPALI -> "धन योग"
    }
    YogaCalculator.YogaCategory.MAHAPURUSHA_YOGA -> when (language) {
        Language.ENGLISH -> "Mahapurusha Yoga"
        Language.NEPALI -> "महापुरुष योग"
    }
    YogaCalculator.YogaCategory.NABHASA_YOGA -> when (language) {
        Language.ENGLISH -> "Nabhasa Yoga"
        Language.NEPALI -> "नाभस योग"
    }
    YogaCalculator.YogaCategory.CHANDRA_YOGA -> when (language) {
        Language.ENGLISH -> "Chandra Yoga"
        Language.NEPALI -> "चन्द्र योग"
    }
    YogaCalculator.YogaCategory.SOLAR_YOGA -> when (language) {
        Language.ENGLISH -> "Solar Yoga"
        Language.NEPALI -> "सूर्य योग"
    }
    YogaCalculator.YogaCategory.NEGATIVE_YOGA -> when (language) {
        Language.ENGLISH -> "Negative Yoga"
        Language.NEPALI -> "नकारात्मक योग"
    }
    YogaCalculator.YogaCategory.SPECIAL_YOGA -> when (language) {
        Language.ENGLISH -> "Special Yoga"
        Language.NEPALI -> "विशेष योग"
    }
}

private fun getCategoryColor(category: YogaCalculator.YogaCategory): Color = when (category) {
    YogaCalculator.YogaCategory.RAJA_YOGA -> ChartDetailColors.AccentGold
    YogaCalculator.YogaCategory.DHANA_YOGA -> ChartDetailColors.SuccessColor
    YogaCalculator.YogaCategory.MAHAPURUSHA_YOGA -> ChartDetailColors.AccentPurple
    YogaCalculator.YogaCategory.NABHASA_YOGA -> ChartDetailColors.AccentTeal
    YogaCalculator.YogaCategory.CHANDRA_YOGA -> ChartDetailColors.AccentBlue
    YogaCalculator.YogaCategory.SOLAR_YOGA -> ChartDetailColors.AccentOrange
    YogaCalculator.YogaCategory.NEGATIVE_YOGA -> ChartDetailColors.ErrorColor
    YogaCalculator.YogaCategory.SPECIAL_YOGA -> ChartDetailColors.AccentRose
}

private fun formatNumber(number: Int, language: Language): String {
    return when (language) {
        Language.ENGLISH -> number.toString()
        Language.NEPALI -> {
            val nepaliDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
            number.toString().map { char ->
                if (char.isDigit()) nepaliDigits[char.digitToInt()] else char
            }.joinToString("")
        }
    }
}

private fun formatPercentage(value: Double, language: Language): String {
    val formatted = String.format("%.1f", value)
    return when (language) {
        Language.ENGLISH -> "$formatted%"
        Language.NEPALI -> {
            val nepaliDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
            val nepaliNumber = formatted.map { char ->
                when {
                    char.isDigit() -> nepaliDigits[char.digitToInt()]
                    else -> char
                }
            }.joinToString("")
            "$nepaliNumber%"
        }
    }
}

private fun getLocalizedStrength(strength: YogaCalculator.YogaStrength, language: Language): String = when (strength) {
    YogaCalculator.YogaStrength.EXTREMELY_STRONG -> when (language) {
        Language.ENGLISH -> "Extremely Strong"
        Language.NEPALI -> "अत्यन्त बलियो"
    }
    YogaCalculator.YogaStrength.STRONG -> when (language) {
        Language.ENGLISH -> "Strong"
        Language.NEPALI -> "बलियो"
    }
    YogaCalculator.YogaStrength.MODERATE -> when (language) {
        Language.ENGLISH -> "Moderate"
        Language.NEPALI -> "मध्यम"
    }
    YogaCalculator.YogaStrength.WEAK -> when (language) {
        Language.ENGLISH -> "Weak"
        Language.NEPALI -> "कमजोर"
    }
    YogaCalculator.YogaStrength.VERY_WEAK -> when (language) {
        Language.ENGLISH -> "Very Weak"
        Language.NEPALI -> "धेरै कमजोर"
    }
}
