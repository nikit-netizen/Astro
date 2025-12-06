package com.astro.storm.ephemeris

import android.content.Context
import com.astro.storm.data.model.*
import swisseph.SweConst
import swisseph.SwissEph
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToLong

/**
 * Comprehensive Varshaphala (Annual Horoscope) Calculator
 *
 * This calculator implements the Tajika system of annual horoscopy as practiced in Vedic astrology.
 * It calculates:
 * - Solar Return Chart with precise Swiss Ephemeris calculations
 * - Muntha (progressed ascendant)
 * - Year Lord (Varsheshwara) determination using traditional rules
 * - Pancha Vargiya Bala (five-fold planetary strength)
 * - Tri-Pataki Chakra (three-flag diagram)
 * - Sahams (Arabic Parts/Lots)
 * - Tajika Aspects and Yogas
 * - Mudda Dasha (annual planetary periods)
 * - House-wise predictions
 */
class VarshaphalaCalculator(context: Context) {

    private val swissEph = SwissEph()
    private val ephemerisPath: String

    companion object {
        private const val AYANAMSA_LAHIRI = SweConst.SE_SIDM_LAHIRI
        private const val SEFLG_SIDEREAL = SweConst.SEFLG_SIDEREAL
        private const val SEFLG_SPEED = SweConst.SEFLG_SPEED
        private const val SIDEREAL_YEAR_DAYS = 365.256363

        // Tajika aspect orbs
        private const val CONJUNCTION_ORB = 12.0
        private const val OPPOSITION_ORB = 9.0
        private const val TRINE_ORB = 8.0
        private const val SQUARE_ORB = 7.0
        private const val SEXTILE_ORB = 6.0

        // Vimshottari Dasha periods (in years)
        private val VIMSHOTTARI_YEARS = mapOf(
            Planet.KETU to 7,
            Planet.VENUS to 20,
            Planet.SUN to 6,
            Planet.MOON to 10,
            Planet.MARS to 7,
            Planet.RAHU to 18,
            Planet.JUPITER to 16,
            Planet.SATURN to 19,
            Planet.MERCURY to 17
        )

        private val VIMSHOTTARI_ORDER = listOf(
            Planet.KETU, Planet.VENUS, Planet.SUN, Planet.MOON, Planet.MARS,
            Planet.RAHU, Planet.JUPITER, Planet.SATURN, Planet.MERCURY
        )

        private val DAY_LORDS = listOf(
            Planet.SUN,     // Sunday
            Planet.MOON,    // Monday
            Planet.MARS,    // Tuesday
            Planet.MERCURY, // Wednesday
            Planet.JUPITER, // Thursday
            Planet.VENUS,   // Friday
            Planet.SATURN   // Saturday
        )

        // Hadda (Term) boundaries for each sign
        private val HADDA_LORDS = mapOf(
            ZodiacSign.ARIES to listOf(
                Triple(0.0, 6.0, Planet.JUPITER),
                Triple(6.0, 12.0, Planet.VENUS),
                Triple(12.0, 20.0, Planet.MERCURY),
                Triple(20.0, 25.0, Planet.MARS),
                Triple(25.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.TAURUS to listOf(
                Triple(0.0, 8.0, Planet.VENUS),
                Triple(8.0, 14.0, Planet.MERCURY),
                Triple(14.0, 22.0, Planet.JUPITER),
                Triple(22.0, 27.0, Planet.SATURN),
                Triple(27.0, 30.0, Planet.MARS)
            ),
            ZodiacSign.GEMINI to listOf(
                Triple(0.0, 6.0, Planet.MERCURY),
                Triple(6.0, 12.0, Planet.JUPITER),
                Triple(12.0, 17.0, Planet.VENUS),
                Triple(17.0, 24.0, Planet.MARS),
                Triple(24.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.CANCER to listOf(
                Triple(0.0, 7.0, Planet.MARS),
                Triple(7.0, 13.0, Planet.VENUS),
                Triple(13.0, 19.0, Planet.MERCURY),
                Triple(19.0, 26.0, Planet.JUPITER),
                Triple(26.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.LEO to listOf(
                Triple(0.0, 6.0, Planet.JUPITER),
                Triple(6.0, 11.0, Planet.VENUS),
                Triple(11.0, 18.0, Planet.SATURN),
                Triple(18.0, 24.0, Planet.MERCURY),
                Triple(24.0, 30.0, Planet.MARS)
            ),
            ZodiacSign.VIRGO to listOf(
                Triple(0.0, 7.0, Planet.MERCURY),
                Triple(7.0, 17.0, Planet.VENUS),
                Triple(17.0, 21.0, Planet.JUPITER),
                Triple(21.0, 28.0, Planet.MARS),
                Triple(28.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.LIBRA to listOf(
                Triple(0.0, 6.0, Planet.SATURN),
                Triple(6.0, 14.0, Planet.MERCURY),
                Triple(14.0, 21.0, Planet.JUPITER),
                Triple(21.0, 28.0, Planet.VENUS),
                Triple(28.0, 30.0, Planet.MARS)
            ),
            ZodiacSign.SCORPIO to listOf(
                Triple(0.0, 7.0, Planet.MARS),
                Triple(7.0, 11.0, Planet.VENUS),
                Triple(11.0, 19.0, Planet.MERCURY),
                Triple(19.0, 24.0, Planet.JUPITER),
                Triple(24.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.SAGITTARIUS to listOf(
                Triple(0.0, 12.0, Planet.JUPITER),
                Triple(12.0, 17.0, Planet.VENUS),
                Triple(17.0, 21.0, Planet.MERCURY),
                Triple(21.0, 26.0, Planet.SATURN),
                Triple(26.0, 30.0, Planet.MARS)
            ),
            ZodiacSign.CAPRICORN to listOf(
                Triple(0.0, 7.0, Planet.MERCURY),
                Triple(7.0, 14.0, Planet.JUPITER),
                Triple(14.0, 22.0, Planet.VENUS),
                Triple(22.0, 26.0, Planet.SATURN),
                Triple(26.0, 30.0, Planet.MARS)
            ),
            ZodiacSign.AQUARIUS to listOf(
                Triple(0.0, 7.0, Planet.MERCURY),
                Triple(7.0, 13.0, Planet.VENUS),
                Triple(13.0, 20.0, Planet.JUPITER),
                Triple(20.0, 25.0, Planet.MARS),
                Triple(25.0, 30.0, Planet.SATURN)
            ),
            ZodiacSign.PISCES to listOf(
                Triple(0.0, 12.0, Planet.VENUS),
                Triple(12.0, 16.0, Planet.JUPITER),
                Triple(16.0, 19.0, Planet.MERCURY),
                Triple(19.0, 28.0, Planet.MARS),
                Triple(28.0, 30.0, Planet.SATURN)
            )
        )

        // Exaltation degrees for planets
        private val EXALTATION_DEGREES = mapOf(
            Planet.SUN to 10.0,      // Aries 10°
            Planet.MOON to 33.0,     // Taurus 3°
            Planet.MARS to 298.0,    // Capricorn 28°
            Planet.MERCURY to 165.0, // Virgo 15°
            Planet.JUPITER to 95.0,  // Cancer 5°
            Planet.VENUS to 357.0,   // Pisces 27°
            Planet.SATURN to 200.0   // Libra 20°
        )

        // Debilitation signs
        private val DEBILITATION_SIGNS = mapOf(
            Planet.SUN to ZodiacSign.LIBRA,
            Planet.MOON to ZodiacSign.SCORPIO,
            Planet.MARS to ZodiacSign.CANCER,
            Planet.MERCURY to ZodiacSign.PISCES,
            Planet.JUPITER to ZodiacSign.CAPRICORN,
            Planet.VENUS to ZodiacSign.VIRGO,
            Planet.SATURN to ZodiacSign.ARIES
        )

        // Own signs for planets
        private val OWN_SIGNS = mapOf(
            Planet.SUN to listOf(ZodiacSign.LEO),
            Planet.MOON to listOf(ZodiacSign.CANCER),
            Planet.MARS to listOf(ZodiacSign.ARIES, ZodiacSign.SCORPIO),
            Planet.MERCURY to listOf(ZodiacSign.GEMINI, ZodiacSign.VIRGO),
            Planet.JUPITER to listOf(ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES),
            Planet.VENUS to listOf(ZodiacSign.TAURUS, ZodiacSign.LIBRA),
            Planet.SATURN to listOf(ZodiacSign.CAPRICORN, ZodiacSign.AQUARIUS)
        )

        // Planetary friendships
        private val FRIENDSHIPS = mapOf(
            Planet.SUN to listOf(Planet.MOON, Planet.MARS, Planet.JUPITER),
            Planet.MOON to listOf(Planet.SUN, Planet.MERCURY),
            Planet.MARS to listOf(Planet.SUN, Planet.MOON, Planet.JUPITER),
            Planet.MERCURY to listOf(Planet.SUN, Planet.VENUS),
            Planet.JUPITER to listOf(Planet.SUN, Planet.MOON, Planet.MARS),
            Planet.VENUS to listOf(Planet.MERCURY, Planet.SATURN),
            Planet.SATURN to listOf(Planet.MERCURY, Planet.VENUS)
        )

        // Neutral relationships
        private val NEUTRALS = mapOf(
            Planet.SUN to listOf(Planet.MERCURY),
            Planet.MOON to listOf(Planet.MARS, Planet.JUPITER, Planet.VENUS, Planet.SATURN),
            Planet.MARS to listOf(Planet.MERCURY, Planet.VENUS, Planet.SATURN),
            Planet.MERCURY to listOf(Planet.MARS, Planet.JUPITER, Planet.SATURN),
            Planet.JUPITER to listOf(Planet.MERCURY, Planet.SATURN),
            Planet.VENUS to listOf(Planet.MARS, Planet.JUPITER),
            Planet.SATURN to listOf(Planet.MARS, Planet.JUPITER)
        )
    }

    init {
        ephemerisPath = context.filesDir.absolutePath + "/ephe"
        swissEph.swe_set_ephe_path(ephemerisPath)
        swissEph.swe_set_sid_mode(AYANAMSA_LAHIRI, 0.0, 0.0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DATA CLASSES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Tajika aspect types used in annual horoscopy
     */
    enum class TajikaAspectType(val displayName: String, val description: String, val isPositive: Boolean) {
        ITHASALA("Ithasala", "Applying aspect - promises fulfillment of matters", true),
        EASARAPHA("Easarapha", "Separating aspect - event has passed or is fading", false),
        NAKTA("Nakta", "Transmission of light with reception - indirect completion", true),
        YAMAYA("Yamaya", "Translation of light - third planet connects significators", true),
        MANAU("Manau", "Reverse application - slower planet applies to faster", false),
        KAMBOOLA("Kamboola", "Powerful Ithasala with angular placement", true),
        GAIRI_KAMBOOLA("Gairi-Kamboola", "Weaker form of Kamboola", true),
        KHALASARA("Khalasara", "Mutual separation - dissolution of matters", false),
        RADDA("Radda", "Refranation - retrograde breaks the aspect", false),
        DUHPHALI_KUTTHA("Duhphali-Kuttha", "Malefic intervention prevents completion", false),
        TAMBIRA("Tambira", "Indirect aspect through intermediary", true),
        KUTTHA("Kuttha", "Impediment to aspect completion", false),
        DURAPHA("Durapha", "Hard aspect causing difficulties", false),
        MUTHASHILA("Muthashila", "Mutual application between planets", true),
        IKKABALA("Ikkabala", "Unity of strength between planets", true)
    }

    /**
     * Aspect strength classification
     */
    enum class AspectStrength(val displayName: String, val weight: Double) {
        VERY_STRONG("Very Strong", 1.0),
        STRONG("Strong", 0.8),
        MODERATE("Moderate", 0.6),
        WEAK("Weak", 0.4),
        VERY_WEAK("Very Weak", 0.2)
    }

    /**
     * Saham (Arabic Part/Lot) types
     */
    enum class SahamType(
        val displayName: String,
        val sanskritName: String,
        val description: String
    ) {
        PUNYA("Fortune", "Punya Saham", "Overall luck and prosperity"),
        VIDYA("Education", "Vidya Saham", "Learning and knowledge"),
        YASHAS("Fame", "Yashas Saham", "Reputation and recognition"),
        MITRA("Friends", "Mitra Saham", "Friendship and alliances"),
        MAHATMYA("Greatness", "Mahatmya Saham", "Spiritual achievement"),
        ASHA("Hope", "Asha Saham", "Aspirations and wishes"),
        SAMARTHA("Capability", "Samartha Saham", "Ability and competence"),
        BHRATRI("Siblings", "Bhratri Saham", "Brothers and sisters"),
        PITRI("Father", "Pitri Saham", "Father's welfare"),
        MATRI("Mother", "Matri Saham", "Mother's welfare"),
        PUTRA("Children", "Putra Saham", "Offspring and progeny"),
        VIVAHA("Marriage", "Vivaha Saham", "Matrimony and partnership"),
        KARMA("Career", "Karma Saham", "Profession and livelihood"),
        ROGA("Disease", "Roga Saham", "Health challenges"),
        MRITYU("Longevity", "Mrityu Saham", "Life span indicators"),
        PARADESA("Foreign", "Paradesa Saham", "Travel and foreign lands"),
        DHANA("Wealth", "Dhana Saham", "Financial prosperity"),
        RAJA("Power", "Raja Saham", "Authority and position"),
        BANDHANA("Bondage", "Bandhana Saham", "Restrictions and obstacles"),
        KARYASIDDHI("Success", "Karyasiddhi Saham", "Accomplishment of goals")
    }

    /**
     * Key date types for the year
     */
    enum class KeyDateType {
        FAVORABLE, CHALLENGING, IMPORTANT, TRANSIT
    }

    /**
     * Solar Return Chart data
     */
    data class SolarReturnChart(
        val year: Int,
        val solarReturnTime: LocalDateTime,
        val solarReturnTimeUtc: LocalDateTime,
        val julianDay: Double,
        val planetPositions: Map<Planet, SolarReturnPlanetPosition>,
        val ascendant: ZodiacSign,
        val ascendantDegree: Double,
        val midheaven: Double,
        val houseCusps: List<Double>,
        val ayanamsa: Double,
        val isDayBirth: Boolean,
        val moonSign: ZodiacSign,
        val moonNakshatra: String
    )

    /**
     * Planet position in solar return chart
     */
    data class SolarReturnPlanetPosition(
        val longitude: Double,
        val sign: ZodiacSign,
        val house: Int,
        val degree: Double,
        val nakshatra: String,
        val nakshatraPada: Int,
        val isRetrograde: Boolean,
        val speed: Double
    )

    /**
     * Muntha (progressed ascendant) information
     */
    data class MunthaResult(
        val longitude: Double,
        val sign: ZodiacSign,
        val house: Int,
        val degree: Double,
        val lord: Planet,
        val lordHouse: Int,
        val lordStrength: String,
        val interpretation: String,
        val themes: List<String>
    )

    /**
     * Saham (Arabic Part) result
     */
    data class SahamResult(
        val type: SahamType,
        val name: String,
        val sanskritName: String,
        val formula: String,
        val longitude: Double,
        val sign: ZodiacSign,
        val house: Int,
        val degree: Double,
        val lord: Planet,
        val lordHouse: Int,
        val lordStrength: String,
        val interpretation: String,
        val isActive: Boolean,
        val activationPeriods: List<String>
    )

    /**
     * Tajika Aspect result
     */
    data class TajikaAspectResult(
        val type: TajikaAspectType,
        val planet1: Planet,
        val planet2: Planet,
        val planet1Longitude: Double,
        val planet2Longitude: Double,
        val aspectAngle: Int,
        val orb: Double,
        val isApplying: Boolean,
        val strength: AspectStrength,
        val relatedHouses: List<Int>,
        val effectDescription: String,
        val prediction: String
    )

    /**
     * Mudda Dasha period
     */
    data class MuddaDashaPeriod(
        val planet: Planet,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val days: Int,
        val subPeriods: List<MuddaAntardasha>,
        val planetStrength: String,
        val houseRuled: List<Int>,
        val prediction: String,
        val keywords: List<String>,
        val isCurrent: Boolean,
        val progressPercent: Float
    )

    /**
     * Mudda Antardasha (sub-period)
     */
    data class MuddaAntardasha(
        val planet: Planet,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val days: Int,
        val interpretation: String
    )

    /**
     * Pancha Vargiya Bala (five-fold strength)
     */
    data class PanchaVargiyaBala(
        val planet: Planet,
        val uchcha: Double,
        val hadda: Double,
        val dreshkana: Double,
        val navamsha: Double,
        val dwadashamsha: Double,
        val total: Double,
        val category: String
    )

    /**
     * Tri-Pataki Chakra sector
     */
    data class TriPatakiSector(
        val name: String,
        val signs: List<ZodiacSign>,
        val planets: List<Planet>,
        val influence: String
    )

    /**
     * Tri-Pataki Chakra (three-flag diagram)
     */
    data class TriPatakiChakra(
        val risingSign: ZodiacSign,
        val sectors: List<TriPatakiSector>,
        val dominantInfluence: String,
        val interpretation: String
    )

    /**
     * House prediction
     */
    data class HousePrediction(
        val house: Int,
        val signOnCusp: ZodiacSign,
        val houseLord: Planet,
        val lordPosition: Int,
        val planetsInHouse: List<Planet>,
        val strength: String,
        val keywords: List<String>,
        val prediction: String,
        val rating: Float,
        val specificEvents: List<String>
    )

    /**
     * Key date in the year
     */
    data class KeyDate(
        val date: LocalDate,
        val event: String,
        val type: KeyDateType,
        val description: String
    )

    /**
     * Complete Varshaphala result
     */
    data class VarshaphalaResult(
        val natalChart: VedicChart,
        val year: Int,
        val age: Int,
        val solarReturnChart: SolarReturnChart,
        val yearLord: Planet,
        val yearLordStrength: String,
        val yearLordHouse: Int,
        val yearLordDignity: String,
        val muntha: MunthaResult,
        val panchaVargiyaBala: List<PanchaVargiyaBala>,
        val triPatakiChakra: TriPatakiChakra,
        val sahams: List<SahamResult>,
        val tajikaAspects: List<TajikaAspectResult>,
        val muddaDasha: List<MuddaDashaPeriod>,
        val housePredictions: List<HousePrediction>,
        val majorThemes: List<String>,
        val favorableMonths: List<Int>,
        val challengingMonths: List<Int>,
        val overallPrediction: String,
        val yearRating: Float,
        val keyDates: List<KeyDate>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * Generate plain text report for export
         */
        fun toPlainText(): String = buildString {
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("            VARSHAPHALA (ANNUAL HOROSCOPE) REPORT")
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine()
            appendLine("Name: ${natalChart.birthData.name}")
            appendLine("Year: $year (Age: $age)")
            appendLine("Solar Return: ${solarReturnChart.solarReturnTime}")
            appendLine("Year Rating: ${String.format("%.1f", yearRating)}/5.0")
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                      YEAR LORD")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("Year Lord: ${yearLord.displayName} ($yearLordStrength)")
            appendLine("Position: House $yearLordHouse")
            appendLine(yearLordDignity)
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                       MUNTHA")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("Muntha Position: ${String.format("%.2f", muntha.degree)}° ${muntha.sign.displayName}")
            appendLine("Muntha House: ${muntha.house}")
            appendLine("Muntha Lord: ${muntha.lord.displayName} in House ${muntha.lordHouse}")
            appendLine(muntha.interpretation)
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                    MAJOR THEMES")
            appendLine("─────────────────────────────────────────────────────────")
            majorThemes.forEach { appendLine("• $it") }
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                   MUDDA DASHA PERIODS")
            appendLine("─────────────────────────────────────────────────────────")
            muddaDasha.forEach { period ->
                val marker = if (period.isCurrent) " [CURRENT]" else ""
                appendLine("${period.planet.displayName}: ${period.startDate} to ${period.endDate} (${period.days} days)$marker")
            }
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("              FAVORABLE MONTHS: ${favorableMonths.joinToString()}")
            appendLine("            CHALLENGING MONTHS: ${challengingMonths.joinToString()}")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine()
            appendLine("                   OVERALL PREDICTION")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine(overallPrediction)
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("Generated by AstroStorm - Ultra-Precision Vedic Astrology")
            appendLine("═══════════════════════════════════════════════════════════")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN CALCULATION METHOD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate complete Varshaphala for a given year
     */
    fun calculateVarshaphala(natalChart: VedicChart, year: Int): VarshaphalaResult {
        val birthDateTime = natalChart.birthData.dateTime
        val birthYear = birthDateTime.year
        val age = year - birthYear

        require(age >= 0) { "Year must be after birth year" }

        // Calculate solar return time using Swiss Ephemeris
        val solarReturnTime = calculateSolarReturnTime(
            natalChart.birthData.dateTime,
            year,
            natalChart.birthData.latitude,
            natalChart.birthData.longitude,
            natalChart.birthData.timezone
        )

        // Build complete solar return chart
        val solarReturnChart = calculateSolarReturnChart(
            solarReturnTime,
            natalChart.birthData.latitude,
            natalChart.birthData.longitude,
            natalChart.birthData.timezone,
            year
        )

        // Calculate Pancha Vargiya Bala for all planets
        val panchaVargiyaBala = calculateAllPanchaVargiyaBalas(solarReturnChart)

        // Calculate Muntha
        val muntha = calculateMuntha(natalChart, age, solarReturnChart)

        // Determine Year Lord
        val yearLord = determineYearLord(solarReturnChart, muntha, natalChart, panchaVargiyaBala)
        val yearLordHouse = solarReturnChart.planetPositions[yearLord]?.house ?: 1
        val yearLordStrength = evaluatePlanetStrengthDescription(yearLord, solarReturnChart)
        val yearLordDignity = getYearLordDignityDescription(yearLord, solarReturnChart)

        // Calculate Tri-Pataki Chakra
        val triPatakiChakra = calculateTriPatakiChakra(solarReturnChart)

        // Calculate Sahams
        val sahams = calculateSahams(solarReturnChart)

        // Calculate Tajika Aspects
        val tajikaAspects = calculateTajikaAspects(solarReturnChart)

        // Calculate Mudda Dasha
        val muddaDasha = calculateMuddaDasha(solarReturnChart, solarReturnTime.toLocalDate())

        // Generate House Predictions
        val housePredictions = generateHousePredictions(solarReturnChart, muntha, yearLord)

        // Identify Major Themes
        val majorThemes = identifyMajorThemes(solarReturnChart, muntha, yearLord, housePredictions, triPatakiChakra, tajikaAspects)

        // Calculate favorable and challenging months
        val (favorableMonths, challengingMonths) = calculateMonthlyInfluences(solarReturnChart, solarReturnTime)

        // Calculate key dates
        val keyDates = calculateKeyDates(solarReturnChart, solarReturnTime, muddaDasha)

        // Generate overall prediction
        val overallPrediction = generateOverallPrediction(
            solarReturnChart, yearLord, muntha, tajikaAspects, housePredictions
        )

        // Calculate year rating
        val yearRating = calculateYearRating(
            solarReturnChart, yearLord, muntha, tajikaAspects, housePredictions
        )

        return VarshaphalaResult(
            natalChart = natalChart,
            year = year,
            age = age,
            solarReturnChart = solarReturnChart,
            yearLord = yearLord,
            yearLordStrength = yearLordStrength,
            yearLordHouse = yearLordHouse,
            yearLordDignity = yearLordDignity,
            muntha = muntha,
            panchaVargiyaBala = panchaVargiyaBala,
            triPatakiChakra = triPatakiChakra,
            sahams = sahams,
            tajikaAspects = tajikaAspects,
            muddaDasha = muddaDasha,
            housePredictions = housePredictions,
            majorThemes = majorThemes,
            favorableMonths = favorableMonths,
            challengingMonths = challengingMonths,
            overallPrediction = overallPrediction,
            yearRating = yearRating,
            keyDates = keyDates
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SOLAR RETURN CALCULATIONS (Using Swiss Ephemeris)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate precise solar return time using Swiss Ephemeris
     */
    private fun calculateSolarReturnTime(
        birthDateTime: LocalDateTime,
        targetYear: Int,
        latitude: Double,
        longitude: Double,
        timezone: String
    ): LocalDateTime {
        val birthZoned = ZonedDateTime.of(birthDateTime, ZoneId.of(timezone))
        val birthUtc = birthZoned.withZoneSameInstant(ZoneId.of("UTC"))
        val birthJd = calculateJulianDay(birthUtc.toLocalDateTime())

        // Get natal Sun longitude using Swiss Ephemeris
        val natalSunLong = getPlanetLongitude(SweConst.SE_SUN, birthJd)

        // Approximate Julian Day for solar return
        val yearsElapsed = targetYear - birthDateTime.year
        val approximateJd = birthJd + (yearsElapsed * SIDEREAL_YEAR_DAYS)
        var currentJd = approximateJd

        // Iterative refinement to find exact solar return
        repeat(50) {
            val currentSunLong = getPlanetLongitude(SweConst.SE_SUN, currentJd)
            var diff = natalSunLong - currentSunLong

            while (diff > 180.0) diff -= 360.0
            while (diff < -180.0) diff += 360.0

            if (abs(diff) < 0.0000001) {
                return jdToLocalDateTime(currentJd, timezone)
            }

            val sunSpeed = getSunSpeed(currentJd)
            val correction = diff / sunSpeed
            currentJd += correction

            if (abs(correction) < 0.00001) {
                return jdToLocalDateTime(currentJd, timezone)
            }
        }

        return jdToLocalDateTime(currentJd, timezone)
    }

    /**
     * Get planet longitude from Swiss Ephemeris
     */
    private fun getPlanetLongitude(planetId: Int, julianDay: Double): Double {
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        swissEph.swe_calc_ut(julianDay, planetId, SEFLG_SIDEREAL or SEFLG_SPEED, xx, serr)
        return normalizeAngle360(xx[0])
    }

    /**
     * Get Sun speed (degrees per day)
     */
    private fun getSunSpeed(julianDay: Double): Double {
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        swissEph.swe_calc_ut(julianDay, SweConst.SE_SUN, SEFLG_SIDEREAL or SEFLG_SPEED, xx, serr)
        return if (xx[3] != 0.0) xx[3] else 0.9856 // Mean motion if speed unavailable
    }

    /**
     * Calculate complete solar return chart
     */
    private fun calculateSolarReturnChart(
        solarReturnTime: LocalDateTime,
        latitude: Double,
        longitude: Double,
        timezone: String,
        year: Int
    ): SolarReturnChart {
        val zonedDateTime = ZonedDateTime.of(solarReturnTime, ZoneId.of(timezone))
        val utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))
        val julianDay = calculateJulianDay(utcDateTime.toLocalDateTime())
        val ayanamsa = swissEph.swe_get_ayanamsa_ut(julianDay)

        // Calculate house cusps
        val cusps = DoubleArray(13)
        val ascmc = DoubleArray(10)
        swissEph.swe_houses(julianDay, SEFLG_SIDEREAL, latitude, longitude, 'W'.code, cusps, ascmc)

        val houseCusps = (1..12).map { cusps[it] }
        val ascendantDegree = cusps[1]
        val ascendant = ZodiacSign.fromLongitude(ascendantDegree)
        val midheaven = ascmc[1]

        // Calculate planet positions
        val planetPositions = mutableMapOf<Planet, SolarReturnPlanetPosition>()
        for (planet in Planet.MAIN_PLANETS) {
            val position = calculateSolarReturnPlanetPosition(planet, julianDay, ascendantDegree)
            planetPositions[planet] = position
        }

        // Determine if day or night birth
        val sunPos = planetPositions[Planet.SUN]?.longitude ?: 0.0
        val isDayBirth = isDayChart(sunPos, ascendantDegree)

        // Get Moon details
        val moonLong = planetPositions[Planet.MOON]?.longitude ?: 0.0
        val moonSign = ZodiacSign.fromLongitude(moonLong)
        val (moonNakshatra, _) = Nakshatra.fromLongitude(moonLong)

        return SolarReturnChart(
            year = year,
            solarReturnTime = solarReturnTime,
            solarReturnTimeUtc = utcDateTime.toLocalDateTime(),
            julianDay = julianDay,
            planetPositions = planetPositions,
            ascendant = ascendant,
            ascendantDegree = ascendantDegree % 30.0,
            midheaven = midheaven,
            houseCusps = houseCusps,
            ayanamsa = ayanamsa,
            isDayBirth = isDayBirth,
            moonSign = moonSign,
            moonNakshatra = moonNakshatra.displayName
        )
    }

    /**
     * Calculate planet position for solar return chart
     */
    private fun calculateSolarReturnPlanetPosition(
        planet: Planet,
        julianDay: Double,
        ascendantLongitude: Double
    ): SolarReturnPlanetPosition {
        val xx = DoubleArray(6)
        val serr = StringBuffer()

        val planetId = when (planet) {
            Planet.KETU -> -1 // Calculate from Rahu
            else -> planet.swissEphId
        }

        if (planetId >= 0) {
            swissEph.swe_calc_ut(julianDay, planetId, SEFLG_SIDEREAL or SEFLG_SPEED, xx, serr)
        } else {
            // Ketu is 180° from Rahu
            swissEph.swe_calc_ut(julianDay, SweConst.SE_MEAN_NODE, SEFLG_SIDEREAL or SEFLG_SPEED, xx, serr)
            xx[0] = normalizeAngle360(xx[0] + 180.0)
            xx[3] = -xx[3]
        }

        val longitude = normalizeAngle360(xx[0])
        val sign = ZodiacSign.fromLongitude(longitude)
        val house = calculateWholeSignHouse(longitude, ascendantLongitude)
        val degree = longitude % 30.0
        val (nakshatra, pada) = Nakshatra.fromLongitude(longitude)
        val isRetrograde = xx[3] < 0
        val speed = xx[3]

        return SolarReturnPlanetPosition(
            longitude = longitude,
            sign = sign,
            house = house,
            degree = degree,
            nakshatra = nakshatra.displayName,
            nakshatraPada = pada,
            isRetrograde = isRetrograde,
            speed = speed
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // YEAR LORD CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Determine Year Lord using traditional Tajika rules
     *
     * The Year Lord (Varsheshwara) is determined from five candidates:
     * 1. Dina Pati (Day Lord)
     * 2. Lagna Pati (Ascendant Lord)
     * 3. Muntha Pati (Muntha Lord)
     * 4. Tri-Rashi Pati (Tri-sign Lord)
     * 5. Janma Rashi Pati (Moon Sign Lord from birth chart)
     *
     * The strongest among these becomes the Year Lord based on Pancha Vargiya Bala
     */
    private fun determineYearLord(
        solarReturnChart: SolarReturnChart,
        muntha: MunthaResult,
        natalChart: VedicChart,
        allBalas: List<PanchaVargiyaBala>
    ): Planet {
        // 1. Dina Pati (Day Lord)
        val dayOfWeek = solarReturnChart.solarReturnTime.dayOfWeek
        val dayIndex = when (dayOfWeek) {
            DayOfWeek.SUNDAY -> 0
            DayOfWeek.MONDAY -> 1
            DayOfWeek.TUESDAY -> 2
            DayOfWeek.WEDNESDAY -> 3
            DayOfWeek.THURSDAY -> 4
            DayOfWeek.FRIDAY -> 5
            DayOfWeek.SATURDAY -> 6
        }
        val dinaPati = DAY_LORDS[dayIndex]

        // 2. Lagna Pati (Ascendant Lord)
        val lagnaPati = solarReturnChart.ascendant.ruler

        // 3. Muntha Pati (Muntha Lord)
        val munthaPati = muntha.lord

        // 4. Janma Rashi Pati (Moon Sign Lord from birth chart)
        val natalMoonSign = natalChart.planetPositions.find { it.planet == Planet.MOON }?.sign
            ?: ZodiacSign.ARIES
        val janmaRashiPati = natalMoonSign.ruler

        // Collect unique candidates
        val candidates = listOf(dinaPati, lagnaPati, munthaPati, janmaRashiPati).distinct()

        // Calculate strength for each candidate
        val candidatesWithStrength = candidates.map { planet ->
            val bala = allBalas.find { it.planet == planet }?.total ?: 0.0
            val additionalStrength = calculateAdditionalStrength(planet, solarReturnChart)
            planet to (bala + additionalStrength)
        }

        // Return the strongest candidate
        return candidatesWithStrength.maxByOrNull { it.second }?.first ?: dinaPati
    }

    /**
     * Calculate additional strength factors for year lord determination
     */
    private fun calculateAdditionalStrength(planet: Planet, chart: SolarReturnChart): Double {
        var strength = 0.0
        val position = chart.planetPositions[planet] ?: return 0.0

        // Angular placement bonus
        when (position.house) {
            1, 4, 7, 10 -> strength += 5.0  // Kendra (angular) houses
            5, 9 -> strength += 3.0          // Trikona (trine) houses
            6, 8, 12 -> strength -= 3.0      // Dusthana (malefic) houses
        }

        // Dignity considerations
        if (position.sign.ruler == planet) strength += 4.0  // Own sign
        if (isExalted(planet, position.sign)) strength += 5.0
        if (isDebilitated(planet, position.sign)) strength -= 5.0

        // Direct motion bonus
        if (!position.isRetrograde) strength += 1.0

        return strength
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MUNTHA CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Muntha (progressed ascendant)
     *
     * Muntha progresses one sign per year from the natal ascendant.
     * Its position and lord are significant indicators for the year.
     */
    private fun calculateMuntha(
        natalChart: VedicChart,
        age: Int,
        solarReturnChart: SolarReturnChart
    ): MunthaResult {
        val natalAscLongitude = natalChart.ascendant
        val progressedLongitude = normalizeAngle360(natalAscLongitude + (age * 30.0))
        val munthaSign = ZodiacSign.fromLongitude(progressedLongitude)
        val degreeInSign = progressedLongitude % 30.0
        val munthaHouse = calculateWholeSignHouse(progressedLongitude,
            solarReturnChart.ascendant.ordinal * 30.0 + solarReturnChart.ascendantDegree)
        val munthaLord = munthaSign.ruler

        val lordPosition = solarReturnChart.planetPositions[munthaLord]
        val lordHouse = lordPosition?.house ?: 1
        val lordStrength = evaluatePlanetStrengthDescription(munthaLord, solarReturnChart)

        val themes = getMunthaThemes(munthaHouse)
        val interpretation = generateMunthaInterpretation(munthaSign, munthaHouse, munthaLord, lordHouse, lordStrength)

        return MunthaResult(
            longitude = progressedLongitude,
            sign = munthaSign,
            house = munthaHouse,
            degree = degreeInSign,
            lord = munthaLord,
            lordHouse = lordHouse,
            lordStrength = lordStrength,
            interpretation = interpretation,
            themes = themes
        )
    }

    private fun getMunthaThemes(house: Int): List<String> {
        return when (house) {
            1 -> listOf("Personal Growth", "New Beginnings", "Health Focus")
            2 -> listOf("Financial Gains", "Family Matters", "Speech")
            3 -> listOf("Communication", "Short Travels", "Siblings")
            4 -> listOf("Home Affairs", "Property", "Inner Peace")
            5 -> listOf("Creativity", "Romance", "Children")
            6 -> listOf("Service", "Health Issues", "Competition")
            7 -> listOf("Partnerships", "Marriage", "Business")
            8 -> listOf("Transformation", "Research", "Inheritance")
            9 -> listOf("Fortune", "Long Travel", "Higher Learning")
            10 -> listOf("Career Advancement", "Recognition", "Authority")
            11 -> listOf("Gains", "Friends", "Fulfilled Wishes")
            12 -> listOf("Spirituality", "Foreign Lands", "Expenses")
            else -> listOf("General Growth")
        }
    }

    private fun generateMunthaInterpretation(
        sign: ZodiacSign,
        house: Int,
        lord: Planet,
        lordHouse: Int,
        lordStrength: String
    ): String {
        val houseSignificance = getHouseSignificance(house)

        val lordQuality = when (lordStrength) {
            "Exalted", "Strong" -> "excellent"
            "Moderate", "Angular" -> "favorable"
            "Debilitated" -> "challenging but growth-oriented"
            else -> "variable"
        }

        return "Muntha in ${sign.displayName} in the ${house}${getOrdinalSuffix(house)} house focuses the year's energy on $houseSignificance. " +
                "The Muntha lord ${lord.displayName} in house $lordHouse provides $lordQuality support for these matters."
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PANCHA VARGIYA BALA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Pancha Vargiya Bala for all planets
     */
    private fun calculateAllPanchaVargiyaBalas(chart: SolarReturnChart): List<PanchaVargiyaBala> {
        return Planet.MAIN_PLANETS.filter { it != Planet.RAHU && it != Planet.KETU }
            .map { calculatePanchaVargiyaBala(it, chart) }
    }

    /**
     * Calculate five-fold planetary strength
     *
     * Components:
     * 1. Uchcha Bala - Exaltation strength
     * 2. Hadda Bala - Term strength
     * 3. Drekkana Bala - Decanate strength
     * 4. Navamsha Bala - Ninth divisional strength
     * 5. Dwadashamsha Bala - Twelfth divisional strength
     */
    private fun calculatePanchaVargiyaBala(planet: Planet, chart: SolarReturnChart): PanchaVargiyaBala {
        val position = chart.planetPositions[planet]
            ?: return PanchaVargiyaBala(planet, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "Unknown")

        val longitude = position.longitude

        val uchcha = calculateUchchaBala(planet, longitude)
        val hadda = calculateHaddaBala(planet, position.sign, position.degree)
        val dreshkana = calculateDrekkanaBala(planet, position.sign, position.degree)
        val navamsha = calculateNavamshaBala(planet, longitude)
        val dwadashamsha = calculateDwadashamshabala(planet, longitude)

        val total = uchcha + hadda + dreshkana + navamsha + dwadashamsha

        val category = when {
            total >= 15 -> "Excellent"
            total >= 12 -> "Good"
            total >= 8 -> "Average"
            total >= 5 -> "Below Average"
            else -> "Weak"
        }

        return PanchaVargiyaBala(
            planet = planet,
            uchcha = uchcha,
            hadda = hadda,
            dreshkana = dreshkana,
            navamsha = navamsha,
            dwadashamsha = dwadashamsha,
            total = total,
            category = category
        )
    }

    private fun calculateUchchaBala(planet: Planet, longitude: Double): Double {
        val exaltationPoint = EXALTATION_DEGREES[planet] ?: return 0.0
        val diff = abs(normalizeAngle360(longitude - exaltationPoint))
        val adjustedDiff = if (diff > 180) 360 - diff else diff
        return ((180 - adjustedDiff) / 180.0 * 5.0).coerceIn(0.0, 5.0)
    }

    private fun calculateHaddaBala(planet: Planet, sign: ZodiacSign, degree: Double): Double {
        val haddaRanges = HADDA_LORDS[sign] ?: return 2.0

        for ((start, end, lord) in haddaRanges) {
            if (degree >= start && degree < end) {
                return when {
                    lord == planet -> 4.0
                    areFriends(planet, lord) -> 3.0
                    areNeutral(planet, lord) -> 2.0
                    else -> 1.0
                }
            }
        }
        return 2.0
    }

    private fun calculateDrekkanaBala(planet: Planet, sign: ZodiacSign, degree: Double): Double {
        val drekkanaNumber = when {
            degree < 10 -> 1
            degree < 20 -> 2
            else -> 3
        }

        val drekkanaSignIndex = (sign.number - 1 + (drekkanaNumber - 1) * 4) % 12
        val drekkanaSign = ZodiacSign.entries[drekkanaSignIndex]
        val drekkanaLord = drekkanaSign.ruler

        return when {
            drekkanaLord == planet -> 4.0
            areFriends(planet, drekkanaLord) -> 3.0
            areNeutral(planet, drekkanaLord) -> 2.0
            else -> 1.0
        }
    }

    private fun calculateNavamshaBala(planet: Planet, longitude: Double): Double {
        val degreeInSign = longitude % 30.0
        val navamshaIndex = (degreeInSign / 3.333333).toInt()
        val signIndex = (longitude / 30.0).toInt()

        val startSign = when (signIndex % 4) {
            0 -> 0  // Fire signs start from Aries
            1 -> 9  // Earth signs start from Capricorn
            2 -> 6  // Air signs start from Libra
            else -> 3  // Water signs start from Cancer
        }

        val navamshaSignIndex = (startSign + navamshaIndex) % 12
        val navamshaLord = ZodiacSign.entries[navamshaSignIndex].ruler

        return when {
            navamshaLord == planet -> 4.0
            areFriends(planet, navamshaLord) -> 3.0
            areNeutral(planet, navamshaLord) -> 2.0
            else -> 1.0
        }
    }

    private fun calculateDwadashamshabala(planet: Planet, longitude: Double): Double {
        val degreeInSign = longitude % 30.0
        val d12Index = (degreeInSign / 2.5).toInt()
        val signIndex = (longitude / 30.0).toInt()
        val d12SignIndex = (signIndex + d12Index) % 12
        val d12Lord = ZodiacSign.entries[d12SignIndex].ruler

        return when {
            d12Lord == planet -> 3.0
            areFriends(planet, d12Lord) -> 2.5
            areNeutral(planet, d12Lord) -> 1.5
            else -> 1.0
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TRI-PATAKI CHAKRA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Tri-Pataki Chakra (Three-Flag Diagram)
     *
     * Divides the zodiac into three sectors based on ascendant:
     * 1. Dharma Trikona (1, 5, 9) - Righteousness
     * 2. Artha Trikona (2, 6, 10) - Material prosperity
     * 3. Kama Trikona (3, 7, 11) - Desires
     */
    private fun calculateTriPatakiChakra(chart: SolarReturnChart): TriPatakiChakra {
        val ascIndex = chart.ascendant.ordinal

        val dharmaSigns = listOf(
            ZodiacSign.entries[ascIndex],
            ZodiacSign.entries[(ascIndex + 4) % 12],
            ZodiacSign.entries[(ascIndex + 8) % 12]
        )

        val arthaSigns = listOf(
            ZodiacSign.entries[(ascIndex + 1) % 12],
            ZodiacSign.entries[(ascIndex + 5) % 12],
            ZodiacSign.entries[(ascIndex + 9) % 12]
        )

        val kamaSigns = listOf(
            ZodiacSign.entries[(ascIndex + 2) % 12],
            ZodiacSign.entries[(ascIndex + 6) % 12],
            ZodiacSign.entries[(ascIndex + 10) % 12]
        )

        fun getPlanetsInSector(signs: List<ZodiacSign>): List<Planet> {
            return chart.planetPositions.filter { (_, pos) -> pos.sign in signs }.keys.toList()
        }

        val dharmaPlanets = getPlanetsInSector(dharmaSigns)
        val arthaPlanets = getPlanetsInSector(arthaSigns)
        val kamaPlanets = getPlanetsInSector(kamaSigns)

        val sectors = listOf(
            TriPatakiSector(
                name = "Dharma (1, 5, 9)",
                signs = dharmaSigns,
                planets = dharmaPlanets,
                influence = generateSectorInfluence("Dharma", dharmaPlanets)
            ),
            TriPatakiSector(
                name = "Artha (2, 6, 10)",
                signs = arthaSigns,
                planets = arthaPlanets,
                influence = generateSectorInfluence("Artha", arthaPlanets)
            ),
            TriPatakiSector(
                name = "Kama (3, 7, 11)",
                signs = kamaSigns,
                planets = kamaPlanets,
                influence = generateSectorInfluence("Kama", kamaPlanets)
            )
        )

        val dominantSector = sectors.maxByOrNull { it.planets.size }
        val dominantInfluence = when (dominantSector?.name?.take(6)) {
            "Dharma" -> "Spiritual growth and righteous pursuits dominate"
            "Artha" -> "Material prosperity and career emphasis"
            "Kama" -> "Relationships and desires take center stage"
            else -> "Balanced influences across all areas"
        }

        val interpretation = buildTriPatakiInterpretation(sectors)

        return TriPatakiChakra(
            risingSign = chart.ascendant,
            sectors = sectors,
            dominantInfluence = dominantInfluence,
            interpretation = interpretation
        )
    }

    private fun generateSectorInfluence(sectorName: String, planets: List<Planet>): String {
        if (planets.isEmpty()) {
            return "No planets in $sectorName sector - quieter year for these matters."
        }

        val benefics = planets.filter { it in listOf(Planet.JUPITER, Planet.VENUS, Planet.MOON, Planet.MERCURY) }
        val malefics = planets.filter { it in listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU) }

        return when {
            benefics.size > malefics.size -> "Benefic ${benefics.joinToString { it.displayName }} bring favorable influences."
            malefics.size > benefics.size -> "Malefic ${malefics.joinToString { it.displayName }} bring challenges requiring effort."
            else -> "Mixed influences suggest variable results."
        }
    }

    private fun buildTriPatakiInterpretation(sectors: List<TriPatakiSector>): String {
        val interpretations = mutableListOf<String>()

        sectors.forEach { sector ->
            if (sector.planets.isNotEmpty()) {
                val areaName = when {
                    sector.name.startsWith("Dharma") -> "righteousness, fortune, and higher learning"
                    sector.name.startsWith("Artha") -> "wealth, career, and practical achievements"
                    else -> "relationships, desires, and social connections"
                }
                interpretations.add("${sector.planets.size} planet(s) in ${sector.name.take(6)} trikona emphasizes $areaName.")
            }
        }

        return if (interpretations.isNotEmpty()) {
            interpretations.joinToString(" ")
        } else {
            "Balanced distribution of planetary energies across all life sectors."
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SAHAMS (ARABIC PARTS)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Sahams (Arabic Parts/Lots)
     */
    private fun calculateSahams(chart: SolarReturnChart): List<SahamResult> {
        val sahams = mutableListOf<SahamResult>()
        val isDayBirth = chart.isDayBirth

        val sunLong = chart.planetPositions[Planet.SUN]?.longitude ?: 0.0
        val moonLong = chart.planetPositions[Planet.MOON]?.longitude ?: 0.0
        val marsLong = chart.planetPositions[Planet.MARS]?.longitude ?: 0.0
        val mercuryLong = chart.planetPositions[Planet.MERCURY]?.longitude ?: 0.0
        val jupiterLong = chart.planetPositions[Planet.JUPITER]?.longitude ?: 0.0
        val venusLong = chart.planetPositions[Planet.VENUS]?.longitude ?: 0.0
        val saturnLong = chart.planetPositions[Planet.SATURN]?.longitude ?: 0.0
        val ascLong = chart.ascendant.ordinal * 30.0 + chart.ascendantDegree

        val sahamFormulas = listOf(
            Triple(SahamType.PUNYA, { if (isDayBirth) moonLong + ascLong - sunLong else sunLong + ascLong - moonLong }, "Moon + Asc - Sun"),
            Triple(SahamType.VIDYA, { if (isDayBirth) mercuryLong + ascLong - sunLong else sunLong + ascLong - mercuryLong }, "Mercury + Asc - Sun"),
            Triple(SahamType.YASHAS, { if (isDayBirth) jupiterLong + ascLong - sunLong else sunLong + ascLong - jupiterLong }, "Jupiter + Asc - Sun"),
            Triple(SahamType.MITRA, { if (isDayBirth) moonLong + ascLong - mercuryLong else mercuryLong + ascLong - moonLong }, "Moon + Asc - Mercury"),
            Triple(SahamType.DHANA, { if (isDayBirth) jupiterLong + ascLong - moonLong else moonLong + ascLong - jupiterLong }, "Jupiter + Asc - Moon"),
            Triple(SahamType.KARMA, { if (isDayBirth) saturnLong + ascLong - sunLong else sunLong + ascLong - saturnLong }, "Saturn + Asc - Sun"),
            Triple(SahamType.VIVAHA, { if (isDayBirth) venusLong + ascLong - saturnLong else saturnLong + ascLong - venusLong }, "Venus + Asc - Saturn"),
            Triple(SahamType.PUTRA, { if (isDayBirth) jupiterLong + ascLong - moonLong else moonLong + ascLong - jupiterLong }, "Jupiter + Asc - Moon"),
            Triple(SahamType.PITRI, { if (isDayBirth) saturnLong + ascLong - sunLong else sunLong + ascLong - saturnLong }, "Saturn + Asc - Sun"),
            Triple(SahamType.MATRI, { if (isDayBirth) moonLong + ascLong - venusLong else venusLong + ascLong - moonLong }, "Moon + Asc - Venus"),
            Triple(SahamType.SAMARTHA, { if (isDayBirth) marsLong + ascLong - saturnLong else saturnLong + ascLong - marsLong }, "Mars + Asc - Saturn"),
            Triple(SahamType.ASHA, { if (isDayBirth) saturnLong + ascLong - venusLong else venusLong + ascLong - saturnLong }, "Saturn + Asc - Venus"),
            Triple(SahamType.ROGA, { if (isDayBirth) saturnLong + ascLong - marsLong else marsLong + ascLong - saturnLong }, "Saturn + Asc - Mars"),
            Triple(SahamType.RAJA, { if (isDayBirth) sunLong + ascLong - saturnLong else saturnLong + ascLong - sunLong }, "Sun + Asc - Saturn"),
            Triple(SahamType.MRITYU, { if (isDayBirth) saturnLong + ascLong - moonLong else moonLong + ascLong - saturnLong }, "Saturn + Asc - Moon"),
            Triple(SahamType.BHRATRI, { if (isDayBirth) jupiterLong + ascLong - saturnLong else saturnLong + ascLong - jupiterLong }, "Jupiter + Asc - Saturn"),
            Triple(SahamType.MAHATMYA, { if (isDayBirth) jupiterLong + ascLong - moonLong else moonLong + ascLong - jupiterLong }, "Jupiter + Asc - Moon"),
            Triple(SahamType.KARYASIDDHI, { if (isDayBirth) saturnLong + ascLong - sunLong else sunLong + ascLong - saturnLong }, "Saturn + Asc - Sun")
        )

        for ((type, formula, formulaStr) in sahamFormulas) {
            try {
                val longitude = normalizeAngle360(formula())
                val sign = ZodiacSign.fromLongitude(longitude)
                val house = calculateWholeSignHouse(longitude, ascLong)
                val degree = longitude % 30.0
                val lord = sign.ruler
                val lordHouse = chart.planetPositions[lord]?.house ?: 1
                val lordStrength = evaluatePlanetStrengthDescription(lord, chart)

                val isActive = isSahamActive(lord, chart, house)
                val interpretation = generateSahamInterpretation(type, sign, house, lord, lordHouse, lordStrength)
                val activationPeriods = getSahamActivationPeriods(lord)

                sahams.add(
                    SahamResult(
                        type = type,
                        name = type.displayName,
                        sanskritName = type.sanskritName,
                        formula = formulaStr,
                        longitude = longitude,
                        sign = sign,
                        house = house,
                        degree = degree,
                        lord = lord,
                        lordHouse = lordHouse,
                        lordStrength = lordStrength,
                        interpretation = interpretation,
                        isActive = isActive,
                        activationPeriods = activationPeriods
                    )
                )
            } catch (e: Exception) {
                // Skip sahams that can't be calculated
            }
        }

        return sahams.sortedByDescending { it.isActive }
    }

    private fun isSahamActive(lord: Planet, chart: SolarReturnChart, house: Int): Boolean {
        val lordPosition = chart.planetPositions[lord] ?: return false
        val lordStrength = evaluatePlanetStrengthDescription(lord, chart)

        val isLordStrong = lordStrength in listOf("Exalted", "Strong", "Angular")
        val isInGoodHouse = house in listOf(1, 2, 4, 5, 7, 9, 10, 11)
        val isLordWellPlaced = lordPosition.house in listOf(1, 4, 5, 7, 9, 10, 11)

        return (isLordStrong && isInGoodHouse) || (isLordWellPlaced && !lordPosition.isRetrograde)
    }

    private fun generateSahamInterpretation(
        type: SahamType,
        sign: ZodiacSign,
        house: Int,
        lord: Planet,
        lordHouse: Int,
        lordStrength: String
    ): String {
        val lordQuality = when (lordStrength) {
            "Exalted", "Strong" -> "well-placed, promising positive outcomes"
            "Moderate", "Angular" -> "providing reasonable support"
            "Debilitated", "Weak" -> "requiring attention and effort"
            else -> "influencing matters variably"
        }

        return "The ${type.displayName} Saham in ${sign.displayName} (House $house) relates to ${type.description.lowercase()} this year. " +
                "Its lord ${lord.displayName} in House $lordHouse is $lordQuality."
    }

    private fun getSahamActivationPeriods(lord: Planet): List<String> {
        val periods = mutableListOf<String>()
        if (lord in VIMSHOTTARI_ORDER) {
            periods.add("${lord.displayName} Mudda Dasha")
        }
        return periods
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TAJIKA ASPECTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Tajika aspects between planets
     */
    private fun calculateTajikaAspects(chart: SolarReturnChart): List<TajikaAspectResult> {
        val aspects = mutableListOf<TajikaAspectResult>()
        val planets = listOf(
            Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
            Planet.JUPITER, Planet.VENUS, Planet.SATURN
        )

        val aspectAngles = listOf(0, 60, 90, 120, 180)

        for (i in planets.indices) {
            for (j in (i + 1) until planets.size) {
                val planet1 = planets[i]
                val planet2 = planets[j]

                val pos1 = chart.planetPositions[planet1] ?: continue
                val pos2 = chart.planetPositions[planet2] ?: continue

                val diff = abs(normalizeAngle360(pos1.longitude - pos2.longitude))

                for (angle in aspectAngles) {
                    val maxOrb = when (angle) {
                        0 -> CONJUNCTION_ORB
                        60 -> SEXTILE_ORB
                        90 -> SQUARE_ORB
                        120 -> TRINE_ORB
                        180 -> OPPOSITION_ORB
                        else -> 5.0
                    }

                    val actualOrb = abs(diff - angle)
                    val reverseOrb = abs(diff - (360 - angle))
                    val effectiveOrb = min(actualOrb, reverseOrb)

                    if (effectiveOrb <= maxOrb) {
                        val isApplying = determineTajikaApplication(pos1.longitude, pos2.longitude, pos1.speed, pos2.speed)

                        val aspectType = determineTajikaAspectType(
                            planet1, planet2, pos1, pos2, isApplying, effectiveOrb, angle, chart
                        )

                        val strength = calculateAspectStrength(effectiveOrb, maxOrb, angle, isApplying)
                        val relatedHouses = listOf(pos1.house, pos2.house).distinct()
                        val effectDescription = getAspectEffectDescription(aspectType, planet1, planet2)
                        val prediction = generateAspectPrediction(aspectType, planet1, planet2, relatedHouses)

                        aspects.add(
                            TajikaAspectResult(
                                type = aspectType,
                                planet1 = planet1,
                                planet2 = planet2,
                                planet1Longitude = pos1.longitude,
                                planet2Longitude = pos2.longitude,
                                aspectAngle = angle,
                                orb = effectiveOrb,
                                isApplying = isApplying,
                                strength = strength,
                                relatedHouses = relatedHouses,
                                effectDescription = effectDescription,
                                prediction = prediction
                            )
                        )
                    }
                }
            }
        }

        return aspects.sortedByDescending { it.strength.weight }
    }

    private fun determineTajikaApplication(long1: Double, long2: Double, speed1: Double, speed2: Double): Boolean {
        val diff = normalizeAngle360(long2 - long1)
        return if (diff < 180) speed1 > speed2 else speed2 > speed1
    }

    private fun determineTajikaAspectType(
        planet1: Planet,
        planet2: Planet,
        pos1: SolarReturnPlanetPosition,
        pos2: SolarReturnPlanetPosition,
        isApplying: Boolean,
        orb: Double,
        angle: Int,
        chart: SolarReturnChart
    ): TajikaAspectType {
        val isAngular1 = pos1.house in listOf(1, 4, 7, 10)
        val isAngular2 = pos2.house in listOf(1, 4, 7, 10)
        val hasReception = checkMutualReception(planet1, planet2, pos1.sign, pos2.sign)

        return when {
            isApplying && angle == 0 && orb < 3 -> {
                if (isAngular1 || isAngular2) TajikaAspectType.KAMBOOLA else TajikaAspectType.ITHASALA
            }
            isApplying && orb < 5 -> {
                if (hasReception) TajikaAspectType.NAKTA else TajikaAspectType.ITHASALA
            }
            !isApplying && orb < 5 -> TajikaAspectType.EASARAPHA
            pos1.isRetrograde || pos2.isRetrograde -> TajikaAspectType.RADDA
            pos1.speed < pos2.speed && isApplying -> TajikaAspectType.MANAU
            isApplying && hasReception -> TajikaAspectType.MUTHASHILA
            angle == 90 || angle == 180 -> TajikaAspectType.DURAPHA
            isAngular1 && isAngular2 && !isApplying -> TajikaAspectType.GAIRI_KAMBOOLA
            else -> if (isApplying) TajikaAspectType.ITHASALA else TajikaAspectType.EASARAPHA
        }
    }

    private fun checkMutualReception(planet1: Planet, planet2: Planet, sign1: ZodiacSign, sign2: ZodiacSign): Boolean {
        return sign1.ruler == planet2 && sign2.ruler == planet1
    }

    private fun calculateAspectStrength(orb: Double, maxOrb: Double, angle: Int, isApplying: Boolean): AspectStrength {
        val orbRatio = orb / maxOrb
        val angleBonus = when (angle) {
            0, 120 -> 0.2
            60 -> 0.1
            90, 180 -> -0.1
            else -> 0.0
        }
        val applyingBonus = if (isApplying) 0.1 else 0.0

        val strength = 1.0 - orbRatio + angleBonus + applyingBonus

        return when {
            strength >= 0.9 -> AspectStrength.VERY_STRONG
            strength >= 0.7 -> AspectStrength.STRONG
            strength >= 0.5 -> AspectStrength.MODERATE
            strength >= 0.3 -> AspectStrength.WEAK
            else -> AspectStrength.VERY_WEAK
        }
    }

    private fun getAspectEffectDescription(type: TajikaAspectType, planet1: Planet, planet2: Planet): String {
        return when (type) {
            TajikaAspectType.ITHASALA -> "${planet1.displayName} applying to ${planet2.displayName} promises fulfillment"
            TajikaAspectType.EASARAPHA -> "Separating aspect suggests matters are concluding"
            TajikaAspectType.KAMBOOLA -> "Powerful angular conjunction promises prominent success"
            TajikaAspectType.RADDA -> "Retrograde motion causes delays or reversals"
            TajikaAspectType.DURAPHA -> "Hard aspect creates challenges that strengthen through difficulty"
            else -> "${type.displayName} influences matters with ${if (type.isPositive) "supportive" else "challenging"} energy"
        }
    }

    private fun generateAspectPrediction(type: TajikaAspectType, planet1: Planet, planet2: Planet, houses: List<Int>): String {
        val houseStr = houses.joinToString(" and ") { "House $it" }
        val quality = if (type.isPositive) "favorable" else "requiring attention"

        return "The ${type.displayName} between ${planet1.displayName} and ${planet2.displayName} is $quality for matters of $houseStr."
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MUDDA DASHA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calculate Mudda Dasha (annual planetary periods)
     */
    private fun calculateMuddaDasha(chart: SolarReturnChart, startDate: LocalDate): List<MuddaDashaPeriod> {
        val dashaOrder = listOf(
            Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
            Planet.JUPITER, Planet.VENUS, Planet.SATURN, Planet.RAHU, Planet.KETU
        )

        // Proportional days based on Vimshottari years (scaled to 365 days)
        val dashaDays = mapOf(
            Planet.SUN to 110,
            Planet.MOON to 60,
            Planet.MARS to 32,
            Planet.MERCURY to 40,
            Planet.JUPITER to 48,
            Planet.VENUS to 56,
            Planet.SATURN to 4,
            Planet.RAHU to 5,
            Planet.KETU to 5
        )

        val totalDays = 360
        val today = LocalDate.now()

        // Determine starting lord based on Moon's nakshatra
        val moonLong = chart.planetPositions[Planet.MOON]?.longitude ?: 0.0
        val (nakshatra, _) = Nakshatra.fromLongitude(moonLong)
        val startingLord = nakshatra.ruler
        val startIndex = dashaOrder.indexOf(
            when (startingLord) {
                in dashaOrder -> startingLord
                else -> Planet.SUN
            }
        )

        val periods = mutableListOf<MuddaDashaPeriod>()
        var currentDate = startDate

        for (i in dashaOrder.indices) {
            val planetIndex = (startIndex + i) % dashaOrder.size
            val planet = dashaOrder[planetIndex]
            val days = (dashaDays[planet] ?: 30) * totalDays / 360

            val endDate = currentDate.plusDays(days.toLong() - 1)
            val isCurrent = today >= currentDate && today <= endDate

            val progressPercent = if (isCurrent) {
                val daysPassed = ChronoUnit.DAYS.between(currentDate, today).toFloat()
                (daysPassed / days).coerceIn(0f, 1f)
            } else if (today > endDate) {
                1f
            } else {
                0f
            }

            val subPeriods = calculateMuddaAntardasha(planet, currentDate, endDate, dashaOrder)
            val planetStrength = evaluatePlanetStrengthDescription(planet, chart)
            val houseRuled = getHousesRuledBy(planet, chart)
            val prediction = generateDashaPrediction(planet, chart, planetStrength)
            val keywords = getDashaKeywords(planet, chart)

            periods.add(
                MuddaDashaPeriod(
                    planet = planet,
                    startDate = currentDate,
                    endDate = endDate,
                    days = days,
                    subPeriods = subPeriods,
                    planetStrength = planetStrength,
                    houseRuled = houseRuled,
                    prediction = prediction,
                    keywords = keywords,
                    isCurrent = isCurrent,
                    progressPercent = progressPercent
                )
            )

            currentDate = endDate.plusDays(1)
        }

        return periods
    }

    private fun calculateMuddaAntardasha(
        mainPlanet: Planet,
        startDate: LocalDate,
        endDate: LocalDate,
        dashaOrder: List<Planet>
    ): List<MuddaAntardasha> {
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        val subPeriods = mutableListOf<MuddaAntardasha>()

        val startIndex = dashaOrder.indexOf(mainPlanet)
        var currentDate = startDate
        val subDays = totalDays / 9

        for (i in dashaOrder.indices) {
            val planetIndex = (startIndex + i) % dashaOrder.size
            val planet = dashaOrder[planetIndex]

            val actualSubDays = if (i == dashaOrder.size - 1) {
                ChronoUnit.DAYS.between(currentDate, endDate).toInt() + 1
            } else {
                subDays
            }

            val subEndDate = currentDate.plusDays(actualSubDays.toLong() - 1)

            subPeriods.add(
                MuddaAntardasha(
                    planet = planet,
                    startDate = currentDate,
                    endDate = subEndDate,
                    days = actualSubDays,
                    interpretation = "${mainPlanet.displayName}-${planet.displayName} period"
                )
            )

            currentDate = subEndDate.plusDays(1)
            if (currentDate > endDate) break
        }

        return subPeriods
    }

    private fun getHousesRuledBy(planet: Planet, chart: SolarReturnChart): List<Int> {
        val houses = mutableListOf<Int>()
        val ascendantIndex = chart.ascendant.ordinal

        for (i in 0..11) {
            val signIndex = (ascendantIndex + i) % 12
            val sign = ZodiacSign.entries[signIndex]
            if (sign.ruler == planet) {
                houses.add(i + 1)
            }
        }

        return houses
    }

    private fun generateDashaPrediction(planet: Planet, chart: SolarReturnChart, strength: String): String {
        val position = chart.planetPositions[planet]
        val house = position?.house ?: 1

        val planetNature = when (planet) {
            Planet.SUN -> "vitality, authority, and self-expression"
            Planet.MOON -> "emotions, nurturing, and public connections"
            Planet.MARS -> "energy, initiative, and competitive drive"
            Planet.MERCURY -> "communication, learning, and business"
            Planet.JUPITER -> "wisdom, expansion, and good fortune"
            Planet.VENUS -> "relationships, creativity, and pleasures"
            Planet.SATURN -> "discipline, responsibility, and long-term goals"
            Planet.RAHU -> "ambition, innovation, and unconventional paths"
            Planet.KETU -> "spirituality, detachment, and past karma"
            else -> "general influences"
        }

        val houseArea = getHouseSignificance(house)

        val strengthQuality = when (strength) {
            "Exalted" -> "This period promises exceptional results"
            "Strong" -> "This period is well-supported for success"
            "Debilitated" -> "This period requires extra effort and patience"
            else -> "This period brings mixed but manageable influences"
        }

        return "During this ${planet.displayName} period, focus shifts to $planetNature, particularly affecting $houseArea. $strengthQuality."
    }

    private fun getDashaKeywords(planet: Planet, chart: SolarReturnChart): List<String> {
        val position = chart.planetPositions[planet]
        val house = position?.house ?: 1

        val planetKeywords = when (planet) {
            Planet.SUN -> listOf("Leadership", "Vitality", "Father")
            Planet.MOON -> listOf("Emotions", "Mother", "Public")
            Planet.MARS -> listOf("Action", "Energy", "Courage")
            Planet.MERCURY -> listOf("Communication", "Learning", "Business")
            Planet.JUPITER -> listOf("Wisdom", "Growth", "Fortune")
            Planet.VENUS -> listOf("Love", "Art", "Comfort")
            Planet.SATURN -> listOf("Discipline", "Karma", "Delays")
            Planet.RAHU -> listOf("Ambition", "Innovation", "Foreign")
            Planet.KETU -> listOf("Spirituality", "Detachment", "Past")
            else -> listOf("General")
        }

        val houseKeywords = when (house) {
            1 -> listOf("Self", "Body")
            2 -> listOf("Wealth", "Speech")
            3 -> listOf("Siblings", "Courage")
            4 -> listOf("Home", "Peace")
            5 -> listOf("Children", "Romance")
            6 -> listOf("Health", "Service")
            7 -> listOf("Marriage", "Business")
            8 -> listOf("Transformation", "Research")
            9 -> listOf("Luck", "Travel")
            10 -> listOf("Career", "Status")
            11 -> listOf("Gains", "Friends")
            12 -> listOf("Spirituality", "Losses")
            else -> listOf()
        }

        return (planetKeywords + houseKeywords).take(5)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HOUSE PREDICTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate predictions for all 12 houses
     */
    private fun generateHousePredictions(
        chart: SolarReturnChart,
        muntha: MunthaResult,
        yearLord: Planet
    ): List<HousePrediction> {
        val predictions = mutableListOf<HousePrediction>()
        val ascIndex = chart.ascendant.ordinal

        for (house in 1..12) {
            val signIndex = (ascIndex + house - 1) % 12
            val sign = ZodiacSign.entries[signIndex]
            val houseLord = sign.ruler
            val lordPosition = chart.planetPositions[houseLord]?.house ?: 1

            val planetsInHouse = chart.planetPositions.filter { (_, pos) -> pos.house == house }.keys.toList()

            val strength = calculateHouseStrength(house, houseLord, lordPosition, planetsInHouse, chart, muntha, yearLord)
            val keywords = getHouseKeywords(house)
            val prediction = generateHousePrediction(house, sign, houseLord, lordPosition, planetsInHouse, chart, muntha, yearLord)
            val rating = calculateHouseRating(house, houseLord, lordPosition, planetsInHouse, chart, muntha, yearLord)
            val specificEvents = generateSpecificEvents(house, houseLord, lordPosition, planetsInHouse, chart)

            predictions.add(
                HousePrediction(
                    house = house,
                    signOnCusp = sign,
                    houseLord = houseLord,
                    lordPosition = lordPosition,
                    planetsInHouse = planetsInHouse,
                    strength = strength,
                    keywords = keywords,
                    prediction = prediction,
                    rating = rating,
                    specificEvents = specificEvents
                )
            )
        }

        return predictions
    }

    private fun calculateHouseStrength(
        house: Int,
        lord: Planet,
        lordPosition: Int,
        planetsInHouse: List<Planet>,
        chart: SolarReturnChart,
        muntha: MunthaResult,
        yearLord: Planet
    ): String {
        var score = 0

        val beneficPositions = listOf(1, 2, 4, 5, 7, 9, 10, 11)
        if (lordPosition in beneficPositions) score += 2

        val lordStrength = evaluatePlanetStrengthDescription(lord, chart)
        when (lordStrength) {
            "Exalted" -> score += 3
            "Strong" -> score += 2
            "Angular" -> score += 1
            "Debilitated" -> score -= 2
        }

        val benefics = listOf(Planet.JUPITER, Planet.VENUS, Planet.MOON)
        val malefics = listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU)

        planetsInHouse.forEach { planet ->
            if (planet in benefics) score += 1
            if (planet in malefics) score -= 1
        }

        if (muntha.house == house) score += 2
        if (yearLord == lord) score += 1

        return when {
            score >= 5 -> "Excellent"
            score >= 3 -> "Strong"
            score >= 1 -> "Moderate"
            score >= -1 -> "Weak"
            else -> "Challenged"
        }
    }

    private fun getHouseKeywords(house: Int): List<String> {
        return when (house) {
            1 -> listOf("Self", "Personality", "Health", "Appearance", "New Beginnings")
            2 -> listOf("Wealth", "Family", "Speech", "Values", "Food")
            3 -> listOf("Siblings", "Courage", "Communication", "Short Travel", "Skills")
            4 -> listOf("Home", "Mother", "Property", "Vehicles", "Inner Peace")
            5 -> listOf("Children", "Intelligence", "Romance", "Creativity", "Investments")
            6 -> listOf("Enemies", "Health Issues", "Service", "Debts", "Competition")
            7 -> listOf("Marriage", "Partnership", "Business", "Public Dealings", "Contracts")
            8 -> listOf("Longevity", "Transformation", "Research", "Inheritance", "Hidden Matters")
            9 -> listOf("Fortune", "Father", "Religion", "Higher Education", "Long Travel")
            10 -> listOf("Career", "Status", "Authority", "Government", "Fame")
            11 -> listOf("Gains", "Income", "Friends", "Elder Siblings", "Aspirations")
            12 -> listOf("Losses", "Expenses", "Spirituality", "Foreign Lands", "Liberation")
            else -> listOf("General")
        }
    }

    private fun generateHousePrediction(
        house: Int,
        sign: ZodiacSign,
        lord: Planet,
        lordPosition: Int,
        planetsInHouse: List<Planet>,
        chart: SolarReturnChart,
        muntha: MunthaResult,
        yearLord: Planet
    ): String {
        val houseArea = getHouseSignificance(house)
        val lordStrength = evaluatePlanetStrengthDescription(lord, chart)

        val lordAnalysis = buildString {
            append("The lord ${lord.displayName} in house $lordPosition ")
            append(
                when (lordStrength) {
                    "Exalted" -> "is excellently placed for positive outcomes."
                    "Strong" -> "is well-positioned for success."
                    "Moderate" -> "provides moderate support."
                    "Debilitated" -> "faces challenges requiring attention."
                    else -> "influences results variably."
                }
            )
        }

        val planetaryInfluence = if (planetsInHouse.isNotEmpty()) {
            val benefics = planetsInHouse.filter { it in listOf(Planet.JUPITER, Planet.VENUS, Planet.MOON) }
            val malefics = planetsInHouse.filter { it in listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU) }

            when {
                benefics.isNotEmpty() && malefics.isEmpty() ->
                    " ${benefics.joinToString { it.displayName }} enhance positive outcomes."
                malefics.isNotEmpty() && benefics.isEmpty() ->
                    " ${malefics.joinToString { it.displayName }} may bring challenges."
                benefics.isNotEmpty() && malefics.isNotEmpty() ->
                    " Mixed influences from ${planetsInHouse.joinToString { it.displayName }}."
                else -> ""
            }
        } else {
            " Results depend primarily on the lord's position."
        }

        val specialIndications = buildString {
            if (muntha.house == house) append(" Muntha emphasizes these matters this year.")
            if (yearLord == lord) append(" Year Lord rules this house - significant developments expected.")
        }

        return "House $house in ${sign.displayName} governs $houseArea. $lordAnalysis$planetaryInfluence$specialIndications".trim()
    }

    private fun calculateHouseRating(
        house: Int,
        lord: Planet,
        lordPosition: Int,
        planetsInHouse: List<Planet>,
        chart: SolarReturnChart,
        muntha: MunthaResult,
        yearLord: Planet
    ): Float {
        var rating = 3.0f

        val beneficLordPositions = listOf(1, 2, 4, 5, 7, 9, 10, 11)
        if (lordPosition in beneficLordPositions) rating += 0.5f

        val lordStrength = evaluatePlanetStrengthDescription(lord, chart)
        rating += when (lordStrength) {
            "Exalted" -> 1.0f
            "Strong" -> 0.7f
            "Angular" -> 0.3f
            "Debilitated" -> -0.8f
            else -> 0.0f
        }

        planetsInHouse.forEach { planet ->
            when (planet) {
                Planet.JUPITER -> rating += 0.5f
                Planet.VENUS -> rating += 0.4f
                Planet.MOON -> rating += 0.2f
                Planet.SATURN -> rating -= 0.3f
                Planet.MARS -> rating -= 0.2f
                Planet.RAHU, Planet.KETU -> rating -= 0.2f
                else -> {}
            }
        }

        if (muntha.house == house) rating += 0.5f
        if (yearLord == lord) rating += 0.3f

        return rating.coerceIn(1.0f, 5.0f)
    }

    private fun generateSpecificEvents(
        house: Int,
        lord: Planet,
        lordPosition: Int,
        planetsInHouse: List<Planet>,
        chart: SolarReturnChart
    ): List<String> {
        val events = mutableListOf<String>()
        val lordStrength = evaluatePlanetStrengthDescription(lord, chart)
        val isLordStrong = lordStrength in listOf("Exalted", "Strong")

        when (house) {
            1 -> {
                if (isLordStrong) {
                    events.add("Increased vitality and personal confidence")
                    events.add("Favorable for starting new ventures")
                }
                if (Planet.JUPITER in planetsInHouse) events.add("Spiritual growth and wisdom")
                if (Planet.MARS in planetsInHouse) events.add("Increased energy - watch for accidents")
            }
            2 -> {
                if (isLordStrong) {
                    events.add("Financial gains and wealth accumulation")
                    events.add("Improvement in family relationships")
                }
                if (Planet.VENUS in planetsInHouse) events.add("Acquisition of luxury items")
            }
            5 -> {
                if (isLordStrong) {
                    events.add("Creative success and recognition")
                    events.add("Favorable for children's matters")
                }
                if (Planet.JUPITER in planetsInHouse) events.add("Academic success or childbirth possible")
                if (Planet.VENUS in planetsInHouse) events.add("Romantic happiness")
            }
            7 -> {
                if (isLordStrong) {
                    events.add("Strengthening of partnerships")
                    events.add("Favorable for marriage or business")
                }
                if (Planet.VENUS in planetsInHouse) events.add("Romantic fulfillment")
            }
            10 -> {
                if (isLordStrong) {
                    events.add("Career advancement or promotion")
                    events.add("Recognition from authorities")
                }
                if (Planet.SUN in planetsInHouse) events.add("Government favor or leadership role")
            }
            11 -> {
                if (isLordStrong) {
                    events.add("Fulfillment of desires and wishes")
                    events.add("Gains from multiple sources")
                }
            }
        }

        return events.take(4)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MAJOR THEMES & OVERALL PREDICTION
    // ═══════════════════════════════════════════════════════════════════════════

    private fun identifyMajorThemes(
        chart: SolarReturnChart,
        muntha: MunthaResult,
        yearLord: Planet,
        housePredictions: List<HousePrediction>,
        triPataki: TriPatakiChakra,
        tajikaAspects: List<TajikaAspectResult>
    ): List<String> {
        val themes = mutableListOf<String>()

        // Year Lord theme
        val yearLordHouse = chart.planetPositions[yearLord]?.house ?: 1
        themes.add("Year Lord ${yearLord.displayName} emphasizes ${getHouseSignificance(yearLordHouse)}")

        // Muntha theme
        themes.add("Muntha in House ${muntha.house} focuses on ${muntha.themes.firstOrNull() ?: "personal growth"}")

        // Tri-Pataki theme
        themes.add("Tri-Pataki: ${triPataki.dominantInfluence}")

        // Strong houses
        housePredictions.filter { it.strength in listOf("Excellent", "Strong") }
            .sortedByDescending { it.rating }
            .take(2)
            .forEach { themes.add("Favorable: ${getHouseSignificance(it.house)} (House ${it.house})") }

        // Tajika aspects theme
        val positiveAspects = tajikaAspects.count { it.type.isPositive }
        val totalAspects = tajikaAspects.size
        if (totalAspects > 0) {
            val aspectQuality = if (positiveAspects > totalAspects / 2) "supportive" else "challenging"
            themes.add("Tajika yogas are predominantly $aspectQuality ($positiveAspects/$totalAspects positive)")
        }

        return themes.take(6)
    }

    private fun calculateMonthlyInfluences(
        chart: SolarReturnChart,
        solarReturnTime: LocalDateTime
    ): Pair<List<Int>, List<Int>> {
        val favorableMonths = mutableListOf<Int>()
        val challengingMonths = mutableListOf<Int>()

        val yearLord = chart.ascendant.ruler
        val yearLordHouse = chart.planetPositions[yearLord]?.house ?: 1

        for (monthOffset in 0..11) {
            val month = ((solarReturnTime.monthValue - 1 + monthOffset) % 12) + 1
            val transitHouse = (yearLordHouse + monthOffset) % 12 + 1

            val isFavorable = transitHouse in listOf(1, 2, 4, 5, 7, 9, 10, 11)

            if (isFavorable && favorableMonths.size < 4) {
                favorableMonths.add(month)
            } else if (!isFavorable && challengingMonths.size < 3) {
                challengingMonths.add(month)
            }
        }

        return Pair(favorableMonths, challengingMonths)
    }

    private fun calculateKeyDates(
        chart: SolarReturnChart,
        solarReturnTime: LocalDateTime,
        muddaDasha: List<MuddaDashaPeriod>
    ): List<KeyDate> {
        val keyDates = mutableListOf<KeyDate>()

        // Solar Return date
        keyDates.add(
            KeyDate(
                date = solarReturnTime.toLocalDate(),
                event = "Solar Return",
                type = KeyDateType.IMPORTANT,
                description = "Beginning of the annual horoscope year"
            )
        )

        // Mudda Dasha start dates
        muddaDasha.forEach { period ->
            keyDates.add(
                KeyDate(
                    date = period.startDate,
                    event = "${period.planet.displayName} Dasha Begins",
                    type = if (period.planetStrength in listOf("Exalted", "Strong"))
                        KeyDateType.FAVORABLE else KeyDateType.IMPORTANT,
                    description = "Start of ${period.planet.displayName} period (${period.days} days)"
                )
            )
        }

        return keyDates.sortedBy { it.date }.take(15)
    }

    private fun generateOverallPrediction(
        chart: SolarReturnChart,
        yearLord: Planet,
        muntha: MunthaResult,
        tajikaAspects: List<TajikaAspectResult>,
        housePredictions: List<HousePrediction>
    ): String {
        val yearLordStrength = evaluatePlanetStrengthDescription(yearLord, chart)
        val yearLordHouse = chart.planetPositions[yearLord]?.house ?: 1

        val strongHouses = housePredictions.filter { it.strength in listOf("Excellent", "Strong") }
        val weakHouses = housePredictions.filter { it.strength in listOf("Weak", "Challenged") }

        val positiveAspects = tajikaAspects.count { it.type.isPositive }
        val challengingAspects = tajikaAspects.size - positiveAspects

        val overallTone = when {
            yearLordStrength in listOf("Exalted", "Strong") && strongHouses.size >= 6 -> "excellent"
            yearLordStrength in listOf("Exalted", "Strong") && strongHouses.size >= 4 -> "favorable"
            strongHouses.size > weakHouses.size -> "positive"
            weakHouses.size > strongHouses.size -> "challenging but growth-oriented"
            else -> "balanced"
        }

        val yearLordInfluence = when (yearLord) {
            Planet.SUN -> "Year Lord Sun brings focus on leadership, authority, and self-expression."
            Planet.MOON -> "Year Lord Moon emphasizes emotional wellbeing and public connections."
            Planet.MARS -> "Year Lord Mars energizes initiatives and competitive endeavors."
            Planet.MERCURY -> "Year Lord Mercury enhances communication and business activities."
            Planet.JUPITER -> "Year Lord Jupiter bestows wisdom, expansion, and good fortune."
            Planet.VENUS -> "Year Lord Venus brings harmony to relationships and creativity."
            Planet.SATURN -> "Year Lord Saturn teaches discipline and responsibility."
            else -> "The Year Lord influences various aspects with balanced energy."
        }

        val munthaInfluence = "Muntha in House ${muntha.house} (${muntha.sign.displayName}) " +
                "directs attention to ${muntha.themes.firstOrNull()?.lowercase() ?: "personal development"}."

        return buildString {
            append("This Varshaphala year presents an overall $overallTone outlook. ")
            append(yearLordInfluence)
            append(" ")
            append(munthaInfluence)
            append(" The Tajika aspects show $positiveAspects favorable and $challengingAspects challenging configurations. ")
            append("By understanding these influences, the year's potential can be maximized.")
        }
    }

    private fun calculateYearRating(
        chart: SolarReturnChart,
        yearLord: Planet,
        muntha: MunthaResult,
        tajikaAspects: List<TajikaAspectResult>,
        housePredictions: List<HousePrediction>
    ): Float {
        var rating = 3.0f

        // Year Lord strength
        val yearLordStrength = evaluatePlanetStrengthDescription(yearLord, chart)
        rating += when (yearLordStrength) {
            "Exalted" -> 0.8f
            "Strong" -> 0.5f
            "Angular" -> 0.3f
            "Debilitated" -> -0.5f
            else -> 0.0f
        }

        // Muntha lord strength
        rating += when (muntha.lordStrength) {
            "Exalted", "Strong" -> 0.3f
            "Moderate" -> 0.1f
            "Debilitated" -> -0.3f
            else -> 0.0f
        }

        // Muntha house
        if (muntha.house in listOf(1, 2, 4, 5, 9, 10, 11)) rating += 0.2f

        // Tajika aspects
        val positiveAspects = tajikaAspects.count { it.type.isPositive && it.strength.weight >= 0.6 }
        val negativeAspects = tajikaAspects.count { !it.type.isPositive && it.strength.weight >= 0.6 }
        rating += (positiveAspects * 0.1f - negativeAspects * 0.1f).coerceIn(-0.5f, 0.5f)

        // House predictions average
        val averageHouseRating = housePredictions.map { it.rating }.average().toFloat()
        rating += (averageHouseRating - 3.0f) * 0.3f

        // Benefics in angles
        val beneficsAngular = chart.planetPositions.count { (planet, pos) ->
            planet in listOf(Planet.JUPITER, Planet.VENUS) && pos.house in listOf(1, 4, 7, 10)
        }
        rating += beneficsAngular * 0.15f

        return rating.coerceIn(1.0f, 5.0f)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun calculateJulianDay(dateTime: LocalDateTime): Double {
        var y = dateTime.year
        var m = dateTime.monthValue
        val d = dateTime.dayOfMonth + dateTime.hour / 24.0 + dateTime.minute / 1440.0 + dateTime.second / 86400.0

        if (m <= 2) {
            y -= 1
            m += 12
        }

        val a = y / 100
        val b = 2 - a + a / 4

        return (365.25 * (y + 4716)).toLong() + (30.6001 * (m + 1)).toLong() + d + b - 1524.5
    }

    private fun jdToLocalDateTime(julianDay: Double, timezone: String): LocalDateTime {
        val z = floor(julianDay + 0.5).toLong()
        val f = julianDay + 0.5 - z

        val a = if (z < 2299161) z else {
            val alpha = floor((z - 1867216.25) / 36524.25).toLong()
            z + 1 + alpha - alpha / 4
        }

        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toLong()
        val d = floor(365.25 * c).toLong()
        val e = floor((b - d) / 30.6001).toLong()

        val day = (b - d - floor(30.6001 * e)).toInt()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715

        val totalHours = f * 24.0
        val hour = totalHours.toInt()
        val totalMinutes = (totalHours - hour) * 60.0
        val minute = totalMinutes.toInt()
        val second = ((totalMinutes - minute) * 60.0).toInt()

        val utcDateTime = LocalDateTime.of(year.toInt(), month.toInt(), day, hour, minute, second)
        val utcZoned = ZonedDateTime.of(utcDateTime, ZoneId.of("UTC"))
        val localZoned = utcZoned.withZoneSameInstant(ZoneId.of(timezone))

        return localZoned.toLocalDateTime()
    }

    private fun normalizeAngle360(angle: Double): Double {
        var result = angle % 360.0
        if (result < 0) result += 360.0
        return result
    }

    private fun calculateWholeSignHouse(longitude: Double, ascendantLongitude: Double): Int {
        val ascSign = (ascendantLongitude / 30.0).toInt()
        val planetSign = (longitude / 30.0).toInt()
        val house = ((planetSign - ascSign + 12) % 12) + 1
        return house
    }

    private fun isDayChart(sunLongitude: Double, ascendant: Double): Boolean {
        val sunSign = (sunLongitude / 30.0).toInt()
        val ascSign = (ascendant / 30.0).toInt()
        val houseOfSun = ((sunSign - ascSign + 12) % 12) + 1
        return houseOfSun in listOf(7, 8, 9, 10, 11, 12, 1)
    }

    private fun evaluatePlanetStrengthDescription(planet: Planet, chart: SolarReturnChart): String {
        val position = chart.planetPositions[planet] ?: return "Unknown"
        val sign = position.sign

        return when {
            isExalted(planet, sign) -> "Exalted"
            isDebilitated(planet, sign) -> "Debilitated"
            OWN_SIGNS[planet]?.contains(sign) == true -> "Strong"
            position.house in listOf(1, 4, 7, 10) -> "Angular"
            position.isRetrograde -> "Retrograde"
            else -> "Moderate"
        }
    }

    private fun isExalted(planet: Planet, sign: ZodiacSign): Boolean {
        val exaltationDegree = EXALTATION_DEGREES[planet] ?: return false
        val exaltationSign = ZodiacSign.fromLongitude(exaltationDegree)
        return sign == exaltationSign
    }

    private fun isDebilitated(planet: Planet, sign: ZodiacSign): Boolean {
        return DEBILITATION_SIGNS[planet] == sign
    }

    private fun areFriends(planet1: Planet, planet2: Planet): Boolean {
        return FRIENDSHIPS[planet1]?.contains(planet2) == true
    }

    private fun areNeutral(planet1: Planet, planet2: Planet): Boolean {
        return NEUTRALS[planet1]?.contains(planet2) == true
    }

    private fun getHouseSignificance(house: Int): String {
        return when (house) {
            1 -> "personal development and health"
            2 -> "finances and family"
            3 -> "communication and siblings"
            4 -> "home and property"
            5 -> "creativity and children"
            6 -> "health and service"
            7 -> "partnerships and marriage"
            8 -> "transformation and inheritance"
            9 -> "fortune and higher learning"
            10 -> "career and status"
            11 -> "gains and friendships"
            12 -> "spirituality and foreign matters"
            else -> "various life areas"
        }
    }

    private fun getOrdinalSuffix(n: Int): String {
        return when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
    }

    /**
     * Get house meaning for external use
     */
    fun getHouseMeaning(house: Int): String = getHouseSignificance(house)

    /**
     * Get house keywords for external use
     */
    fun getHouseKeywordsExternal(house: Int): List<String> = getHouseKeywords(house)
}
