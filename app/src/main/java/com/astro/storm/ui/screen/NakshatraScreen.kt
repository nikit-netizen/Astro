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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.StringKeyDosha
import com.astro.storm.data.localization.StringKeyAnalysis
import com.astro.storm.data.localization.StringKeyMatch
import com.astro.storm.data.localization.StringResources
import com.astro.storm.data.localization.currentLanguage
import com.astro.storm.data.localization.stringResource
import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import com.astro.storm.ui.theme.AppTheme

/**
 * Nakshatra Analysis Screen
 *
 * Comprehensive Vedic astrology nakshatra (lunar mansion) analysis featuring:
 * - Birth nakshatra details with deity, ruler, and characteristics
 * - All planetary nakshatra placements
 * - Nakshatra compatibility analysis (Tarabala, Chandrabala)
 * - Personalized nakshatra-based remedies
 * - Detailed nakshatra attributes and symbolism
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NakshatraScreen(
    chart: VedicChart?,
    onBack: () -> Unit
) {
    if (chart == null) {
        EmptyChartScreen(
            title = stringResource(StringKeyDosha.NAKSHATRA_TITLE),
            message = stringResource(StringKey.NO_PROFILE_MESSAGE),
            onBack = onBack
        )
        return
    }

    val language = currentLanguage()
    val clipboardManager = LocalClipboardManager.current
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedPlanet by remember { mutableStateOf<Planet?>(null) }

    val tabs = listOf(
        stringResource(StringKeyDosha.NAKSHATRA_OVERVIEW),
        stringResource(StringKeyDosha.NAKSHATRA_DETAILS),
        stringResource(StringKeyDosha.NAKSHATRA_COMPATIBILITY),
        stringResource(StringKeyDosha.NAKSHATRA_REMEDIES)
    )

    // Calculate nakshatra analysis
    val nakshatraAnalysis = remember(chart) {
        calculateNakshatraAnalysis(chart, language)
    }

    if (showInfoDialog) {
        NakshatraInfoDialog(onDismiss = { showInfoDialog = false })
    }

    Scaffold(
        containerColor = AppTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(StringKeyDosha.NAKSHATRA_TITLE),
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
                            contentDescription = stringResource(StringKeyDosha.NAKSHATRA_INFO_TITLE),
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.ScreenBackground),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Tab selector
            item {
                NakshatraTabSelector(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }

            // Tab content
            when (selectedTab) {
                0 -> item { NakshatraOverviewTab(analysis = nakshatraAnalysis, chart = chart, language = language) }
                1 -> item { NakshatraDetailsTab(analysis = nakshatraAnalysis, expandedPlanet = expandedPlanet, onExpandPlanet = { expandedPlanet = if (expandedPlanet == it) null else it }, language = language) }
                2 -> item { NakshatraCompatibilityTab(analysis = nakshatraAnalysis, language = language) }
                3 -> item { NakshatraRemediesTab(analysis = nakshatraAnalysis, onCopyMantra = { mantra -> clipboardManager.setText(AnnotatedString(mantra)) }, language = language) }
            }
        }
    }
}

// ============================================
// Data Classes for Nakshatra Analysis
// ============================================

data class NakshatraDetails(
    val nakshatra: Nakshatra,
    val pada: Int,
    val degreeInNakshatra: Double,
    val deity: String,
    val symbol: String,
    val nature: NakshatraNature,
    val gana: NakshatraGana,
    val animal: String,
    val element: NakshatraElement,
    val caste: NakshatraCaste,
    val direction: String,
    val bodyPart: String,
    val gender: NakshatraGender,
    val dosha: NakshatraDosha,
    val qualities: List<String>,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val careerAptitudes: List<String>,
    val mantra: String
)

enum class NakshatraNature(val displayName: String, val nepaliName: String) {
    FIXED("Fixed (Dhruva)", "स्थिर"),
    MOVEABLE("Moveable (Chara)", "चर"),
    SHARP("Sharp (Tikshna)", "तीक्ष्ण"),
    SOFT("Soft (Mridu)", "मृदु"),
    MIXED("Mixed (Mishra)", "मिश्र"),
    LIGHT("Light (Laghu)", "लघु"),
    FIERCE("Fierce (Ugra)", "उग्र");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

enum class NakshatraGana(val displayName: String, val nepaliName: String) {
    DEVA("Deva (Divine)", "देव"),
    MANUSHYA("Manushya (Human)", "मनुष्य"),
    RAKSHASA("Rakshasa (Demon)", "राक्षस");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

enum class NakshatraElement(val displayName: String, val nepaliName: String) {
    FIRE("Fire (Agni)", "अग्नि"),
    EARTH("Earth (Prithvi)", "पृथ्वी"),
    AIR("Air (Vayu)", "वायु"),
    WATER("Water (Jala)", "जल"),
    ETHER("Ether (Akasha)", "आकाश");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

enum class NakshatraCaste(val displayName: String, val nepaliName: String) {
    BRAHMIN("Brahmin", "ब्राह्मण"),
    KSHATRIYA("Kshatriya", "क्षत्रिय"),
    VAISHYA("Vaishya", "वैश्य"),
    SHUDRA("Shudra", "शूद्र");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

enum class NakshatraGender(val displayName: String, val nepaliName: String) {
    MALE("Male", "पुरुष"),
    FEMALE("Female", "स्त्री"),
    NEUTRAL("Neutral", "नपुंसक");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

enum class NakshatraDosha(val displayName: String, val nepaliName: String) {
    VATA("Vata", "वात"),
    PITTA("Pitta", "पित्त"),
    KAPHA("Kapha", "कफ");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

data class PlanetaryNakshatra(
    val planet: Planet,
    val position: PlanetPosition,
    val details: NakshatraDetails
)

data class TarabalaResult(
    val nakshatraName: String,
    val taraNumber: Int,
    val taraName: String,
    val isFavorable: Boolean,
    val description: String
)

data class NakshatraCompatibility(
    val vedhaPartners: List<Nakshatra>,
    val rajjuType: RajjuType,
    val favorableNakshatras: List<Nakshatra>,
    val unfavorableNakshatras: List<Nakshatra>,
    val tarabalaResults: List<TarabalaResult>
)

enum class RajjuType(val displayName: String, val nepaliName: String, val bodyPart: String) {
    PAADA("Paada", "पाद", "Feet"),
    KATI("Kati", "कटि", "Waist"),
    NABHI("Nabhi", "नाभि", "Navel"),
    KANTHA("Kantha", "कण्ठ", "Neck"),
    SHIRO("Shiro", "शिरो", "Head");

    fun getLocalizedName(language: Language): String =
        if (language == Language.NEPALI) nepaliName else displayName
}

data class NakshatraRemedy(
    val title: String,
    val description: String,
    val mantra: String,
    val timing: String,
    val gemstone: String?,
    val deity: String,
    val favorableDays: List<String>,
    val luckyNumbers: List<Int>,
    val luckyColors: List<String>
)

data class NakshatraAnalysis(
    val birthNakshatra: NakshatraDetails,
    val moonNakshatra: NakshatraDetails,
    val planetaryNakshatras: List<PlanetaryNakshatra>,
    val compatibility: NakshatraCompatibility,
    val remedy: NakshatraRemedy
)

// ============================================
// Nakshatra Calculation Engine
// ============================================

private fun calculateNakshatraAnalysis(chart: VedicChart, language: Language): NakshatraAnalysis {
    val moonPosition = chart.planetPositions.find { it.planet == Planet.MOON }
        ?: throw IllegalStateException("Moon position not found in chart")

    val birthNakshatra = calculateNakshatraDetails(moonPosition.nakshatra, moonPosition.nakshatraPada, moonPosition.longitude)
    val moonNakshatra = birthNakshatra // Same as birth nakshatra for moon

    val planetaryNakshatras = chart.planetPositions.map { pos ->
        PlanetaryNakshatra(
            planet = pos.planet,
            position = pos,
            details = calculateNakshatraDetails(pos.nakshatra, pos.nakshatraPada, pos.longitude)
        )
    }

    val compatibility = calculateNakshatraCompatibility(birthNakshatra.nakshatra)
    val remedy = calculateNakshatraRemedy(birthNakshatra, language)

    return NakshatraAnalysis(
        birthNakshatra = birthNakshatra,
        moonNakshatra = moonNakshatra,
        planetaryNakshatras = planetaryNakshatras,
        compatibility = compatibility,
        remedy = remedy
    )
}

private fun calculateNakshatraDetails(nakshatra: Nakshatra, pada: Int, longitude: Double): NakshatraDetails {
    val nakshatraSpan = 360.0 / 27.0
    val normalizedLong = ((longitude % 360.0) + 360.0) % 360.0
    val degreeInNakshatra = (normalizedLong % nakshatraSpan)

    return NakshatraDetails(
        nakshatra = nakshatra,
        pada = pada,
        degreeInNakshatra = degreeInNakshatra,
        deity = getNakshatraDeity(nakshatra),
        symbol = getNakshatraSymbol(nakshatra),
        nature = getNakshatraNature(nakshatra),
        gana = getNakshatraGana(nakshatra),
        animal = getNakshatraAnimal(nakshatra),
        element = getNakshatraElement(nakshatra),
        caste = getNakshatraCaste(nakshatra),
        direction = getNakshatraDirection(nakshatra),
        bodyPart = getNakshatraBodyPart(nakshatra),
        gender = getNakshatraGender(nakshatra),
        dosha = getNakshatraDosha(nakshatra),
        qualities = getNakshatraQualities(nakshatra),
        strengths = getNakshatraStrengths(nakshatra),
        weaknesses = getNakshatraWeaknesses(nakshatra),
        careerAptitudes = getNakshatraCareerAptitudes(nakshatra),
        mantra = getNakshatraMantra(nakshatra)
    )
}

private fun getNakshatraDeity(nakshatra: Nakshatra): String = when (nakshatra) {
    Nakshatra.ASHWINI -> "Ashwini Kumaras (Divine Physicians)"
    Nakshatra.BHARANI -> "Yama (God of Death)"
    Nakshatra.KRITTIKA -> "Agni (Fire God)"
    Nakshatra.ROHINI -> "Brahma (Creator)"
    Nakshatra.MRIGASHIRA -> "Soma (Moon God)"
    Nakshatra.ARDRA -> "Rudra (Storm God)"
    Nakshatra.PUNARVASU -> "Aditi (Mother of Gods)"
    Nakshatra.PUSHYA -> "Brihaspati (Jupiter)"
    Nakshatra.ASHLESHA -> "Naga (Serpent)"
    Nakshatra.MAGHA -> "Pitris (Ancestors)"
    Nakshatra.PURVA_PHALGUNI -> "Bhaga (God of Fortune)"
    Nakshatra.UTTARA_PHALGUNI -> "Aryaman (God of Contracts)"
    Nakshatra.HASTA -> "Savitar (Sun God)"
    Nakshatra.CHITRA -> "Tvashtar (Divine Architect)"
    Nakshatra.SWATI -> "Vayu (Wind God)"
    Nakshatra.VISHAKHA -> "Indra-Agni"
    Nakshatra.ANURADHA -> "Mitra (God of Friendship)"
    Nakshatra.JYESHTHA -> "Indra (King of Gods)"
    Nakshatra.MULA -> "Nirriti (Goddess of Destruction)"
    Nakshatra.PURVA_ASHADHA -> "Apas (Water Deity)"
    Nakshatra.UTTARA_ASHADHA -> "Vishwadevas (Universal Gods)"
    Nakshatra.SHRAVANA -> "Vishnu (Preserver)"
    Nakshatra.DHANISHTHA -> "Vasus (Eight Gods)"
    Nakshatra.SHATABHISHA -> "Varuna (God of Cosmic Waters)"
    Nakshatra.PURVA_BHADRAPADA -> "Aja Ekapada (One-footed Goat)"
    Nakshatra.UTTARA_BHADRAPADA -> "Ahir Budhnya (Serpent of the Deep)"
    Nakshatra.REVATI -> "Pushan (God of Nourishment)"
}

private fun getNakshatraSymbol(nakshatra: Nakshatra): String = when (nakshatra) {
    Nakshatra.ASHWINI -> "Horse's Head"
    Nakshatra.BHARANI -> "Yoni (Female Organ)"
    Nakshatra.KRITTIKA -> "Razor/Flame"
    Nakshatra.ROHINI -> "Cart/Chariot"
    Nakshatra.MRIGASHIRA -> "Deer's Head"
    Nakshatra.ARDRA -> "Teardrop/Diamond"
    Nakshatra.PUNARVASU -> "Bow/House"
    Nakshatra.PUSHYA -> "Cow's Udder/Flower"
    Nakshatra.ASHLESHA -> "Coiled Serpent"
    Nakshatra.MAGHA -> "Royal Throne"
    Nakshatra.PURVA_PHALGUNI -> "Swinging Hammock"
    Nakshatra.UTTARA_PHALGUNI -> "Bed/Legs of Cot"
    Nakshatra.HASTA -> "Hand/Palm"
    Nakshatra.CHITRA -> "Bright Pearl/Gem"
    Nakshatra.SWATI -> "Coral/Sword"
    Nakshatra.VISHAKHA -> "Triumphal Arch"
    Nakshatra.ANURADHA -> "Lotus"
    Nakshatra.JYESHTHA -> "Earring/Talisman"
    Nakshatra.MULA -> "Tied Roots"
    Nakshatra.PURVA_ASHADHA -> "Elephant Tusk/Fan"
    Nakshatra.UTTARA_ASHADHA -> "Elephant Tusk/Bed"
    Nakshatra.SHRAVANA -> "Ear/Arrow"
    Nakshatra.DHANISHTHA -> "Drum/Flute"
    Nakshatra.SHATABHISHA -> "Empty Circle"
    Nakshatra.PURVA_BHADRAPADA -> "Sword/Two Front Legs"
    Nakshatra.UTTARA_BHADRAPADA -> "Twin/Back Legs"
    Nakshatra.REVATI -> "Fish/Drum"
}

private fun getNakshatraNature(nakshatra: Nakshatra): NakshatraNature = when (nakshatra) {
    Nakshatra.ROHINI, Nakshatra.UTTARA_PHALGUNI, Nakshatra.UTTARA_ASHADHA, Nakshatra.UTTARA_BHADRAPADA -> NakshatraNature.FIXED
    Nakshatra.PUNARVASU, Nakshatra.SWATI, Nakshatra.SHRAVANA, Nakshatra.DHANISHTHA, Nakshatra.SHATABHISHA -> NakshatraNature.MOVEABLE
    Nakshatra.ARDRA, Nakshatra.ASHLESHA, Nakshatra.JYESHTHA, Nakshatra.MULA -> NakshatraNature.SHARP
    Nakshatra.MRIGASHIRA, Nakshatra.CHITRA, Nakshatra.ANURADHA, Nakshatra.REVATI -> NakshatraNature.SOFT
    Nakshatra.KRITTIKA, Nakshatra.VISHAKHA -> NakshatraNature.MIXED
    Nakshatra.ASHWINI, Nakshatra.PUSHYA, Nakshatra.HASTA -> NakshatraNature.LIGHT
    Nakshatra.BHARANI, Nakshatra.MAGHA, Nakshatra.PURVA_PHALGUNI, Nakshatra.PURVA_ASHADHA, Nakshatra.PURVA_BHADRAPADA -> NakshatraNature.FIERCE
}

private fun getNakshatraGana(nakshatra: Nakshatra): NakshatraGana = when (nakshatra) {
    Nakshatra.ASHWINI, Nakshatra.MRIGASHIRA, Nakshatra.PUNARVASU, Nakshatra.PUSHYA,
    Nakshatra.HASTA, Nakshatra.SWATI, Nakshatra.ANURADHA, Nakshatra.SHRAVANA, Nakshatra.REVATI -> NakshatraGana.DEVA
    Nakshatra.BHARANI, Nakshatra.ROHINI, Nakshatra.ARDRA, Nakshatra.PURVA_PHALGUNI,
    Nakshatra.UTTARA_PHALGUNI, Nakshatra.PURVA_ASHADHA, Nakshatra.UTTARA_ASHADHA,
    Nakshatra.PURVA_BHADRAPADA, Nakshatra.UTTARA_BHADRAPADA -> NakshatraGana.MANUSHYA
    Nakshatra.KRITTIKA, Nakshatra.ASHLESHA, Nakshatra.MAGHA, Nakshatra.CHITRA,
    Nakshatra.VISHAKHA, Nakshatra.JYESHTHA, Nakshatra.MULA, Nakshatra.DHANISHTHA, Nakshatra.SHATABHISHA -> NakshatraGana.RAKSHASA
}

private fun getNakshatraAnimal(nakshatra: Nakshatra): String = when (nakshatra) {
    Nakshatra.ASHWINI, Nakshatra.SHATABHISHA -> "Horse"
    Nakshatra.BHARANI, Nakshatra.REVATI -> "Elephant"
    Nakshatra.KRITTIKA, Nakshatra.PUSHYA -> "Sheep/Goat"
    Nakshatra.ROHINI, Nakshatra.MRIGASHIRA -> "Serpent"
    Nakshatra.ARDRA, Nakshatra.MULA -> "Dog"
    Nakshatra.PUNARVASU, Nakshatra.ASHLESHA -> "Cat"
    Nakshatra.MAGHA, Nakshatra.PURVA_PHALGUNI -> "Rat"
    Nakshatra.UTTARA_PHALGUNI, Nakshatra.UTTARA_BHADRAPADA -> "Cow"
    Nakshatra.HASTA, Nakshatra.SWATI -> "Buffalo"
    Nakshatra.CHITRA, Nakshatra.VISHAKHA -> "Tiger"
    Nakshatra.ANURADHA, Nakshatra.JYESHTHA -> "Deer"
    Nakshatra.PURVA_ASHADHA, Nakshatra.SHRAVANA -> "Monkey"
    Nakshatra.UTTARA_ASHADHA, Nakshatra.PURVA_BHADRAPADA -> "Mongoose"
    Nakshatra.DHANISHTHA -> "Lion"
}

private fun getNakshatraElement(nakshatra: Nakshatra): NakshatraElement = when (nakshatra) {
    Nakshatra.KRITTIKA, Nakshatra.ASHWINI, Nakshatra.BHARANI -> NakshatraElement.FIRE
    Nakshatra.ROHINI, Nakshatra.MRIGASHIRA, Nakshatra.HASTA, Nakshatra.UTTARA_PHALGUNI, Nakshatra.UTTARA_ASHADHA, Nakshatra.UTTARA_BHADRAPADA -> NakshatraElement.EARTH
    Nakshatra.ARDRA, Nakshatra.SWATI, Nakshatra.PUNARVASU, Nakshatra.DHANISHTHA, Nakshatra.SHATABHISHA -> NakshatraElement.AIR
    Nakshatra.PUSHYA, Nakshatra.ASHLESHA, Nakshatra.ANURADHA, Nakshatra.JYESHTHA, Nakshatra.PURVA_ASHADHA, Nakshatra.REVATI -> NakshatraElement.WATER
    else -> NakshatraElement.ETHER
}

private fun getNakshatraCaste(nakshatra: Nakshatra): NakshatraCaste = when (nakshatra) {
    Nakshatra.KRITTIKA, Nakshatra.PUSHYA, Nakshatra.UTTARA_PHALGUNI, Nakshatra.VISHAKHA, Nakshatra.UTTARA_ASHADHA, Nakshatra.SHRAVANA, Nakshatra.UTTARA_BHADRAPADA -> NakshatraCaste.BRAHMIN
    Nakshatra.ASHWINI, Nakshatra.PURVA_PHALGUNI, Nakshatra.MULA, Nakshatra.PURVA_ASHADHA, Nakshatra.DHANISHTHA -> NakshatraCaste.KSHATRIYA
    Nakshatra.BHARANI, Nakshatra.ROHINI, Nakshatra.CHITRA, Nakshatra.SWATI, Nakshatra.PURVA_BHADRAPADA -> NakshatraCaste.VAISHYA
    else -> NakshatraCaste.SHUDRA
}

private fun getNakshatraDirection(nakshatra: Nakshatra): String = when (nakshatra.number % 4) {
    1 -> "East"
    2 -> "South"
    3 -> "West"
    0 -> "North"
    else -> "Center"
}

private fun getNakshatraBodyPart(nakshatra: Nakshatra): String = when (nakshatra) {
    Nakshatra.ASHWINI, Nakshatra.BHARANI, Nakshatra.KRITTIKA -> "Head"
    Nakshatra.ROHINI, Nakshatra.MRIGASHIRA, Nakshatra.ARDRA -> "Face/Eyes"
    Nakshatra.PUNARVASU, Nakshatra.PUSHYA, Nakshatra.ASHLESHA -> "Neck/Shoulders"
    Nakshatra.MAGHA, Nakshatra.PURVA_PHALGUNI, Nakshatra.UTTARA_PHALGUNI -> "Heart/Back"
    Nakshatra.HASTA, Nakshatra.CHITRA, Nakshatra.SWATI -> "Hands/Stomach"
    Nakshatra.VISHAKHA, Nakshatra.ANURADHA, Nakshatra.JYESHTHA -> "Lower Abdomen"
    Nakshatra.MULA, Nakshatra.PURVA_ASHADHA, Nakshatra.UTTARA_ASHADHA -> "Thighs"
    Nakshatra.SHRAVANA, Nakshatra.DHANISHTHA, Nakshatra.SHATABHISHA -> "Knees/Legs"
    Nakshatra.PURVA_BHADRAPADA, Nakshatra.UTTARA_BHADRAPADA, Nakshatra.REVATI -> "Feet/Ankles"
}

private fun getNakshatraGender(nakshatra: Nakshatra): NakshatraGender = when (nakshatra.number % 2) {
    1 -> NakshatraGender.MALE
    else -> NakshatraGender.FEMALE
}

private fun getNakshatraDosha(nakshatra: Nakshatra): NakshatraDosha = when (nakshatra) {
    Nakshatra.ASHWINI, Nakshatra.BHARANI, Nakshatra.MRIGASHIRA, Nakshatra.ARDRA,
    Nakshatra.PUNARVASU, Nakshatra.SWATI, Nakshatra.SHRAVANA, Nakshatra.DHANISHTHA, Nakshatra.SHATABHISHA -> NakshatraDosha.VATA
    Nakshatra.KRITTIKA, Nakshatra.ROHINI, Nakshatra.MAGHA, Nakshatra.PURVA_PHALGUNI,
    Nakshatra.CHITRA, Nakshatra.MULA, Nakshatra.PURVA_ASHADHA, Nakshatra.PURVA_BHADRAPADA -> NakshatraDosha.PITTA
    else -> NakshatraDosha.KAPHA
}

private fun getNakshatraQualities(nakshatra: Nakshatra): List<String> = when (nakshatra) {
    Nakshatra.ASHWINI -> listOf("Quick-thinking", "Healing abilities", "Pioneering spirit", "Youthful energy")
    Nakshatra.BHARANI -> listOf("Transformative", "Creative", "Strong willpower", "Bearing responsibility")
    Nakshatra.KRITTIKA -> listOf("Fiery determination", "Sharp intellect", "Purifying nature", "Leadership")
    Nakshatra.ROHINI -> listOf("Creative", "Artistic", "Magnetic personality", "Material abundance")
    Nakshatra.MRIGASHIRA -> listOf("Curious mind", "Searching nature", "Gentle demeanor", "Adaptable")
    Nakshatra.ARDRA -> listOf("Transformative", "Intense emotions", "Destructive-creative", "Research-oriented")
    Nakshatra.PUNARVASU -> listOf("Renewal", "Prosperity", "Spiritual wisdom", "Return to roots")
    Nakshatra.PUSHYA -> listOf("Nourishing", "Protective", "Spiritual growth", "Teaching ability")
    Nakshatra.ASHLESHA -> listOf("Mystical wisdom", "Psychic abilities", "Secretive nature", "Hypnotic charm")
    Nakshatra.MAGHA -> listOf("Royal bearing", "Ancestral pride", "Leadership", "Traditionalist")
    Nakshatra.PURVA_PHALGUNI -> listOf("Creative", "Pleasure-seeking", "Affectionate", "Artistic talent")
    Nakshatra.UTTARA_PHALGUNI -> listOf("Service-oriented", "Helpful nature", "Good organizer", "Contract-keeper")
    Nakshatra.HASTA -> listOf("Skilled hands", "Clever", "Resourceful", "Healing touch")
    Nakshatra.CHITRA -> listOf("Brilliant", "Creative genius", "Magnetic", "Artistic mastery")
    Nakshatra.SWATI -> listOf("Independent", "Adaptable", "Business acumen", "Diplomatic")
    Nakshatra.VISHAKHA -> listOf("Goal-oriented", "Determined", "Ambitious", "Single-minded focus")
    Nakshatra.ANURADHA -> listOf("Devoted", "Friendly", "Organizational ability", "Travel-loving")
    Nakshatra.JYESHTHA -> listOf("Protective", "Elder brother energy", "Leadership", "Responsibility")
    Nakshatra.MULA -> listOf("Investigative", "Root-seeking", "Spiritual depth", "Transformation")
    Nakshatra.PURVA_ASHADHA -> listOf("Invincible spirit", "Philosophical", "Optimistic", "Purifying")
    Nakshatra.UTTARA_ASHADHA -> listOf("Final victory", "Leadership", "Integrity", "Universal values")
    Nakshatra.SHRAVANA -> listOf("Good listener", "Learning ability", "Connection to truth", "Preservation")
    Nakshatra.DHANISHTHA -> listOf("Musical talent", "Wealth-giving", "Marching forward", "Group activities")
    Nakshatra.SHATABHISHA -> listOf("Healing powers", "Secretive wisdom", "Research ability", "Self-sufficient")
    Nakshatra.PURVA_BHADRAPADA -> listOf("Fiery passion", "Spiritual warrior", "Transformation", "Asceticism")
    Nakshatra.UTTARA_BHADRAPADA -> listOf("Deep meditation", "Kundalini energy", "Wisdom", "Control over desires")
    Nakshatra.REVATI -> listOf("Nourishing", "Protective of others", "Journey completion", "Wealth-giving")
}

private fun getNakshatraStrengths(nakshatra: Nakshatra): List<String> = when (nakshatra) {
    Nakshatra.ASHWINI -> listOf("Quick healer", "Initiative", "Courage", "Rejuvenation ability")
    Nakshatra.BHARANI -> listOf("Strong will", "Creativity", "Self-discipline", "Endurance")
    Nakshatra.KRITTIKA -> listOf("Sharp mind", "Purifying influence", "Courage", "Fame potential")
    else -> listOf("Determination", "Intuition", "Adaptability", "Inner strength")
}

private fun getNakshatraWeaknesses(nakshatra: Nakshatra): List<String> = when (nakshatra) {
    Nakshatra.ASHWINI -> listOf("Impatience", "Recklessness", "Overconfidence", "Restlessness")
    Nakshatra.BHARANI -> listOf("Stubbornness", "Possessiveness", "Jealousy", "Extremism")
    Nakshatra.KRITTIKA -> listOf("Harsh speech", "Anger issues", "Critical nature", "Dominating")
    else -> listOf("Overthinking", "Anxiety", "Sensitivity", "Perfectionism")
}

private fun getNakshatraCareerAptitudes(nakshatra: Nakshatra): List<String> = when (nakshatra) {
    Nakshatra.ASHWINI -> listOf("Medicine", "Sports", "Emergency services", "Transport")
    Nakshatra.BHARANI -> listOf("Arts", "Entertainment", "Legal profession", "Publishing")
    Nakshatra.KRITTIKA -> listOf("Military", "Cooking", "Engineering", "Surgery")
    Nakshatra.ROHINI -> listOf("Agriculture", "Fashion", "Beauty industry", "Real estate")
    Nakshatra.PUSHYA -> listOf("Teaching", "Counseling", "Clergy", "Food industry")
    else -> listOf("Management", "Consulting", "Technology", "Research")
}

private fun getNakshatraMantra(nakshatra: Nakshatra): String = when (nakshatra) {
    Nakshatra.ASHWINI -> "ॐ अश्विनीकुमाराभ्यां नमः। Om Ashwini Kumarabhyam Namah"
    Nakshatra.BHARANI -> "ॐ यमाय नमः। Om Yamaya Namah"
    Nakshatra.KRITTIKA -> "ॐ अग्नये नमः। Om Agnaye Namah"
    Nakshatra.ROHINI -> "ॐ ब्रह्मणे नमः। Om Brahmane Namah"
    Nakshatra.MRIGASHIRA -> "ॐ सोमाय नमः। Om Somaya Namah"
    Nakshatra.ARDRA -> "ॐ रुद्राय नमः। Om Rudraya Namah"
    Nakshatra.PUNARVASU -> "ॐ अदित्यै नमः। Om Adityai Namah"
    Nakshatra.PUSHYA -> "ॐ बृहस्पतये नमः। Om Brihaspataye Namah"
    Nakshatra.ASHLESHA -> "ॐ नागाय नमः। Om Nagaya Namah"
    Nakshatra.MAGHA -> "ॐ पितृभ्यो नमः। Om Pitribhyo Namah"
    Nakshatra.PURVA_PHALGUNI -> "ॐ भगाय नमः। Om Bhagaya Namah"
    Nakshatra.UTTARA_PHALGUNI -> "ॐ अर्यम्णे नमः। Om Aryamne Namah"
    Nakshatra.HASTA -> "ॐ सवित्रे नमः। Om Savitre Namah"
    Nakshatra.CHITRA -> "ॐ त्वष्ट्रे नमः। Om Tvashtre Namah"
    Nakshatra.SWATI -> "ॐ वायवे नमः। Om Vayave Namah"
    Nakshatra.VISHAKHA -> "ॐ इन्द्राग्निभ्यां नमः। Om Indragnibhyam Namah"
    Nakshatra.ANURADHA -> "ॐ मित्राय नमः। Om Mitraya Namah"
    Nakshatra.JYESHTHA -> "ॐ इन्द्राय नमः। Om Indraya Namah"
    Nakshatra.MULA -> "ॐ निर्ऋत्यै नमः। Om Nirrutyai Namah"
    Nakshatra.PURVA_ASHADHA -> "ॐ अपां नमः। Om Apam Namah"
    Nakshatra.UTTARA_ASHADHA -> "ॐ विश्वेभ्यो देवेभ्यो नमः। Om Vishvebhyo Devebhyo Namah"
    Nakshatra.SHRAVANA -> "ॐ विष्णवे नमः। Om Vishnave Namah"
    Nakshatra.DHANISHTHA -> "ॐ वसुभ्यो नमः। Om Vasubhyo Namah"
    Nakshatra.SHATABHISHA -> "ॐ वरुणाय नमः। Om Varunaya Namah"
    Nakshatra.PURVA_BHADRAPADA -> "ॐ अजैकपदाय नमः। Om Ajaikapadaya Namah"
    Nakshatra.UTTARA_BHADRAPADA -> "ॐ अहिर्बुध्न्याय नमः। Om Ahirbudhnyaya Namah"
    Nakshatra.REVATI -> "ॐ पूष्णे नमः। Om Pushne Namah"
}

private fun calculateNakshatraCompatibility(nakshatra: Nakshatra): NakshatraCompatibility {
    // Vedha pairs - nakshatras that create obstruction when used together
    val vedhaPairs = getVedhaPairs(nakshatra)

    // Rajju classification
    val rajjuType = getRajjuType(nakshatra)

    // Calculate Tarabala for all 27 nakshatras
    val tarabalaResults = Nakshatra.entries.map { targetNakshatra ->
        calculateTarabala(nakshatra, targetNakshatra)
    }

    val favorable = tarabalaResults.filter { it.isFavorable }.map {
        Nakshatra.entries.find { n -> n.displayName == it.nakshatraName } ?: Nakshatra.ASHWINI
    }

    val unfavorable = tarabalaResults.filter { !it.isFavorable }.map {
        Nakshatra.entries.find { n -> n.displayName == it.nakshatraName } ?: Nakshatra.ASHWINI
    }

    return NakshatraCompatibility(
        vedhaPartners = vedhaPairs,
        rajjuType = rajjuType,
        favorableNakshatras = favorable,
        unfavorableNakshatras = unfavorable,
        tarabalaResults = tarabalaResults
    )
}

private fun getVedhaPairs(nakshatra: Nakshatra): List<Nakshatra> = when (nakshatra) {
    Nakshatra.ASHWINI -> listOf(Nakshatra.JYESHTHA)
    Nakshatra.BHARANI -> listOf(Nakshatra.ANURADHA)
    Nakshatra.KRITTIKA -> listOf(Nakshatra.VISHAKHA)
    Nakshatra.ROHINI -> listOf(Nakshatra.SWATI)
    Nakshatra.ARDRA -> listOf(Nakshatra.SHRAVANA)
    Nakshatra.PUNARVASU -> listOf(Nakshatra.UTTARA_ASHADHA)
    Nakshatra.PUSHYA -> listOf(Nakshatra.PURVA_ASHADHA)
    Nakshatra.ASHLESHA -> listOf(Nakshatra.MULA)
    Nakshatra.MAGHA -> listOf(Nakshatra.REVATI)
    Nakshatra.PURVA_PHALGUNI -> listOf(Nakshatra.UTTARA_BHADRAPADA)
    Nakshatra.UTTARA_PHALGUNI -> listOf(Nakshatra.PURVA_BHADRAPADA)
    Nakshatra.HASTA -> listOf(Nakshatra.SHATABHISHA)
    Nakshatra.CHITRA -> listOf(Nakshatra.DHANISHTHA)
    Nakshatra.SWATI -> listOf(Nakshatra.ROHINI)
    Nakshatra.VISHAKHA -> listOf(Nakshatra.KRITTIKA)
    Nakshatra.ANURADHA -> listOf(Nakshatra.BHARANI)
    Nakshatra.JYESHTHA -> listOf(Nakshatra.ASHWINI)
    Nakshatra.MULA -> listOf(Nakshatra.ASHLESHA)
    Nakshatra.PURVA_ASHADHA -> listOf(Nakshatra.PUSHYA)
    Nakshatra.UTTARA_ASHADHA -> listOf(Nakshatra.PUNARVASU)
    Nakshatra.SHRAVANA -> listOf(Nakshatra.ARDRA)
    Nakshatra.DHANISHTHA -> listOf(Nakshatra.CHITRA)
    Nakshatra.SHATABHISHA -> listOf(Nakshatra.HASTA)
    Nakshatra.PURVA_BHADRAPADA -> listOf(Nakshatra.UTTARA_PHALGUNI)
    Nakshatra.UTTARA_BHADRAPADA -> listOf(Nakshatra.PURVA_PHALGUNI)
    Nakshatra.REVATI -> listOf(Nakshatra.MAGHA)
    Nakshatra.MRIGASHIRA -> emptyList()
}

private fun getRajjuType(nakshatra: Nakshatra): RajjuType = when (nakshatra) {
    Nakshatra.ASHWINI, Nakshatra.ASHLESHA, Nakshatra.MAGHA, Nakshatra.JYESHTHA, Nakshatra.MULA, Nakshatra.REVATI -> RajjuType.PAADA
    Nakshatra.BHARANI, Nakshatra.PUSHYA, Nakshatra.PURVA_PHALGUNI, Nakshatra.ANURADHA, Nakshatra.PURVA_ASHADHA, Nakshatra.UTTARA_BHADRAPADA -> RajjuType.KATI
    Nakshatra.KRITTIKA, Nakshatra.PUNARVASU, Nakshatra.UTTARA_PHALGUNI, Nakshatra.VISHAKHA, Nakshatra.UTTARA_ASHADHA, Nakshatra.PURVA_BHADRAPADA -> RajjuType.NABHI
    Nakshatra.ROHINI, Nakshatra.ARDRA, Nakshatra.HASTA, Nakshatra.SWATI, Nakshatra.SHRAVANA, Nakshatra.SHATABHISHA -> RajjuType.KANTHA
    Nakshatra.MRIGASHIRA, Nakshatra.CHITRA, Nakshatra.DHANISHTHA -> RajjuType.SHIRO
}

private fun calculateTarabala(birthNakshatra: Nakshatra, targetNakshatra: Nakshatra): TarabalaResult {
    val birthNum = birthNakshatra.number
    val targetNum = targetNakshatra.number

    // Calculate tara number (1-9 cycle)
    var diff = targetNum - birthNum
    if (diff < 0) diff += 27
    val taraNumber = (diff % 9) + 1

    val (taraName, isFavorable, description) = when (taraNumber) {
        1 -> Triple("Janma", false, "Birth star - challenging for new beginnings")
        2 -> Triple("Sampat", true, "Wealth - favorable for financial matters")
        3 -> Triple("Vipat", false, "Danger - avoid important activities")
        4 -> Triple("Kshema", true, "Well-being - favorable for health matters")
        5 -> Triple("Pratyari", false, "Obstacle - creates hindrances")
        6 -> Triple("Sadhaka", true, "Achievement - good for goals")
        7 -> Triple("Vadha", false, "Death - highly inauspicious")
        8 -> Triple("Mitra", true, "Friend - favorable for relationships")
        9 -> Triple("Parama Mitra", true, "Great Friend - highly auspicious")
        else -> Triple("Unknown", false, "")
    }

    return TarabalaResult(
        nakshatraName = targetNakshatra.displayName,
        taraNumber = taraNumber,
        taraName = taraName,
        isFavorable = isFavorable,
        description = description
    )
}

private fun calculateNakshatraRemedy(details: NakshatraDetails, language: Language): NakshatraRemedy {
    val nakshatra = details.nakshatra

    val gemstone = when (nakshatra.ruler) {
        Planet.SUN -> "Ruby"
        Planet.MOON -> "Pearl"
        Planet.MARS -> "Red Coral"
        Planet.MERCURY -> "Emerald"
        Planet.JUPITER -> "Yellow Sapphire"
        Planet.VENUS -> "Diamond"
        Planet.SATURN -> "Blue Sapphire"
        Planet.RAHU -> "Hessonite"
        Planet.KETU -> "Cat's Eye"
        else -> null
    }

    val favorableDays = when (nakshatra.ruler) {
        Planet.SUN -> listOf("Sunday")
        Planet.MOON -> listOf("Monday")
        Planet.MARS -> listOf("Tuesday")
        Planet.MERCURY -> listOf("Wednesday")
        Planet.JUPITER -> listOf("Thursday")
        Planet.VENUS -> listOf("Friday")
        Planet.SATURN -> listOf("Saturday")
        Planet.RAHU -> listOf("Saturday")
        Planet.KETU -> listOf("Tuesday")
        else -> listOf("Any day")
    }

    return NakshatraRemedy(
        title = "Remedies for ${nakshatra.displayName}",
        description = "Personalized remedies based on your birth nakshatra to enhance positive qualities and mitigate challenges.",
        mantra = details.mantra,
        timing = "Best performed during ${nakshatra.ruler.displayName} hora or on ${favorableDays.first()}",
        gemstone = gemstone,
        deity = details.deity,
        favorableDays = favorableDays,
        luckyNumbers = getLuckyNumbers(nakshatra),
        luckyColors = getLuckyColors(nakshatra)
    )
}

private fun getLuckyNumbers(nakshatra: Nakshatra): List<Int> = when (nakshatra.ruler) {
    Planet.SUN -> listOf(1, 4, 10)
    Planet.MOON -> listOf(2, 7, 11)
    Planet.MARS -> listOf(3, 9, 18)
    Planet.MERCURY -> listOf(5, 14, 23)
    Planet.JUPITER -> listOf(3, 12, 21)
    Planet.VENUS -> listOf(6, 15, 24)
    Planet.SATURN -> listOf(8, 17, 26)
    Planet.RAHU -> listOf(4, 13, 22)
    Planet.KETU -> listOf(7, 16, 25)
    else -> listOf(1, 5, 9)
}

private fun getLuckyColors(nakshatra: Nakshatra): List<String> = when (nakshatra.ruler) {
    Planet.SUN -> listOf("Orange", "Gold", "Red")
    Planet.MOON -> listOf("White", "Silver", "Pearl")
    Planet.MARS -> listOf("Red", "Coral", "Scarlet")
    Planet.MERCURY -> listOf("Green", "Emerald", "Turquoise")
    Planet.JUPITER -> listOf("Yellow", "Gold", "Orange")
    Planet.VENUS -> listOf("White", "Pink", "Pastel")
    Planet.SATURN -> listOf("Blue", "Black", "Dark colors")
    Planet.RAHU -> listOf("Smoky", "Grey", "Mixed colors")
    Planet.KETU -> listOf("Grey", "Brown", "Multi-colored")
    else -> listOf("White", "Yellow", "Blue")
}

// ============================================
// UI Components
// ============================================

@Composable
private fun NakshatraTabSelector(
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
private fun NakshatraOverviewTab(
    analysis: NakshatraAnalysis,
    chart: VedicChart,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Birth Nakshatra Card
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
                    stringResource(StringKeyDosha.NAKSHATRA_BIRTH_STAR),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppTheme.TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    analysis.birthNakshatra.nakshatra.getLocalizedName(language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.TextPrimary
                )

                Text(
                    "${stringResource(StringKeyAnalysis.NAKSHATRA_PADA)} ${analysis.birthNakshatra.pada}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.AccentPrimary
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.5f))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NakshatraInfoChip(
                        label = stringResource(StringKeyDosha.NAKSHATRA_RULER),
                        value = analysis.birthNakshatra.nakshatra.ruler.getLocalizedName(language),
                        icon = Icons.Filled.Star
                    )
                    NakshatraInfoChip(
                        label = stringResource(StringKeyAnalysis.NAKSHATRA_GANA),
                        value = analysis.birthNakshatra.gana.getLocalizedName(language),
                        icon = Icons.Filled.Groups
                    )
                    NakshatraInfoChip(
                        label = stringResource(StringKeyDosha.NAKSHATRA_ELEMENT),
                        value = analysis.birthNakshatra.element.getLocalizedName(language),
                        icon = Icons.Filled.Public
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Deity and Symbol Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                NakshatraDetailRow(
                    label = stringResource(StringKeyAnalysis.NAKSHATRA_DEITY),
                    value = analysis.birthNakshatra.deity
                )
                Spacer(modifier = Modifier.height(8.dp))
                NakshatraDetailRow(
                    label = stringResource(StringKeyAnalysis.NAKSHATRA_SYMBOL),
                    value = analysis.birthNakshatra.symbol
                )
                Spacer(modifier = Modifier.height(8.dp))
                NakshatraDetailRow(
                    label = stringResource(StringKeyAnalysis.NAKSHATRA_NATURE),
                    value = analysis.birthNakshatra.nature.getLocalizedName(language)
                )
                Spacer(modifier = Modifier.height(8.dp))
                NakshatraDetailRow(
                    label = stringResource(StringKeyAnalysis.NAKSHATRA_ANIMAL),
                    value = analysis.birthNakshatra.animal
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Characteristics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.NAKSHATRA_CHARACTERISTICS),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                analysis.birthNakshatra.qualities.forEach { quality ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.SuccessColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            quality,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NakshatraInfoChip(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppTheme.AccentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AppTheme.AccentPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.TextSubtle
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.TextPrimary
        )
    }
}

@Composable
private fun NakshatraDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.TextMuted
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppTheme.TextPrimary
        )
    }
}

@Composable
private fun NakshatraDetailsTab(
    analysis: NakshatraAnalysis,
    expandedPlanet: Planet?,
    onExpandPlanet: (Planet) -> Unit,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(StringKeyDosha.NAKSHATRA_ALL_PLANETS),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        analysis.planetaryNakshatras.forEach { planetary ->
            PlanetaryNakshatraCard(
                planetary = planetary,
                isExpanded = expandedPlanet == planetary.planet,
                onToggle = { onExpandPlanet(planetary.planet) },
                language = language
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlanetaryNakshatraCard(
    planetary: PlanetaryNakshatra,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    language: Language
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppTheme.getPlanetColor(planetary.planet).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            planetary.planet.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.getPlanetColor(planetary.planet)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            planetary.planet.getLocalizedName(language),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary
                        )
                        Text(
                            "${planetary.details.nakshatra.getLocalizedName(language)} (Pada ${planetary.details.pada})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                    }
                }

                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = AppTheme.TextMuted,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = AppTheme.BorderColor.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    NakshatraDetailRow(
                        label = stringResource(StringKeyAnalysis.NAKSHATRA_DEITY),
                        value = planetary.details.deity
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    NakshatraDetailRow(
                        label = stringResource(StringKeyDosha.NAKSHATRA_DASHA_LORD),
                        value = planetary.details.nakshatra.ruler.getLocalizedName(language)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    NakshatraDetailRow(
                        label = stringResource(StringKeyDosha.NAKSHATRA_DEGREE_IN),
                        value = String.format("%.2f°", planetary.details.degreeInNakshatra)
                    )
                }
            }
        }
    }
}

@Composable
private fun NakshatraCompatibilityTab(
    analysis: NakshatraAnalysis,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Tarabala section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.NAKSHATRA_TARABALA),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Show favorable and unfavorable count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TarabalaSummaryChip(
                        label = stringResource(StringKeyDosha.NAKSHATRA_COMPATIBLE_WITH),
                        count = analysis.compatibility.favorableNakshatras.size,
                        color = AppTheme.SuccessColor
                    )
                    TarabalaSummaryChip(
                        label = stringResource(StringKeyDosha.NAKSHATRA_INCOMPATIBLE_WITH),
                        count = analysis.compatibility.unfavorableNakshatras.size,
                        color = AppTheme.WarningColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rajju
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(StringKeyDosha.NAKSHATRA_RAJJU_TYPE),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppTheme.AccentTeal.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = AppTheme.AccentTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            analysis.compatibility.rajjuType.getLocalizedName(language),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary
                        )
                        Text(
                            "Body part: ${analysis.compatibility.rajjuType.bodyPart}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vedha Pairs
        if (analysis.compatibility.vedhaPartners.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.WarningColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = AppTheme.WarningColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(StringKeyDosha.NAKSHATRA_VEDHA_PAIRS),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Avoid pairing with: ${analysis.compatibility.vedhaPartners.joinToString { it.getLocalizedName(language) }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TarabalaSummaryChip(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun NakshatraRemediesTab(
    analysis: NakshatraAnalysis,
    onCopyMantra: (String) -> Unit,
    language: Language
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Mantra Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.AccentGold.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(StringKeyDosha.NAKSHATRA_MANTRA),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary
                    )
                    IconButton(
                        onClick = { onCopyMantra(analysis.remedy.mantra) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = stringResource(StringKeyMatch.REMEDIES_COPY_MANTRA),
                            tint = AppTheme.AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    analysis.remedy.mantra,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.AccentGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    analysis.remedy.timing,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gemstone
        if (analysis.remedy.gemstone != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AppTheme.AccentPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Diamond,
                            contentDescription = null,
                            tint = AppTheme.AccentPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            stringResource(StringKeyDosha.NAKSHATRA_LUCKY_STONES),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.TextMuted
                        )
                        Text(
                            analysis.remedy.gemstone,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Lucky Numbers and Colors
        Row(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(StringKeyDosha.NAKSHATRA_LUCKY_NUMBERS),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        analysis.remedy.luckyNumbers.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(StringKeyDosha.NAKSHATRA_LUCKY_COLORS),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        analysis.remedy.luckyColors.take(2).joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Favorable Days
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    stringResource(StringKeyDosha.NAKSHATRA_FAVORABLE_DAYS),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    analysis.remedy.favorableDays.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun NakshatraInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(StringKeyDosha.NAKSHATRA_INFO_TITLE),
                fontWeight = FontWeight.Bold,
                color = AppTheme.TextPrimary
            )
        },
        text = {
            Text(
                stringResource(StringKeyDosha.NAKSHATRA_INFO_DESC),
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(StringKey.BTN_CLOSE), color = AppTheme.AccentGold)
            }
        },
        containerColor = AppTheme.CardBackground
    )
}
