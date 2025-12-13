package com.astro.storm.ui.screen.chartdetail.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.LocalLanguage
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.StringKeyAnalysis
import com.astro.storm.data.localization.StringKeyMatch
import com.astro.storm.data.localization.getLocalizedName
import com.astro.storm.data.localization.stringResource
import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.model.VedicChart
import com.astro.storm.ephemeris.Karana
import com.astro.storm.ephemeris.PanchangaCalculator
import com.astro.storm.ephemeris.PanchangaData
import com.astro.storm.ephemeris.Vara
import com.astro.storm.ephemeris.Yoga
import com.astro.storm.ui.screen.chartdetail.ChartDetailColors
import java.util.Locale

@Composable
fun PanchangaTabContent(chart: VedicChart) {
    val context = LocalContext.current

    val panchanga = remember(chart) {
        PanchangaCalculator(context).use { calculator ->
            calculator.calculatePanchanga(
                dateTime = chart.birthData.dateTime,
                latitude = chart.birthData.latitude,
                longitude = chart.birthData.longitude,
                timezone = chart.birthData.timezone
            )
        }
    }

    var expandedCards by rememberSaveable { mutableStateOf(setOf<String>()) }

    fun toggleCard(cardKey: String, expand: Boolean) {
        expandedCards = if (expand) expandedCards + cardKey else expandedCards - cardKey
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "summary") {
            PanchangaSummaryCard(panchanga)
        }

        item(key = "tithi") {
            TithiCard(
                panchanga = panchanga,
                isExpanded = "tithi" in expandedCards,
                onToggleExpand = { toggleCard("tithi", it) }
            )
        }

        item(key = "nakshatra") {
            NakshatraCard(
                panchanga = panchanga,
                isExpanded = "nakshatra" in expandedCards,
                onToggleExpand = { toggleCard("nakshatra", it) }
            )
        }

        item(key = "yoga") {
            YogaCard(
                panchanga = panchanga,
                isExpanded = "yoga" in expandedCards,
                onToggleExpand = { toggleCard("yoga", it) }
            )
        }

        item(key = "karana") {
            KaranaCard(
                panchanga = panchanga,
                isExpanded = "karana" in expandedCards,
                onToggleExpand = { toggleCard("karana", it) }
            )
        }

        item(key = "vara") {
            VaraCard(
                panchanga = panchanga,
                isExpanded = "vara" in expandedCards,
                onToggleExpand = { toggleCard("vara", it) }
            )
        }

        item(key = "info") {
            PanchangaInfoCard(
                isExpanded = "info" in expandedCards,
                onToggleExpand = { toggleCard("info", it) }
            )
        }

        item(key = "spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PanchangaSummaryCard(panchanga: PanchangaData) {
    val language = LocalLanguage.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Panchanga summary for birth time" },
        shape = RoundedCornerShape(20.dp),
        color = ChartDetailColors.CardBackground,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = ChartDetailColors.AccentGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(StringKeyAnalysis.PANCHANGA_AT_BIRTH),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChartDetailColors.TextPrimary
                    )
                    Text(
                        text = stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT),
                        fontSize = 12.sp,
                        color = ChartDetailColors.TextMuted
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PanchangaElement(
                    label = stringResource(StringKeyMatch.PANCHANGA_TITHI),
                    sanskrit = stringResource(StringKeyAnalysis.PANCHANGA_LUNAR_DAY_SANSKRIT),
                    value = panchanga.tithi.tithi.getLocalizedName(language),
                    color = ChartDetailColors.AccentTeal
                )
                PanchangaElement(
                    label = stringResource(StringKeyMatch.PANCHANGA_NAKSHATRA_LABEL),
                    sanskrit = stringResource(StringKeyAnalysis.PANCHANGA_LUNAR_MANSION_SANSKRIT),
                    value = panchanga.nakshatra.nakshatra.getLocalizedName(language),
                    color = ChartDetailColors.AccentPurple
                )
                PanchangaElement(
                    label = stringResource(StringKeyMatch.PANCHANGA_YOGA),
                    sanskrit = stringResource(StringKeyAnalysis.PANCHANGA_LUNISOLAR_SANSKRIT),
                    value = panchanga.yoga.yoga.getLocalizedName(language),
                    color = ChartDetailColors.AccentGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PanchangaElement(
                    label = stringResource(StringKeyMatch.PANCHANGA_KARANA),
                    sanskrit = stringResource(StringKeyAnalysis.PANCHANGA_HALF_LUNAR_DAY_SANSKRIT),
                    value = panchanga.karana.karana.getLocalizedName(language),
                    color = ChartDetailColors.AccentBlue
                )
                PanchangaElement(
                    label = stringResource(StringKeyMatch.PANCHANGA_VARA),
                    sanskrit = stringResource(StringKeyAnalysis.PANCHANGA_WEEKDAY_SANSKRIT),
                    value = panchanga.vara.getLocalizedName(language),
                    color = ChartDetailColors.AccentOrange
                )
            }

            HorizontalDivider(
                color = ChartDetailColors.DividerColor,
                modifier = Modifier.padding(vertical = 20.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SunTimeIndicator(
                    label = stringResource(StringKeyMatch.PANCHANGA_SUNRISE),
                    time = panchanga.sunrise,
                    icon = Icons.Outlined.WbSunny,
                    color = ChartDetailColors.AccentGold
                )
                MoonPhaseIndicator(
                    phase = panchanga.moonPhase,
                    paksha = panchanga.paksha.getLocalizedName(language)
                )
                SunTimeIndicator(
                    label = stringResource(StringKeyMatch.PANCHANGA_SUNSET),
                    time = panchanga.sunset,
                    icon = Icons.Outlined.WbTwilight,
                    color = ChartDetailColors.AccentOrange
                )
            }
        }
    }
}

@Composable
private fun SunTimeIndicator(
    label: String,
    time: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = "$label at $time",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = ChartDetailColors.TextMuted
        )
        Text(
            text = time,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun MoonPhaseIndicator(phase: Double, paksha: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            MoonPhaseCanvas(phase = phase)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = paksha,
            fontSize = 10.sp,
            color = ChartDetailColors.TextMuted
        )
        Text(
            text = "${String.format(Locale.US, "%.1f", phase)}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChartDetailColors.AccentPurple
        )
    }
}

