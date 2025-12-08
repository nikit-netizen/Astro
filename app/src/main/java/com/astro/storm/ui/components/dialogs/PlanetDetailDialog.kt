package com.astro.storm.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.stringResource
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import com.astro.storm.ephemeris.PlanetaryShadbala
import com.astro.storm.ephemeris.RetrogradeCombustionCalculator
import com.astro.storm.ephemeris.ShadbalaCalculator

/**
 * Comprehensive planet detail dialog showing position, strength, and interpretations.
 */
@Composable
fun PlanetDetailDialog(
    planetPosition: PlanetPosition,
    chart: VedicChart,
    onDismiss: () -> Unit
) {
    val shadbala = remember(chart) {
        ShadbalaCalculator.calculatePlanetShadbala(planetPosition, chart)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = DialogColors.DialogBackground
        ) {
            Column {
                PlanetDialogHeader(planetPosition, onDismiss)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { PlanetPositionCard(planetPosition) }
                    item { ShadbalaCard(shadbala) }
                    item { SignificationsCard(planetPosition.planet) }
                    item { HousePlacementCard(planetPosition) }
                    item { PlanetStatusCard(planetPosition, chart) }
                    item { PredictionsCard(planetPosition, shadbala) }
                }
            }
        }
    }
}

@Composable
private fun PlanetDialogHeader(
    planetPosition: PlanetPosition,
    onDismiss: () -> Unit
) {
    val planetColor = DialogColors.getPlanetColor(planetPosition.planet)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DialogColors.DialogSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(planetColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = planetPosition.planet.symbol,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = planetPosition.planet.displayName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DialogColors.TextPrimary
                    )
                    Text(
                        text = "${planetPosition.sign.displayName} • ${stringResource(StringKey.HOUSE)} ${planetPosition.house}",
                        fontSize = 14.sp,
                        color = DialogColors.TextSecondary
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(StringKey.DIALOG_CLOSE), tint = DialogColors.TextPrimary)
            }
        }
    }
}

@Composable
private fun PlanetPositionCard(position: PlanetPosition) {
    DialogCard(title = stringResource(StringKey.DIALOG_POSITION_DETAILS), icon = Icons.Outlined.LocationOn) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailRow(stringResource(StringKey.DIALOG_ZODIAC_SIGN), position.sign.displayName, DialogColors.AccentTeal)
            DetailRow(stringResource(StringKey.DIALOG_DEGREE), formatDegree(position.longitude), DialogColors.TextPrimary)
            DetailRow(stringResource(StringKey.DIALOG_HOUSE), "${stringResource(StringKey.HOUSE)} ${position.house}", DialogColors.AccentGold)
            DetailRow(stringResource(StringKey.DIALOG_NAKSHATRA), "${position.nakshatra.displayName} (${stringResource(StringKey.PANCHANGA_PADA)} ${position.nakshatraPada})", DialogColors.AccentPurple)
            DetailRow(stringResource(StringKey.DIALOG_NAKSHATRA_LORD), position.nakshatra.ruler.displayName, DialogColors.TextSecondary)
            DetailRow(stringResource(StringKey.DIALOG_NAKSHATRA_DEITY), position.nakshatra.deity, DialogColors.TextSecondary)
            if (position.isRetrograde) {
                DetailRow(stringResource(StringKey.DIALOG_MOTION), stringResource(StringKey.DIALOG_RETROGRADE), DialogColors.AccentOrange)
            }
        }
    }
}