@Composable
private fun MoonPhaseCanvas(phase: Double) {
    val illuminatedColor = ChartDetailColors.AccentPurple
    val darkColor = ChartDetailColors.CardBackground

    Canvas(
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = "Moon phase ${phase.toInt()}% illuminated" }
    ) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)

        drawCircle(
            color = darkColor,
            radius = radius,
            center = center
        )

        drawCircle(
            color = illuminatedColor.copy(alpha = 0.3f),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        val illuminationFraction = (phase / 100).toFloat().coerceIn(0f, 1f)
        val sweepAngle = 360f * illuminationFraction

        drawArc(
            color = illuminatedColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

@Composable
private fun PanchangaElement(
    label: String,
    sanskrit: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = ChartDetailColors.TextMuted
        )
        Text(
            text = sanskrit,
            fontSize = 9.sp,
            color = ChartDetailColors.TextMuted.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TithiCard(
    panchanga: PanchangaData,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val tithiData = getTithiData(panchanga.tithi.tithi.number)

    ExpandableDetailCard(
        title = stringResource(StringKeyMatch.PANCHANGA_TITHI),
        subtitle = stringResource(StringKeyAnalysis.PANCHANGA_TITHI_SUBTITLE),
        value = panchanga.tithi.tithi.getLocalizedName(language),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        icon = Icons.Outlined.Brightness4,
        iconColor = ChartDetailColors.AccentTeal,
        qualityIndicator = tithiData.quality
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT_LABEL), panchanga.tithi.tithi.sanskrit, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NUMBER), "${panchanga.tithi.number} of 30", ChartDetailColors.TextPrimary)
            DetailRow(stringResource(StringKeyMatch.PANCHANGA_PAKSHA), panchanga.paksha.getLocalizedName(language), ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_DEITY), tithiData.deity, ChartDetailColors.AccentPurple)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_LORD), panchanga.tithi.lord.getLocalizedName(language), ChartDetailColors.AccentTeal)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NATURE), tithiData.nature, getQualityColor(tithiData.quality))
            ProgressRow(stringResource(StringKeyAnalysis.PANCHANGA_PROGRESS), panchanga.tithi.progress, ChartDetailColors.AccentGold)

            DescriptionSection(
                title = stringResource(StringKeyAnalysis.PANCHANGA_SIGNIFICANCE),
                description = tithiData.description
            )

            if (tithiData.activities.isNotEmpty()) {
                ActivitiesSection(
                    favorable = tithiData.activities,
                    unfavorable = tithiData.avoid
                )
            }
        }
    }
}

@Composable
private fun NakshatraCard(
    panchanga: PanchangaData,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val nakshatraData = getNakshatraData(panchanga.nakshatra.nakshatra)

    ExpandableDetailCard(
        title = stringResource(StringKeyMatch.PANCHANGA_NAKSHATRA_LABEL),
        subtitle = stringResource(StringKeyAnalysis.PANCHANGA_NAKSHATRA_SUBTITLE),
        value = panchanga.nakshatra.nakshatra.getLocalizedName(language),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        icon = Icons.Outlined.Star,
        iconColor = ChartDetailColors.AccentPurple,
        qualityIndicator = nakshatraData.quality
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT_LABEL), nakshatraData.sanskrit, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NUMBER), "${panchanga.nakshatra.number} of 27", ChartDetailColors.TextPrimary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_PADA), "${panchanga.nakshatra.pada} of 4", ChartDetailColors.AccentTeal)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_RULER), panchanga.nakshatra.lord.getLocalizedName(language), ChartDetailColors.AccentGold)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_DEITY), nakshatraData.deity, ChartDetailColors.AccentPurple)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SYMBOL), nakshatraData.symbol, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_GANA), nakshatraData.gana, getGanaColor(nakshatraData.gana))
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_GUNA), nakshatraData.guna, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_ANIMAL), nakshatraData.animal, ChartDetailColors.TextSecondary)
            ProgressRow(stringResource(StringKeyAnalysis.PANCHANGA_PROGRESS), panchanga.nakshatra.progress, ChartDetailColors.AccentGold)

            DescriptionSection(
                title = stringResource(StringKeyAnalysis.PANCHANGA_CHARACTERISTICS),
                description = nakshatraData.description
            )
        }
    }
}

@Composable
private fun YogaCard(
    panchanga: PanchangaData,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val yogaData = getYogaData(panchanga.yoga.yoga)

    ExpandableDetailCard(
        title = stringResource(StringKeyMatch.PANCHANGA_YOGA),
        subtitle = stringResource(StringKeyAnalysis.PANCHANGA_YOGA_SUBTITLE),
        value = panchanga.yoga.yoga.getLocalizedName(language),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        icon = Icons.Outlined.AutoAwesome,
        iconColor = ChartDetailColors.AccentGold,
        qualityIndicator = yogaData.quality
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT_LABEL), yogaData.sanskrit, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NUMBER), "${panchanga.yoga.number} of 27", ChartDetailColors.TextPrimary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_MEANING), yogaData.meaning, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NATURE), panchanga.yoga.yoga.nature.getLocalizedName(language), getQualityColor(yogaData.quality))
            ProgressRow(stringResource(StringKeyAnalysis.PANCHANGA_PROGRESS), panchanga.yoga.progress, ChartDetailColors.AccentTeal)

            DescriptionSection(
                title = stringResource(StringKeyAnalysis.PANCHANGA_EFFECTS),
                description = yogaData.description
            )
        }
    }
}

@Composable
private fun KaranaCard(
    panchanga: PanchangaData,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val karanaData = getKaranaData(panchanga.karana.karana)

    ExpandableDetailCard(
        title = stringResource(StringKeyMatch.PANCHANGA_KARANA),
        subtitle = stringResource(StringKeyAnalysis.PANCHANGA_KARANA_SUBTITLE),
        value = panchanga.karana.karana.getLocalizedName(language),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        icon = Icons.Outlined.Schedule,
        iconColor = ChartDetailColors.AccentBlue,
        qualityIndicator = karanaData.quality
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT_LABEL), karanaData.sanskrit, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NUMBER), "${panchanga.karana.number} of 60", ChartDetailColors.TextPrimary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_TYPE), karanaData.type, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_NATURE), panchanga.karana.karana.type.getLocalizedName(language), getQualityColor(karanaData.quality))
            ProgressRow(stringResource(StringKeyAnalysis.PANCHANGA_PROGRESS), panchanga.karana.progress, ChartDetailColors.AccentGold)

            DescriptionSection(
                title = stringResource(StringKeyAnalysis.PANCHANGA_SIGNIFICANCE),
                description = karanaData.description
            )
        }
    }
}

@Composable
private fun VaraCard(
    panchanga: PanchangaData,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val varaData = getVaraData(panchanga.vara)

    ExpandableDetailCard(
        title = stringResource(StringKeyMatch.PANCHANGA_VARA),
        subtitle = stringResource(StringKeyAnalysis.PANCHANGA_VARA_SUBTITLE),
        value = panchanga.vara.getLocalizedName(language),
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        icon = Icons.Outlined.CalendarMonth,
        iconColor = ChartDetailColors.AccentOrange,
        qualityIndicator = null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_SANSKRIT_LABEL), varaData.sanskrit, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_RULING_PLANET), panchanga.vara.lord.getLocalizedName(language), ChartDetailColors.getPlanetColor(panchanga.vara.lord))
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_ELEMENT), varaData.element, ChartDetailColors.TextSecondary)
            DetailRow(stringResource(StringKeyAnalysis.PANCHANGA_DIRECTION), varaData.direction, ChartDetailColors.TextSecondary)

            DescriptionSection(
                title = stringResource(StringKeyAnalysis.PANCHANGA_SIGNIFICANCE),
                description = varaData.description
            )

            if (varaData.favorable.isNotEmpty()) {
                ActivitiesSection(
                    favorable = varaData.favorable,
                    unfavorable = varaData.unfavorable
                )
            }
        }
    }
}