@Composable
private fun ShadbalaCard(shadbala: PlanetaryShadbala) {
    DialogCard(title = stringResource(StringKey.DIALOG_STRENGTH_ANALYSIS), icon = Icons.Outlined.TrendingUp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val strengthPercentage = (shadbala.percentageOfRequired / 150.0).coerceIn(0.0, 1.0).toFloat()
            val color = DialogColors.getStrengthColor(shadbala.percentageOfRequired)

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(StringKey.DIALOG_OVERALL, String.format("%.2f", shadbala.totalRupas), String.format("%.2f", shadbala.requiredRupas)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DialogColors.TextPrimary
                    )
                    Text(
                        text = shadbala.strengthRating.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { strengthPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = DialogColors.DividerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(StringKey.DIALOG_PERCENT_OF_REQUIRED, String.format("%.1f", shadbala.percentageOfRequired)),
                    fontSize = 12.sp,
                    color = DialogColors.TextMuted
                )
            }

            HorizontalDivider(color = DialogColors.DividerColor)

            Text(stringResource(StringKey.DIALOG_STRENGTH_BREAKDOWN), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DialogColors.TextSecondary)

            StrengthRow(stringResource(StringKey.DIALOG_STHANA_BALA), shadbala.sthanaBala.total, 180.0)
            StrengthRow(stringResource(StringKey.DIALOG_DIG_BALA), shadbala.digBala, 60.0)
            StrengthRow(stringResource(StringKey.DIALOG_KALA_BALA), shadbala.kalaBala.total, 180.0)
            StrengthRow(stringResource(StringKey.DIALOG_CHESTA_BALA), shadbala.chestaBala, 60.0)
            StrengthRow(stringResource(StringKey.DIALOG_NAISARGIKA_BALA), shadbala.naisargikaBala, 60.0)
            StrengthRow(stringResource(StringKey.DIALOG_DRIK_BALA), shadbala.drikBala, 60.0)
        }
    }
}

@Composable
private fun SignificationsCard(planet: Planet) {
    val significations = getPlanetSignifications(planet)

    DialogCard(title = stringResource(StringKey.DIALOG_SIGNIFICATIONS), icon = Icons.Outlined.Info) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailRow(stringResource(StringKey.DIALOG_NATURE), significations.nature, when (significations.nature) {
                "Benefic" -> DialogColors.AccentGreen
                "Malefic" -> DialogColors.AccentRose
                else -> DialogColors.AccentOrange
            })
            DetailRow(stringResource(StringKey.DIALOG_ELEMENT), significations.element, DialogColors.TextSecondary)

            Text(stringResource(StringKey.DIALOG_REPRESENTS), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DialogColors.TextSecondary)
            significations.represents.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(DialogColors.AccentGold, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item, fontSize = 13.sp, color = DialogColors.TextPrimary)
                }
            }

            Text(stringResource(StringKey.DIALOG_BODY_PARTS), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DialogColors.TextSecondary)
            Text(text = significations.bodyParts, fontSize = 13.sp, color = DialogColors.TextPrimary)

            Text(stringResource(StringKey.DIALOG_PROFESSIONS), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DialogColors.TextSecondary)
            Text(text = significations.professions, fontSize = 13.sp, color = DialogColors.TextPrimary)
        }
    }
}