@Composable
private fun ExpandableDetailCard(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit,
    qualityIndicator: Quality?,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "expand_rotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand(!isExpanded) }
            .semantics { contentDescription = "$title: $value. Tap to ${if (isExpanded) "collapse" else "expand"}" },
        shape = RoundedCornerShape(16.dp),
        color = ChartDetailColors.CardBackground,
        tonalElevation = 1.dp
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
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ChartDetailColors.TextPrimary
                            )
                            qualityIndicator?.let { quality ->
                                Spacer(modifier = Modifier.width(8.dp))
                                QualityBadge(quality)
                            }
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = ChartDetailColors.TextMuted
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = iconColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = iconColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ChartDetailColors.TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        color = ChartDetailColors.DividerColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun QualityBadge(quality: Quality) {
    val (color, text) = when (quality) {
        Quality.EXCELLENT -> ChartDetailColors.SuccessColor to stringResource(StringKeyAnalysis.QUALITY_EXCELLENT)
        Quality.GOOD -> ChartDetailColors.AccentTeal to stringResource(StringKeyAnalysis.QUALITY_GOOD)
        Quality.NEUTRAL -> ChartDetailColors.TextMuted to stringResource(StringKeyAnalysis.QUALITY_NEUTRAL)
        Quality.CHALLENGING -> ChartDetailColors.WarningColor to stringResource(StringKeyAnalysis.QUALITY_CHALLENGING)
        Quality.INAUSPICIOUS -> ChartDetailColors.ErrorColor to stringResource(StringKeyAnalysis.QUALITY_INAUSPICIOUS)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = ChartDetailColors.TextMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun ProgressRow(
    label: String,
    progress: Double,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = ChartDetailColors.TextMuted
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(2.dp),
                    color = color.copy(alpha = 0.2f)
                ) {}
                Surface(
                    modifier = Modifier
                        .fillMaxWidth((progress / 100).toFloat().coerceIn(0f, 1f))
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = color
                ) {}
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${String.format(Locale.US, "%.1f", progress)}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun DescriptionSection(
    title: String,
    description: String
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        HorizontalDivider(
            color = ChartDetailColors.DividerColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChartDetailColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = ChartDetailColors.TextPrimary,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ActivitiesSection(
    favorable: List<String>,
    unfavorable: List<String>
) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        if (favorable.isNotEmpty()) {
            Text(
                text = stringResource(StringKeyAnalysis.PANCHANGA_FAVORABLE_ACTIVITIES),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChartDetailColors.SuccessColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = favorable.joinToString(", "),
                fontSize = 12.sp,
                color = ChartDetailColors.TextSecondary,
                lineHeight = 18.sp
            )
        }

        if (unfavorable.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(StringKeyAnalysis.PANCHANGA_AVOID),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ChartDetailColors.WarningColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = unfavorable.joinToString(", "),
                fontSize = 12.sp,
                color = ChartDetailColors.TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PanchangaInfoCard(
    isExpanded: Boolean,
    onToggleExpand: (Boolean) -> Unit
) {
    val language = LocalLanguage.current
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "info_rotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand(!isExpanded) },
        shape = RoundedCornerShape(16.dp),
        color = ChartDetailColors.CardBackground,
        tonalElevation = 1.dp
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = ChartDetailColors.AccentPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(StringKeyAnalysis.PANCHANGA_ABOUT),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ChartDetailColors.TextPrimary
                        )
                        Text(
                            text = stringResource(StringKeyAnalysis.PANCHANGA_ABOUT_SUBTITLE),
                            fontSize = 11.sp,
                            color = ChartDetailColors.TextMuted
                        )
                    }
                }
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = ChartDetailColors.TextMuted,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(
                        color = ChartDetailColors.DividerColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = stringResource(StringKeyAnalysis.PANCHANGA_ABOUT_DESCRIPTION),
                        fontSize = 13.sp,
                        color = ChartDetailColors.TextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val elements = listOf(
                        Triple(stringResource(StringKeyAnalysis.PANCHANGA_INFO_TITHI_TITLE), stringResource(StringKeyAnalysis.PANCHANGA_INFO_TITHI_LABEL), stringResource(StringKeyAnalysis.PANCHANGA_TITHI_DESC)),
                        Triple(stringResource(StringKeyAnalysis.PANCHANGA_INFO_NAKSHATRA_TITLE), stringResource(StringKeyAnalysis.PANCHANGA_INFO_NAKSHATRA_LABEL), stringResource(StringKeyAnalysis.PANCHANGA_NAKSHATRA_DESC)),
                        Triple(stringResource(StringKeyAnalysis.PANCHANGA_INFO_YOGA_TITLE), stringResource(StringKeyAnalysis.PANCHANGA_INFO_YOGA_LABEL), stringResource(StringKeyAnalysis.PANCHANGA_YOGA_DESC)),
                        Triple(stringResource(StringKeyAnalysis.PANCHANGA_INFO_KARANA_TITLE), stringResource(StringKeyAnalysis.PANCHANGA_INFO_KARANA_LABEL), stringResource(StringKeyAnalysis.PANCHANGA_KARANA_DESC)),
                        Triple(stringResource(StringKeyAnalysis.PANCHANGA_INFO_VARA_TITLE), stringResource(StringKeyAnalysis.PANCHANGA_INFO_VARA_LABEL), stringResource(StringKeyAnalysis.PANCHANGA_VARA_DESC))
                    )

                    elements.forEach { (name, subtitle, description) ->
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                color = ChartDetailColors.AccentGold,
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                            )
                            Column {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ChartDetailColors.AccentTeal
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = ChartDetailColors.TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    fontSize = 12.sp,
                                    color = ChartDetailColors.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChartDetailColors.AccentGold.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = stringResource(StringKeyAnalysis.PANCHANGA_BIRTH_INSIGHT),
                            fontSize = 12.sp,
                            color = ChartDetailColors.TextSecondary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private enum class Quality {
    EXCELLENT, GOOD, NEUTRAL, CHALLENGING, INAUSPICIOUS
}

private fun getQualityColor(quality: Quality): Color {
    return when (quality) {
        Quality.EXCELLENT -> ChartDetailColors.SuccessColor
        Quality.GOOD -> ChartDetailColors.AccentTeal
        Quality.NEUTRAL -> ChartDetailColors.TextSecondary
        Quality.CHALLENGING -> ChartDetailColors.WarningColor
        Quality.INAUSPICIOUS -> ChartDetailColors.ErrorColor
    }
}

private fun getGanaColor(gana: String): Color {
    return when (gana) {
        "Deva" -> ChartDetailColors.AccentGold
        "Manushya" -> ChartDetailColors.AccentTeal
        "Rakshasa" -> ChartDetailColors.AccentOrange
        else -> ChartDetailColors.TextSecondary
    }
}

private data class TithiData(
    val deity: String,
    val nature: String,
    val quality: Quality,
    val description: String,
    val activities: List<String>,
    val avoid: List<String>
)

private fun getTithiData(tithiNumber: Int): TithiData {
    return when (tithiNumber) {
        1, 16 -> TithiData(
            deity = "Agni (Fire God)",
            nature = "Nanda (Joyful)",
            quality = Quality.GOOD,
            description = "Pratipada marks new beginnings. The first tithi after New Moon or Full Moon carries the energy of initiation and fresh starts. It is auspicious for beginning new ventures, laying foundations, and starting journeys.",
            activities = listOf("New beginnings", "Starting ventures", "Foundation laying", "Travel"),
            avoid = listOf("Completing projects", "Endings")
        )
        2, 17 -> TithiData(
            deity = "Brahma (Creator)",
            nature = "Bhadra (Auspicious)",
            quality = Quality.EXCELLENT,
            description = "Dwitiya is ruled by Brahma, the creator. This tithi is excellent for creative endeavors, naming ceremonies, and constructive activities. It supports growth and expansion of new initiatives.",
            activities = listOf("Creative work", "Naming ceremonies", "Marriage", "House warming"),
            avoid = listOf("Conflict", "Aggressive actions")
        )
        3, 18 -> TithiData(
            deity = "Gauri (Parvati)",
            nature = "Jaya (Victory)",
            quality = Quality.EXCELLENT,
            description = "Tritiya is associated with victory and success. Ruled by Gauri, it is highly auspicious for celebrations, religious ceremonies, and activities requiring divine grace and feminine energy.",
            activities = listOf("Religious ceremonies", "Celebrations", "Victory rituals", "Arts"),
            avoid = listOf("Conflicts", "Harsh activities")
        )
        4, 19 -> TithiData(
            deity = "Ganesha/Yama",
            nature = "Rikta (Empty)",
            quality = Quality.CHALLENGING,
            description = "Chaturthi is ruled by Ganesha (4th) and Yama (19th). While the 4th is for Ganesha worship, these tithis are generally considered inauspicious for beginning new work due to their 'empty' nature.",
            activities = listOf("Ganesha worship", "Removing obstacles", "Spiritual practices"),
            avoid = listOf("New beginnings", "Travel", "Important decisions")
        )
        5, 20 -> TithiData(
            deity = "Nagas (Serpent deities)",
            nature = "Nanda (Joyful)",
            quality = Quality.EXCELLENT,
            description = "Panchami is excellent for education, learning, and wisdom pursuits. Ruled by the Nagas, it supports activities requiring knowledge, skill development, and intellectual growth.",
            activities = listOf("Education", "Learning", "Writing", "Medicine", "Healing"),
            avoid = listOf("Destructive activities")
        )
        6, 21 -> TithiData(
            deity = "Kartikeya (Skanda)",
            nature = "Bhadra (Auspicious)",
            quality = Quality.GOOD,
            description = "Shashthi is dedicated to Kartikeya (Skanda), the god of war and victory. It is favorable for activities requiring courage, medical treatments, and overcoming enemies or diseases.",
            activities = listOf("Medical treatments", "Surgery", "Overcoming obstacles", "Courage"),
            avoid = listOf("Timid actions", "Postponements")
        )
        7, 22 -> TithiData(
            deity = "Surya (Sun God)",
            nature = "Jaya (Victory)",
            quality = Quality.EXCELLENT,
            description = "Saptami is ruled by the Sun and brings victory and success. Excellent for travel, especially pilgrimages, vehicle purchases, and activities requiring solar energy and vitality.",
            activities = listOf("Travel", "Pilgrimages", "Vehicle purchase", "Government work"),
            avoid = listOf("Night activities", "Moon-related work")
        )
        8, 23 -> TithiData(
            deity = "Shiva/Rudra",
            nature = "Rikta (Empty)",
            quality = Quality.NEUTRAL,
            description = "Ashtami is sacred to Lord Shiva and considered powerful for spiritual practices. While classified as Rikta, it is excellent for worship, fasting, and tantric practices. Mixed for worldly activities.",
            activities = listOf("Shiva worship", "Fasting", "Spiritual practices", "Meditation"),
            avoid = listOf("New ventures", "Material pursuits", "Celebrations")
        )
        9, 24 -> TithiData(
            deity = "Durga (Mother Goddess)",
            nature = "Nanda (Joyful)",
            quality = Quality.GOOD,
            description = "Navami is sacred to Durga and other fierce forms of the Divine Mother. Excellent for worship of the goddess, overcoming enemies, and activities requiring aggressive energy and protection.",
            activities = listOf("Durga worship", "Protection rituals", "Overcoming enemies", "Strength"),
            avoid = listOf("Peaceful negotiations", "Gentle activities")
        )
        10, 25 -> TithiData(
            deity = "Yama (God of Death)",
            nature = "Bhadra (Auspicious)",
            quality = Quality.EXCELLENT,
            description = "Dashami represents victory and completion. Ruled by Yama, it is excellent for completing tasks, achieving goals, and celebrating success. Highly auspicious for important undertakings.",
            activities = listOf("Completing projects", "Victory celebrations", "Important tasks", "Success"),
            avoid = listOf("Beginning long-term projects")
        )
        11, 26 -> TithiData(
            deity = "Vishnu (Preserver)",
            nature = "Jaya (Victory)",
            quality = Quality.EXCELLENT,
            description = "Ekadashi is the most spiritually significant tithi, sacred to Lord Vishnu. Fasting on this day is considered highly meritorious. Excellent for spiritual practices, but material activities should be minimized.",
            activities = listOf("Fasting", "Vishnu worship", "Spiritual practices", "Meditation", "Charity"),
            avoid = listOf("Material pursuits", "Eating grains", "Worldly pleasures")
        )
        12, 27 -> TithiData(
            deity = "Vishnu (Preserver)",
            nature = "Bhadra (Auspicious)",
            quality = Quality.GOOD,
            description = "Dwadashi follows Ekadashi and is auspicious for breaking the fast and religious ceremonies. Good for charitable activities, feeding Brahmins, and continuing spiritual practices.",
            activities = listOf("Breaking fast", "Religious ceremonies", "Charity", "Feeding others"),
            avoid = listOf("Fasting continuation", "Heavy foods")
        )
        13, 28 -> TithiData(
            deity = "Kamadeva (God of Love)",
            nature = "Jaya (Victory)",
            quality = Quality.GOOD,
            description = "Trayodashi is favorable for Shiva worship, especially on Maha Shivaratri. Good for love-related matters, arts, and activities bringing joy. The 13th tithi is associated with auspiciousness.",
            activities = listOf("Shiva worship", "Romance", "Arts", "Music", "Celebrations"),
            avoid = listOf("Aggressive activities", "Conflicts")
        )
        14, 29 -> TithiData(
            deity = "Shiva/Kali",
            nature = "Rikta (Empty)",
            quality = Quality.CHALLENGING,
            description = "Chaturdashi is ruled by Shiva and Kali. While powerful for tantric practices and worship, it is considered challenging for worldly activities. Excellent for spiritual disciplines and removing negative energies.",
            activities = listOf("Tantric practices", "Shiva/Kali worship", "Removing negativity", "Spiritual austerities"),
            avoid = listOf("New beginnings", "Auspicious ceremonies", "Travel")
        )
        15 -> TithiData(
            deity = "Chandra (Moon God)",
            nature = "Purna (Complete)",
            quality = Quality.EXCELLENT,
            description = "Purnima (Full Moon) is the most auspicious tithi, representing completion and fullness. The Moon is at peak strength, making it excellent for all auspicious activities, spiritual practices, and celebrations.",
            activities = listOf("All auspicious activities", "Celebrations", "Spiritual practices", "Charity", "Worship"),
            avoid = listOf("Surgery", "Activities requiring darkness")
        )
        30 -> TithiData(
            deity = "Pitris (Ancestors)",
            nature = "Purna (Complete)",
            quality = Quality.NEUTRAL,
            description = "Amavasya (New Moon) is sacred to the ancestors. While considered inauspicious for new beginnings, it is excellent for ancestral rites (Shraddha), Kali worship, and tantric practices. A time for introspection.",
            activities = listOf("Ancestral rites", "Kali worship", "Tantric practices", "Introspection", "Shadow work"),
            avoid = listOf("New beginnings", "Auspicious ceremonies", "Travel", "Important decisions")
        )
        else -> TithiData(
            deity = "Various",
            nature = "Mixed",
            quality = Quality.NEUTRAL,
            description = "This tithi carries mixed influences based on various planetary factors at the time.",
            activities = listOf("General activities"),
            avoid = emptyList()
        )
    }
}

private data class NakshatraData(
    val sanskrit: String,
    val deity: String,
    val symbol: String,
    val gana: String,
    val guna: String,
    val animal: String,
    val quality: Quality,
    val description: String
)

private fun getNakshatraData(nakshatra: Nakshatra): NakshatraData {
    return when (nakshatra) {
        Nakshatra.ASHWINI -> NakshatraData(
            sanskrit = "अश्विनी",
            deity = "Ashwini Kumaras (Divine Physicians)",
            symbol = "Horse's head",
            gana = "Deva",
            guna = "Rajas",
            animal = "Male Horse",
            quality = Quality.EXCELLENT,
            description = "Ashwini is the first nakshatra, ruled by Ketu. Those born under this star possess quick healing abilities, spontaneity, and pioneering spirit. They are energetic, courageous, and drawn to helping others. Associated with speed, initiative, and new beginnings."
        )
        Nakshatra.BHARANI -> NakshatraData(
            sanskrit = "भरणी",
            deity = "Yama (God of Death)",
            symbol = "Yoni (Female reproductive organ)",
            gana = "Manushya",
            guna = "Rajas",
            animal = "Male Elephant",
            quality = Quality.NEUTRAL,
            description = "Bharani is ruled by Venus and represents the power of transformation. Those born here possess strong will, creativity, and the ability to bear great responsibilities. Associated with birth, death, and transformation cycles."
        )
        Nakshatra.KRITTIKA -> NakshatraData(
            sanskrit = "कृत्तिका",
            deity = "Agni (Fire God)",
            symbol = "Razor/Flame",
            gana = "Rakshasa",
            guna = "Rajas",
            animal = "Female Sheep",
            quality = Quality.GOOD,
            description = "Krittika is ruled by the Sun and represents purifying fire. Those born here possess sharp intellect, determination, and the ability to cut through illusions. They are often leaders with strong principles and purifying influence."
        )
        Nakshatra.ROHINI -> NakshatraData(
            sanskrit = "रोहिणी",
            deity = "Brahma (Creator)",
            symbol = "Ox cart/Chariot",
            gana = "Manushya",
            guna = "Rajas",
            animal = "Male Serpent",
            quality = Quality.EXCELLENT,
            description = "Rohini is the most fertile nakshatra, ruled by the Moon. Those born here possess beauty, creativity, and material abundance. They are charming, artistic, and have strong desires. The Moon is exalted in this nakshatra."
        )
        Nakshatra.MRIGASHIRA -> NakshatraData(
            sanskrit = "मृगशिरा",
            deity = "Soma (Moon God)",
            symbol = "Deer's head",
            gana = "Deva",
            guna = "Tamas",
            animal = "Female Serpent",
            quality = Quality.GOOD,
            description = "Mrigashira is ruled by Mars and represents the searching nature. Those born here are curious, gentle, and always seeking. They possess artistic sensibilities and are drawn to exploration and research."
        )
        Nakshatra.ARDRA -> NakshatraData(
            sanskrit = "आर्द्रा",
            deity = "Rudra (Storm God)",
            symbol = "Teardrop/Diamond",
            gana = "Manushya",
            guna = "Tamas",
            animal = "Female Dog",
            quality = Quality.CHALLENGING,
            description = "Ardra is ruled by Rahu and represents transformation through storms. Those born here experience intense emotions and transformative life events. They possess sharp intellect and the ability to destroy old patterns for renewal."
        )
        Nakshatra.PUNARVASU -> NakshatraData(
            sanskrit = "पुनर्वसु",
            deity = "Aditi (Mother of Gods)",
            symbol = "Bow and quiver",
            gana = "Deva",
            guna = "Sattva",
            animal = "Female Cat",
            quality = Quality.EXCELLENT,
            description = "Punarvasu is ruled by Jupiter and represents renewal and return to light. Those born here possess optimism, philosophical nature, and the ability to bounce back from difficulties. Associated with safety, home, and nurturing."
        )
        Nakshatra.PUSHYA -> NakshatraData(
            sanskrit = "पुष्य",
            deity = "Brihaspati (Jupiter)",
            symbol = "Cow's udder/Flower",
            gana = "Deva",
            guna = "Sattva",
            animal = "Male Sheep",
            quality = Quality.EXCELLENT,
            description = "Pushya is considered the most auspicious nakshatra for most activities, ruled by Saturn. Those born here are nurturing, wise, and spiritually inclined. They possess the ability to nourish and support others' growth."
        )
        Nakshatra.ASHLESHA -> NakshatraData(
            sanskrit = "आश्लेषा",
            deity = "Nagas (Serpent Gods)",
            symbol = "Coiled serpent",
            gana = "Rakshasa",
            guna = "Sattva",
            animal = "Male Cat",
            quality = Quality.CHALLENGING,
            description = "Ashlesha is ruled by Mercury and represents serpentine wisdom. Those born here possess deep intuition, hypnotic abilities, and kundalini energy. They can be secretive and possess transformative mystical powers."
        )
        Nakshatra.MAGHA -> NakshatraData(
            sanskrit = "मघा",
            deity = "Pitris (Ancestors)",
            symbol = "Royal throne/Palanquin",
            gana = "Rakshasa",
            guna = "Tamas",
            animal = "Male Rat",
            quality = Quality.GOOD,
            description = "Magha is ruled by Ketu and represents ancestral power and royal heritage. Those born here possess natural authority, connection to lineage, and leadership qualities. They honor traditions and carry ancestral blessings."
        )
        Nakshatra.PURVA_PHALGUNI -> NakshatraData(
            sanskrit = "पूर्वा फाल्गुनी",
            deity = "Bhaga (God of Fortune)",
            symbol = "Front legs of bed/Hammock",
            gana = "Manushya",
            guna = "Tamas",
            animal = "Female Rat",
            quality = Quality.GOOD,
            description = "Purva Phalguni is ruled by Venus and represents creative pleasure and luxury. Those born here enjoy life's pleasures, are artistic, and possess natural charm. Associated with love, creativity, and enjoyment."
        )
        Nakshatra.UTTARA_PHALGUNI -> NakshatraData(
            sanskrit = "उत्तरा फाल्गुनी",
            deity = "Aryaman (God of Contracts)",
            symbol = "Back legs of bed",
            gana = "Manushya",
            guna = "Rajas",
            animal = "Male Cow",
            quality = Quality.EXCELLENT,
            description = "Uttara Phalguni is ruled by the Sun and represents lasting prosperity. Those born here are generous, friendly, and establish lasting partnerships. They excel in leadership and creating stable foundations."
        )
        Nakshatra.HASTA -> NakshatraData(
            sanskrit = "हस्त",
            deity = "Savitar (Sun God)",
            symbol = "Hand/Palm",
            gana = "Deva",
            guna = "Rajas",
            animal = "Female Buffalo",
            quality = Quality.EXCELLENT,
            description = "Hasta is ruled by the Moon and represents skill and dexterity. Those born here are clever, resourceful, and possess healing hands. They excel in crafts, arts, and activities requiring manual dexterity."
        )
        Nakshatra.CHITRA -> NakshatraData(
            sanskrit = "चित्रा",
            deity = "Vishwakarma (Divine Architect)",
            symbol = "Bright jewel/Pearl",
            gana = "Rakshasa",
            guna = "Rajas",
            animal = "Female Tiger",
            quality = Quality.GOOD,
            description = "Chitra is ruled by Mars and represents brilliant creativity. Those born here possess artistic talent, attention to detail, and the ability to create beautiful things. Associated with architecture, design, and craftsmanship."
        )
        Nakshatra.SWATI -> NakshatraData(
            sanskrit = "स्वाति",
            deity = "Vayu (Wind God)",
            symbol = "Young plant/Coral",
            gana = "Deva",
            guna = "Tamas",
            animal = "Male Buffalo",
            quality = Quality.GOOD,
            description = "Swati is ruled by Rahu and represents independence and flexibility. Those born here are adaptable, diplomatic, and value freedom. Like the wind, they can navigate various situations with grace."
        )
        Nakshatra.VISHAKHA -> NakshatraData(
            sanskrit = "विशाखा",
            deity = "Indra-Agni",
            symbol = "Triumphal archway/Potter's wheel",
            gana = "Rakshasa",
            guna = "Sattva",
            animal = "Male Tiger",
            quality = Quality.GOOD,
            description = "Vishakha is ruled by Jupiter and represents determined pursuit of goals. Those born here possess single-pointed focus, ambition, and the ability to achieve their objectives. Associated with transformation and success."
        )
        Nakshatra.ANURADHA -> NakshatraData(
            sanskrit = "अनुराधा",
            deity = "Mitra (God of Friendship)",
            symbol = "Lotus flower/Archway",
            gana = "Deva",
            guna = "Sattva",
            animal = "Female Deer",
            quality = Quality.EXCELLENT,
            description = "Anuradha is ruled by Saturn and represents devotion and friendship. Those born here are loyal, cooperative, and succeed in foreign lands. They possess the ability to create harmony and meaningful connections."
        )
        Nakshatra.JYESHTHA -> NakshatraData(
            sanskrit = "ज्येष्ठा",
            deity = "Indra (King of Gods)",
            symbol = "Circular amulet/Umbrella",
            gana = "Rakshasa",
            guna = "Sattva",
            animal = "Male Deer",
            quality = Quality.NEUTRAL,
            description = "Jyeshtha is ruled by Mercury and represents seniority and protection. Those born here possess leadership qualities, protective nature, and may face karmic challenges. They often rise to positions of authority and responsibility."
        )
        Nakshatra.MULA -> NakshatraData(
            sanskrit = "मूल",
            deity = "Nirriti (Goddess of Dissolution)",
            symbol = "Bundle of roots/Lion's tail",
            gana = "Rakshasa",
            guna = "Tamas",
            animal = "Male Dog",
            quality = Quality.CHALLENGING,
            description = "Mula is ruled by Ketu and represents getting to the root of things. Those born here experience transformative life events that lead to spiritual awakening. Associated with investigation, research, and uprooting the old."
        )
        Nakshatra.PURVA_ASHADHA -> NakshatraData(
            sanskrit = "पूर्वाषाढ़ा",
            deity = "Apas (Water Goddess)",
            symbol = "Elephant tusk/Fan",
            gana = "Manushya",
            guna = "Rajas",
            animal = "Male Monkey",
            quality = Quality.GOOD,
            description = "Purva Ashadha is ruled by Venus and represents invincible victory. Those born here possess charisma, philosophical nature, and the ability to influence others. They are optimistic and believe in ultimate success."
        )
        Nakshatra.UTTARA_ASHADHA -> NakshatraData(
            sanskrit = "उत्तराषाढ़ा",
            deity = "Vishwadevas (Universal Gods)",
            symbol = "Elephant tusk/Small bed",
            gana = "Manushya",
            guna = "Rajas",
            animal = "Female Mongoose",
            quality = Quality.EXCELLENT,
            description = "Uttara Ashadha is ruled by the Sun and represents final victory. Those born here achieve lasting success through righteousness and perseverance. They are principled, responsible, and earn respect through merit."
        )
        Nakshatra.SHRAVANA -> NakshatraData(
            sanskrit = "श्रवण",
            deity = "Vishnu (Preserver)",
            symbol = "Three footprints/Ear",
            gana = "Deva",
            guna = "Rajas",
            animal = "Female Monkey",
            quality = Quality.EXCELLENT,
            description = "Shravana is ruled by the Moon and represents learning through listening. Those born here are wise, knowledgeable, and possess excellent communication skills. Associated with preservation of knowledge and teaching."
        )
        Nakshatra.DHANISHTHA -> NakshatraData(
            sanskrit = "धनिष्ठा",
            deity = "Vasus (Eight Elemental Gods)",
            symbol = "Drum/Flute",
            gana = "Rakshasa",
            guna = "Tamas",
            animal = "Female Lion",
            quality = Quality.GOOD,
            description = "Dhanishtha is ruled by Mars and represents wealth and musical talent. Those born here possess rhythm, prosperity consciousness, and adaptability. Associated with fame, wealth, and artistic expression."
        )
        Nakshatra.SHATABHISHA -> NakshatraData(
            sanskrit = "शतभिषा",
            deity = "Varuna (God of Waters)",
            symbol = "Empty circle/100 flowers",
            gana = "Rakshasa",
            guna = "Tamas",
            animal = "Female Horse",
            quality = Quality.NEUTRAL,
            description = "Shatabhisha is ruled by Rahu and represents healing and veiling. Those born here possess healing abilities, secretive nature, and interest in mysteries. Associated with alternative healing and unconventional approaches."
        )
        Nakshatra.PURVA_BHADRAPADA -> NakshatraData(
            sanskrit = "पूर्वा भाद्रपद",
            deity = "Aja Ekapada (One-footed Goat)",
            symbol = "Front of funeral cot/Sword",
            gana = "Manushya",
            guna = "Sattva",
            animal = "Male Lion",
            quality = Quality.CHALLENGING,
            description = "Purva Bhadrapada is ruled by Jupiter and represents spiritual fire. Those born here possess intensity, occult interests, and transformative abilities. Associated with purification through spiritual heat and sacrifice."
        )
        Nakshatra.UTTARA_BHADRAPADA -> NakshatraData(
            sanskrit = "उत्तरा भाद्रपद",
            deity = "Ahir Budhnya (Serpent of the Deep)",
            symbol = "Back of funeral cot/Twins",
            gana = "Manushya",
            guna = "Sattva",
            animal = "Female Cow",
            quality = Quality.EXCELLENT,
            description = "Uttara Bhadrapada is ruled by Saturn and represents spiritual depth. Those born here possess wisdom, self-control, and compassionate nature. Associated with enlightenment, renunciation, and spiritual attainment."
        )
        Nakshatra.REVATI -> NakshatraData(
            sanskrit = "रेवती",
            deity = "Pushan (Nourisher/Protector)",
            symbol = "Fish/Drum",
            gana = "Deva",
            guna = "Sattva",
            animal = "Female Elephant",
            quality = Quality.EXCELLENT,
            description = "Revati is the final nakshatra, ruled by Mercury. Those born here are nurturing, wealthy, and possess safe travel. Associated with protection, guidance, and completion of cycles. Represents the journey's end and new beginnings."
        )
    }
}

private data class YogaData(
    val sanskrit: String,
    val meaning: String,
    val quality: Quality,
    val description: String
)

private fun getYogaData(yoga: Yoga): YogaData {
    return when (yoga) {
        Yoga.VISHKUMBHA -> YogaData("विष्कुम्भ", "Supporting", Quality.CHALLENGING, "Vishkumbha yoga indicates obstacles and difficulties. Those born under this yoga may face initial struggles but develop resilience. Not favorable for beginning new ventures.")
        Yoga.PRITI -> YogaData("प्रीति", "Love", Quality.EXCELLENT, "Priti yoga brings love, affection, and pleasant relationships. Those born under this yoga are charming and attract positive connections. Excellent for romance and partnerships.")
        Yoga.AYUSHMAN -> YogaData("आयुष्मान", "Long-lived", Quality.EXCELLENT, "Ayushman yoga bestows longevity and good health. Those born under this yoga enjoy vitality and well-being. Auspicious for health-related matters and new beginnings.")
        Yoga.SAUBHAGYA -> YogaData("सौभाग्य", "Good Fortune", Quality.EXCELLENT, "Saubhagya yoga brings luck and prosperity. Those born under this yoga are fortunate in material matters. Highly auspicious for wealth-related activities.")
        Yoga.SHOBHANA -> YogaData("शोभन", "Splendor", Quality.EXCELLENT, "Shobhana yoga bestows beauty, grace, and splendor. Those born under this yoga possess attractive qualities. Favorable for arts, aesthetics, and public appearances.")
        Yoga.ATIGANDA -> YogaData("अतिगण्ड", "Great Obstacle", Quality.INAUSPICIOUS, "Atiganda yoga indicates significant obstacles and dangers. Those born under this yoga face karmic challenges. Avoid important activities during this period.")
        Yoga.SUKARMA -> YogaData("सुकर्म", "Good Deeds", Quality.EXCELLENT, "Sukarma yoga favors righteous actions and good deeds. Those born under this yoga are inclined toward virtuous acts. Excellent for charitable activities and spiritual practices.")
        Yoga.DHRITI -> YogaData("धृति", "Steadiness", Quality.GOOD, "Dhriti yoga bestows determination and stability. Those born under this yoga possess mental strength and perseverance. Good for activities requiring patience and commitment.")
        Yoga.SHULA -> YogaData("शूल", "Spear", Quality.CHALLENGING, "Shula yoga indicates sharp experiences and potential conflicts. Those born under this yoga may face sudden challenges. Caution advised in risky activities.")
        Yoga.GANDA -> YogaData("गण्ड", "Obstacle", Quality.CHALLENGING, "Ganda yoga brings obstacles and difficulties. Those born under this yoga develop problem-solving abilities through challenges. Not ideal for new beginnings.")
        Yoga.VRIDDHI -> YogaData("वृद्धि", "Growth", Quality.EXCELLENT, "Vriddhi yoga promotes growth and expansion in all areas. Those born under this yoga experience progressive development. Excellent for investments and long-term projects.")
        Yoga.DHRUVA -> YogaData("ध्रुव", "Fixed", Quality.GOOD, "Dhruva yoga provides stability and permanence. Those born under this yoga create lasting foundations. Good for activities meant to endure, like property purchases.")
        Yoga.VYAGHATA -> YogaData("व्याघात", "Destruction", Quality.INAUSPICIOUS, "Vyaghata yoga indicates destruction and loss. Those born under this yoga may experience significant transformations. Avoid major decisions and beginnings.")
        Yoga.HARSHANA -> YogaData("हर्षण", "Joy", Quality.EXCELLENT, "Harshana yoga brings joy and happiness. Those born under this yoga spread positivity and attract good fortune. Excellent for celebrations and social activities.")
        Yoga.VAJRA -> YogaData("वज्र", "Thunderbolt", Quality.NEUTRAL, "Vajra yoga provides strength like a thunderbolt but can be harsh. Those born under this yoga are powerful and determined. Mixed results for activities.")
        Yoga.SIDDHI -> YogaData("सिद्धि", "Accomplishment", Quality.EXCELLENT, "Siddhi yoga bestows success and accomplishment. Those born under this yoga achieve their goals. Highly auspicious for completing important tasks.")
        Yoga.VYATIPATA -> YogaData("व्यतीपात", "Calamity", Quality.INAUSPICIOUS, "Vyatipata yoga is considered highly inauspicious. Those born under this yoga face significant karmic challenges. Avoid all important activities.")
        Yoga.VARIYAN -> YogaData("वरीयान", "Excellent", Quality.EXCELLENT, "Variyan yoga is one of the most auspicious yogas. Those born under this yoga enjoy success and recognition. Favorable for all important undertakings.")
        Yoga.PARIGHA -> YogaData("परिघ", "Obstruction", Quality.CHALLENGING, "Parigha yoga indicates barriers and restrictions. Those born under this yoga develop strength through overcoming obstacles. Caution in new ventures.")
        Yoga.SHIVA -> YogaData("शिव", "Auspicious", Quality.EXCELLENT, "Shiva yoga is highly auspicious and brings divine blessings. Those born under this yoga possess spiritual inclinations. Excellent for all auspicious activities.")
        Yoga.SIDDHA -> YogaData("सिद्ध", "Accomplished", Quality.EXCELLENT, "Siddha yoga bestows accomplishment and success. Those born under this yoga are naturally talented. Very favorable for achieving goals.")
        Yoga.SADHYA -> YogaData("साध्य", "Achievable", Quality.GOOD, "Sadhya yoga makes goals achievable with effort. Those born under this yoga succeed through determination. Good for pursuing challenging objectives.")
        Yoga.SHUBHA -> YogaData("शुभ", "Auspicious", Quality.EXCELLENT, "Shubha yoga is highly auspicious for all activities. Those born under this yoga attract good fortune. Excellent for ceremonies and celebrations.")
        Yoga.SHUKLA -> YogaData("शुक्ल", "Bright", Quality.EXCELLENT, "Shukla yoga brings brightness and clarity. Those born under this yoga have clear minds and pure intentions. Favorable for education and spiritual practices.")
        Yoga.BRAHMA -> YogaData("ब्रह्म", "Creator", Quality.EXCELLENT, "Brahma yoga bestows creative powers and wisdom. Those born under this yoga are natural creators. Excellent for beginning creative projects.")
        Yoga.INDRA -> YogaData("इन्द्र", "King of Gods", Quality.EXCELLENT, "Indra yoga bestows power, authority, and success. Those born under this yoga are natural leaders. Favorable for leadership and governmental matters.")
        Yoga.VAIDHRITI -> YogaData("वैधृति", "Discord", Quality.INAUSPICIOUS, "Vaidhriti yoga is the final yoga and indicates discord. Those born under this yoga face endings and transitions. Avoid important beginnings; good for conclusions.")
    }
}

private data class KaranaData(
    val sanskrit: String,
    val type: String,
    val quality: Quality,
    val description: String
)

private fun getKaranaData(karana: Karana): KaranaData {
    return when (karana) {
        Karana.BAVA -> KaranaData("बव", "Chara (Movable)", Quality.GOOD, "Bava karana is favorable for travel, beginning new ventures, and activities requiring movement. It supports progress and forward motion in all endeavors.")
        Karana.BALAVA -> KaranaData("बालव", "Chara (Movable)", Quality.GOOD, "Balava karana brings youthful energy and is good for learning, education, and activities involving young people. Favorable for new learning experiences.")
        Karana.KAULAVA -> KaranaData("कौलव", "Chara (Movable)", Quality.GOOD, "Kaulava karana is excellent for family matters, social gatherings, and community activities. It supports harmony in relationships and group endeavors.")
        Karana.TAITILA -> KaranaData("तैतिल", "Chara (Movable)", Quality.EXCELLENT, "Taitila karana is highly favorable for business, commerce, and material gain. Excellent for financial transactions and wealth-building activities.")
        Karana.GARA -> KaranaData("गर", "Chara (Movable)", Quality.GOOD, "Gara karana is good for agriculture, construction, and activities involving the earth. Favorable for property matters and physical labor.")
        Karana.VANIJA -> KaranaData("वणिज", "Chara (Movable)", Quality.EXCELLENT, "Vanija karana is excellent for trade, business deals, and commercial success. Highly favorable for merchants and those in business.")
        Karana.VISHTI -> KaranaData("विष्टि", "Chara (Movable)", Quality.INAUSPICIOUS, "Vishti (Bhadra) karana is inauspicious and should be avoided for important activities. Only suitable for fierce activities or confronting enemies.")
        Karana.SHAKUNI -> KaranaData("शकुनि", "Sthira (Fixed)", Quality.NEUTRAL, "Shakuni is a fixed karana appearing only once per lunar month. It supports activities requiring cleverness and strategy but can bring mixed results.")
        Karana.CHATUSHPADA -> KaranaData("चतुष्पद", "Sthira (Fixed)", Quality.GOOD, "Chatushpada is a fixed karana favorable for activities involving four-legged animals and agriculture. Good for animal husbandry and farming.")
        Karana.NAGA -> KaranaData("नाग", "Sthira (Fixed)", Quality.NEUTRAL, "Naga is a fixed karana associated with serpent energies. Favorable for activities involving hidden matters, underground work, and protective rituals.")
        Karana.KIMSTUGHNA -> KaranaData("किंस्तुघ्न", "Sthira (Fixed)", Quality.GOOD, "Kimstughna is a fixed karana that destroys negativity. Favorable for removing obstacles, protection rituals, and overcoming enemies.")
    }
}

private data class VaraData(
    val sanskrit: String,
    val element: String,
    val direction: String,
    val description: String,
    val favorable: List<String>,
    val unfavorable: List<String>
)

private fun getVaraData(vara: Vara): VaraData {
    return when (vara) {
        Vara.SUNDAY -> VaraData(
            sanskrit = "रविवार",
            element = "Fire",
            direction = "East",
            description = "Sunday is ruled by the Sun (Surya), representing the soul, authority, and vitality. The Sun's energy brings power, leadership, and illumination. This day is sacred to Lord Vishnu and Surya Deva.",
            favorable = listOf("Government work", "Authority matters", "Health initiatives", "Father-related activities", "Leadership", "Spiritual practices", "Worship of Surya"),
            unfavorable = listOf("Beginning journeys to the East", "Starting new businesses (according to some traditions)")
        )
        Vara.MONDAY -> VaraData(
            sanskrit = "सोमवार",
            element = "Water",
            direction = "Northwest",
            description = "Monday is ruled by the Moon (Chandra), representing the mind, emotions, and nurturing. The Moon's energy brings sensitivity, intuition, and public connection. This day is sacred to Lord Shiva.",
            favorable = listOf("Travel", "Public dealings", "Emotional matters", "Starting ventures", "Agriculture", "Dealing with women", "Shiva worship"),
            unfavorable = listOf("Surgery", "Cutting activities")
        )
        Vara.TUESDAY -> VaraData(
            sanskrit = "मंगलवार",
            element = "Fire",
            direction = "South",
            description = "Tuesday is ruled by Mars (Mangal), representing courage, energy, and action. Mars brings warrior energy, determination, and physical strength. This day is sacred to Lord Hanuman and Kartikeya.",
            favorable = listOf("Property matters", "Surgery", "Competitive activities", "Physical training", "Dealing with enemies", "Hanuman worship", "Military activities"),
            unfavorable = listOf("Marriage", "Peaceful negotiations", "Beginning gentle activities")
        )
        Vara.WEDNESDAY -> VaraData(
            sanskrit = "बुधवार",
            element = "Earth",
            direction = "North",
            description = "Wednesday is ruled by Mercury (Budha), representing intellect, communication, and commerce. Mercury brings mental agility, business acumen, and learning ability. This day is sacred to Lord Vishnu.",
            favorable = listOf("Education", "Communication", "Business", "Writing", "Trade", "Intellectual pursuits", "Travel", "Vishnu worship"),
            unfavorable = listOf("Agriculture", "Building construction (according to some traditions)")
        )
        Vara.THURSDAY -> VaraData(
            sanskrit = "गुरुवार",
            element = "Ether",
            direction = "Northeast",
            description = "Thursday is ruled by Jupiter (Guru), representing wisdom, expansion, and divine grace. Jupiter brings blessings, prosperity, and spiritual growth. This day is sacred to Lord Brihaspati and Vishnu.",
            favorable = listOf("Religious ceremonies", "Marriage", "Education", "Financial matters", "Legal matters", "Guru worship", "Charity", "Beginning important ventures"),
            unfavorable = listOf("Hair cutting (for some traditions)", "Lending money")
        )
        Vara.FRIDAY -> VaraData(
            sanskrit = "शुक्रवार",
            element = "Water",
            direction = "Southeast",
            description = "Friday is ruled by Venus (Shukra), representing love, beauty, and pleasure. Venus brings artistic sensibility, romance, and material enjoyment. This day is sacred to Goddess Lakshmi and Santoshi Ma.",
            favorable = listOf("Romance", "Marriage", "Arts", "Music", "Luxury purchases", "Beauty treatments", "Entertainment", "Lakshmi worship"),
            unfavorable = listOf("Surgery", "Aggressive activities", "Conflicts")
        )
        Vara.SATURDAY -> VaraData(
            sanskrit = "शनिवार",
            element = "Air",
            direction = "West",
            description = "Saturday is ruled by Saturn (Shani), representing discipline, karma, and endurance. Saturn brings lessons, structure, and spiritual growth through challenges. This day is sacred to Lord Shani and Hanuman.",
            favorable = listOf("Property matters", "Agriculture", "Labor work", "Iron/oil business", "Spiritual discipline", "Shani worship", "Hanuman worship"),
            unfavorable = listOf("New beginnings", "Travel", "Auspicious ceremonies", "Hair cutting")
        )
    }
}