@Composable
private fun HousePlacementCard(position: PlanetPosition) {
    val interpretation = getHousePlacementInterpretation(position.planet, position.house)

    DialogCard(title = stringResource(StringKey.DIALOG_HOUSE_PLACEMENT, position.house), icon = Icons.Outlined.Home) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = interpretation.houseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DialogColors.AccentGold
            )
            Text(
                text = interpretation.houseSignification,
                fontSize = 13.sp,
                color = DialogColors.TextSecondary
            )
            HorizontalDivider(color = DialogColors.DividerColor)
            Text(
                text = interpretation.interpretation,
                fontSize = 14.sp,
                color = DialogColors.TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanetStatusCard(position: PlanetPosition, chart: VedicChart) {
    val conditions = remember(chart) {
        RetrogradeCombustionCalculator.analyzePlanetaryConditions(chart)
    }
    val planetCondition = conditions.getCondition(position.planet)

    DialogCard(title = stringResource(StringKey.DIALOG_STATUS_CONDITIONS), icon = Icons.Outlined.FactCheck) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val dignity = getDignity(position.planet, position.sign)
            StatusChip(label = stringResource(StringKey.DIALOG_DIGNITY), value = dignity.status, color = dignity.color)

            if (position.isRetrograde) {
                StatusChip(label = stringResource(StringKey.DIALOG_MOTION), value = stringResource(StringKey.DIALOG_RETROGRADE), color = DialogColors.AccentOrange)
            }

            planetCondition?.let { cond ->
                if (cond.combustionStatus != RetrogradeCombustionCalculator.CombustionStatus.NOT_COMBUST) {
                    StatusChip(
                        label = stringResource(StringKey.DIALOG_COMBUSTION),
                        value = cond.combustionStatus.displayName,
                        color = DialogColors.AccentRose
                    )
                }

                if (cond.isInPlanetaryWar) {
                    StatusChip(
                        label = stringResource(StringKey.DIALOG_PLANETARY_WAR),
                        value = stringResource(StringKey.DIALOG_AT_WAR_WITH, cond.warData?.loser?.displayName ?: ""),
                        color = DialogColors.AccentPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun PredictionsCard(
    position: PlanetPosition,
    shadbala: PlanetaryShadbala
) {
    val predictions = getPlanetPredictions(position, shadbala)

    DialogCard(title = stringResource(StringKey.DIALOG_INSIGHTS_PREDICTIONS), icon = Icons.Outlined.AutoAwesome) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            predictions.forEach { prediction ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        when (prediction.type) {
                            PredictionType.POSITIVE -> Icons.Default.CheckCircle
                            PredictionType.NEGATIVE -> Icons.Default.Warning
                            PredictionType.NEUTRAL -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (prediction.type) {
                            PredictionType.POSITIVE -> DialogColors.AccentGreen
                            PredictionType.NEGATIVE -> DialogColors.AccentOrange
                            PredictionType.NEUTRAL -> DialogColors.AccentBlue
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = prediction.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DialogColors.TextPrimary
                        )
                        Text(
                            text = prediction.description,
                            fontSize = 13.sp,
                            color = DialogColors.TextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// Helper data classes and functions

data class PlanetSignifications(
    val nature: String,
    val element: String,
    val represents: List<String>,
    val bodyParts: String,
    val professions: String
)

data class HousePlacementInterpretation(
    val houseName: String,
    val houseSignification: String,
    val interpretation: String
)

data class Dignity(val status: String, val color: Color)

data class Prediction(
    val type: PredictionType,
    val title: String,
    val description: String
)

enum class PredictionType { POSITIVE, NEGATIVE, NEUTRAL }

private fun formatDegree(degree: Double): String {
    val normalizedDegree = (degree % 360.0 + 360.0) % 360.0
    val deg = normalizedDegree.toInt()
    val min = ((normalizedDegree - deg) * 60).toInt()
    val sec = ((((normalizedDegree - deg) * 60) - min) * 60).toInt()
    return "$deg° $min' $sec\""
}

private fun getPlanetSignifications(planet: Planet): PlanetSignifications {
    return when (planet) {
        Planet.SUN -> PlanetSignifications(
            nature = "Malefic",
            element = "Fire",
            represents = listOf("Soul, Self, Ego", "Father, Authority Figures", "Government, Power", "Health, Vitality", "Fame, Recognition"),
            bodyParts = "Heart, Spine, Right Eye, Bones",
            professions = "Government jobs, Politics, Medicine, Administration, Leadership roles"
        )
        Planet.MOON -> PlanetSignifications(
            nature = "Benefic",
            element = "Water",
            represents = listOf("Mind, Emotions", "Mother, Nurturing", "Public, Masses", "Comforts, Happiness", "Memory, Imagination"),
            bodyParts = "Mind, Left Eye, Breast, Blood, Fluids",
            professions = "Nursing, Hotel industry, Shipping, Agriculture, Psychology"
        )
        Planet.MARS -> PlanetSignifications(
            nature = "Malefic",
            element = "Fire",
            represents = listOf("Energy, Action, Courage", "Siblings, Younger Brothers", "Property, Land", "Competition, Sports", "Technical Skills"),
            bodyParts = "Blood, Muscles, Marrow, Head injuries",
            professions = "Military, Police, Surgery, Engineering, Sports, Real Estate"
        )
        Planet.MERCURY -> PlanetSignifications(
            nature = "Benefic",
            element = "Earth",
            represents = listOf("Intelligence, Communication", "Learning, Education", "Business, Trade", "Writing, Speech", "Siblings, Friends"),
            bodyParts = "Nervous system, Skin, Speech, Hands",
            professions = "Writing, Teaching, Accounting, Trading, IT, Media"
        )
        Planet.JUPITER -> PlanetSignifications(
            nature = "Benefic",
            element = "Ether",
            represents = listOf("Wisdom, Knowledge", "Teachers, Gurus", "Fortune, Luck", "Children, Dharma", "Expansion, Growth"),
            bodyParts = "Liver, Fat tissue, Ears, Thighs",
            professions = "Teaching, Law, Priesthood, Banking, Counseling"
        )
        Planet.VENUS -> PlanetSignifications(
            nature = "Benefic",
            element = "Water",
            represents = listOf("Love, Beauty, Art", "Marriage, Relationships", "Luxuries, Comforts", "Vehicles, Pleasures", "Creativity"),
            bodyParts = "Reproductive system, Face, Skin, Throat",
            professions = "Entertainment, Fashion, Art, Hospitality, Beauty industry"
        )
        Planet.SATURN -> PlanetSignifications(
            nature = "Malefic",
            element = "Air",
            represents = listOf("Discipline, Hard work", "Karma, Delays", "Longevity, Service", "Laborers, Servants", "Chronic issues"),
            bodyParts = "Bones, Teeth, Knees, Joints, Nerves",
            professions = "Mining, Agriculture, Labor, Judiciary, Real Estate"
        )
        Planet.RAHU -> PlanetSignifications(
            nature = "Malefic",
            element = "Air",
            represents = listOf("Obsession, Illusion", "Foreign lands, Travel", "Technology, Innovation", "Unconventional paths", "Material desires"),
            bodyParts = "Skin diseases, Nervous disorders",
            professions = "Technology, Foreign affairs, Aviation, Politics, Research"
        )
        Planet.KETU -> PlanetSignifications(
            nature = "Malefic",
            element = "Fire",
            represents = listOf("Spirituality, Liberation", "Past life karma", "Detachment, Isolation", "Occult, Mysticism", "Healing abilities"),
            bodyParts = "Skin, Spine, Nervous system",
            professions = "Spirituality, Research, Healing, Astrology, Philosophy"
        )
        else -> PlanetSignifications("", "", emptyList(), "", "")
    }
}

@Composable
private fun getHousePlacementInterpretation(planet: Planet, house: Int): HousePlacementInterpretation {
    val houseNameKeys = listOf(
        null, StringKey.HOUSE_1_NAME, StringKey.HOUSE_2_NAME, StringKey.HOUSE_3_NAME,
        StringKey.HOUSE_4_NAME, StringKey.HOUSE_5_NAME, StringKey.HOUSE_6_NAME,
        StringKey.HOUSE_7_NAME, StringKey.HOUSE_8_NAME, StringKey.HOUSE_9_NAME,
        StringKey.HOUSE_10_NAME, StringKey.HOUSE_11_NAME, StringKey.HOUSE_12_NAME
    )

    val houseSigKeys = listOf(
        null, StringKey.HOUSE_1_SIG, StringKey.HOUSE_2_SIG, StringKey.HOUSE_3_SIG,
        StringKey.HOUSE_4_SIG, StringKey.HOUSE_5_SIG, StringKey.HOUSE_6_SIG,
        StringKey.HOUSE_7_SIG, StringKey.HOUSE_8_SIG, StringKey.HOUSE_9_SIG,
        StringKey.HOUSE_10_SIG, StringKey.HOUSE_11_SIG, StringKey.HOUSE_12_SIG
    )

    val houseName = houseNameKeys.getOrNull(house)?.let { stringResource(it) } ?: "${stringResource(StringKey.HOUSE)} $house"
    val houseSignification = houseSigKeys.getOrNull(house)?.let { stringResource(it) } ?: ""
    val interpretation = "${planet.displayName} ${stringResource(StringKey.DIALOG_HOUSE)} $house"

    return HousePlacementInterpretation(
        houseName = houseName,
        houseSignification = houseSignification,
        interpretation = interpretation
    )
}

@Composable
private fun getDignity(planet: Planet, sign: ZodiacSign): Dignity {
    val exalted = mapOf(
        Planet.SUN to ZodiacSign.ARIES,
        Planet.MOON to ZodiacSign.TAURUS,
        Planet.MARS to ZodiacSign.CAPRICORN,
        Planet.MERCURY to ZodiacSign.VIRGO,
        Planet.JUPITER to ZodiacSign.CANCER,
        Planet.VENUS to ZodiacSign.PISCES,
        Planet.SATURN to ZodiacSign.LIBRA
    )
    if (exalted[planet] == sign) return Dignity(stringResource(StringKey.PLANETARY_STATUS_EXALTED), DialogColors.AccentGreen)

    val debilitated = mapOf(
        Planet.SUN to ZodiacSign.LIBRA,
        Planet.MOON to ZodiacSign.SCORPIO,
        Planet.MARS to ZodiacSign.CANCER,
        Planet.MERCURY to ZodiacSign.PISCES,
        Planet.JUPITER to ZodiacSign.CAPRICORN,
        Planet.VENUS to ZodiacSign.VIRGO,
        Planet.SATURN to ZodiacSign.ARIES
    )
    if (debilitated[planet] == sign) return Dignity(stringResource(StringKey.PLANETARY_STATUS_DEBILITATED), DialogColors.AccentRose)

    if (sign.ruler == planet) return Dignity(stringResource(StringKey.PLANETARY_STATUS_OWN_SIGN), DialogColors.AccentGold)

    return Dignity(stringResource(StringKey.RELATION_NEUTRAL), DialogColors.TextSecondary)
}

@Composable
private fun getPlanetPredictions(
    position: PlanetPosition,
    shadbala: PlanetaryShadbala
): List<Prediction> {
    val predictions = mutableListOf<Prediction>()
    val planet = position.planet

    if (shadbala.isStrong) {
        predictions.add(Prediction(
            PredictionType.POSITIVE,
            stringResource(StringKey.PREDICTION_STRONG_PLANET, planet.displayName),
            stringResource(StringKey.PREDICTION_STRONG_DESC)
        ))
    } else {
        predictions.add(Prediction(
            PredictionType.NEGATIVE,
            stringResource(StringKey.PREDICTION_WEAK_PLANET, planet.displayName),
            stringResource(StringKey.PREDICTION_WEAK_DESC)
        ))
    }

    val dignity = getDignity(planet, position.sign)
    val exaltedStatus = stringResource(StringKey.PLANETARY_STATUS_EXALTED)
    val debilitatedStatus = stringResource(StringKey.PLANETARY_STATUS_DEBILITATED)
    val ownSignStatus = stringResource(StringKey.PLANETARY_STATUS_OWN_SIGN)

    when (dignity.status) {
        exaltedStatus -> predictions.add(Prediction(
            PredictionType.POSITIVE,
            stringResource(StringKey.PREDICTION_EXALTED),
            stringResource(StringKey.PREDICTION_EXALTED_DESC, planet.displayName)
        ))
        debilitatedStatus -> predictions.add(Prediction(
            PredictionType.NEGATIVE,
            stringResource(StringKey.PREDICTION_DEBILITATED),
            stringResource(StringKey.PREDICTION_DEBILITATED_DESC, planet.displayName)
        ))
        ownSignStatus -> predictions.add(Prediction(
            PredictionType.POSITIVE,
            stringResource(StringKey.PREDICTION_OWN_SIGN),
            stringResource(StringKey.PREDICTION_OWN_SIGN_DESC, planet.displayName)
        ))
    }

    if (position.isRetrograde) {
        predictions.add(Prediction(
            PredictionType.NEUTRAL,
            stringResource(StringKey.PREDICTION_RETROGRADE),
            stringResource(StringKey.PREDICTION_RETROGRADE_DESC)
        ))
    }

    return predictions
}
