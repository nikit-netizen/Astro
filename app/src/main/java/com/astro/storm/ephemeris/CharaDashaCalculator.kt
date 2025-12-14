package com.astro.storm.ephemeris

import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Chara Dasha Calculator (Jaimini System)
 *
 * Chara Dasha is a sign-based dasha system from Jaimini astrology that provides
 * an alternative timing perspective to Vimsottari Dasha. Unlike Vimsottari which
 * is planet-based, Chara Dasha operates on the principle that signs (rashis) rule
 * periods of life.
 *
 * ## Key Principles
 *
 * ### 1. Starting Sign Determination
 * - For odd signs (Aries, Gemini, Leo, Libra, Sagittarius, Aquarius) as Lagna:
 *   Dasha starts from Lagna itself, counting FORWARD
 * - For even signs (Taurus, Cancer, Virgo, Scorpio, Capricorn, Pisces) as Lagna:
 *   Dasha starts from Lagna, counting BACKWARD
 *
 * ### 2. Dasha Period Calculation
 * The period of each sign is determined by the position of its lord:
 * - If lord is in own sign: 12 years
 * - Otherwise: Count from sign to its lord (or from lord to sign for even signs)
 * - Exception: Scorpio and Aquarius have dual lordship (Mars/Ketu, Saturn/Rahu)
 *
 * ### 3. Jaimini Karakas (Significators)
 * - Atmakaraka (AK): Planet with highest longitude - Soul significator
 * - Amatyakaraka (AmK): 2nd highest - Career significator
 * - Bhratrikaraka (BK): 3rd highest - Siblings significator
 * - Matrikaraka (MK): 4th highest - Mother significator
 * - Pitrikaraka (PiK): 5th highest - Father significator
 * - Putrakaraka (PuK): 6th highest - Children significator
 * - Gnatikaraka (GK): 7th highest - Cousins/Competition significator
 * - Darakaraka (DK): Lowest longitude - Spouse significator
 *
 * ### 4. Karakamsha
 * The Navamsa sign where Atmakaraka is placed - crucial for determining soul's purpose
 *
 * ## References
 * - Jaimini Sutras (Chapters 1-2)
 * - K.N. Rao's research on Chara Dasha
 * - Sanjay Rath's "Jaimini Maharishi's Upadesa Sutras"
 * - P.S. Sastri's commentary on Jaimini Sutras
 *
 * @author AstroStorm
 */
object CharaDashaCalculator {

    private val MATH_CONTEXT = MathContext(20, RoundingMode.HALF_EVEN)
    private val DAYS_PER_YEAR = BigDecimal("365.25")

    // ============================================
    // DATA CLASSES
    // ============================================

    /**
     * Complete Chara Dasha analysis result
     */
    data class CharaDashaResult(
        val lagnaSign: ZodiacSign,
        val isOddLagna: Boolean,
        val startingSign: ZodiacSign,
        val countDirection: CountDirection,
        val charaKarakas: CharaKarakas,
        val karakamsha: ZodiacSign,
        val mahadashas: List<CharaMahadasha>,
        val currentMahadasha: CharaMahadasha?,
        val currentAntardasha: CharaAntardasha?,
        val interpretation: CharaDashaInterpretation
    )

    /**
     * Chara Karaka assignments (Jaimini significators)
     */
    data class CharaKarakas(
        val atmakaraka: KarakaInfo,        // Soul - highest degree
        val amatyakaraka: KarakaInfo,      // Career - 2nd highest
        val bhratrikaraka: KarakaInfo,     // Siblings - 3rd highest
        val matrikaraka: KarakaInfo,       // Mother - 4th highest
        val pitrikaraka: KarakaInfo,       // Father - 5th highest
        val putrakaraka: KarakaInfo,       // Children - 6th highest
        val gnatikaraka: KarakaInfo,       // Competition - 7th highest
        val darakaraka: KarakaInfo         // Spouse - lowest degree
    ) {
        fun toList(): List<KarakaInfo> = listOf(
            atmakaraka, amatyakaraka, bhratrikaraka, matrikaraka,
            pitrikaraka, putrakaraka, gnatikaraka, darakaraka
        )
    }

    /**
     * Individual Karaka information
     */
    data class KarakaInfo(
        val karakaType: KarakaType,
        val planet: Planet,
        val longitude: Double,
        val degreeInSign: Double,
        val sign: ZodiacSign,
        val navamsaSign: ZodiacSign,
        val description: String
    )

    /**
     * Chara Mahadasha period
     */
    data class CharaMahadasha(
        val sign: ZodiacSign,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val durationYears: Int,
        val signLord: Planet,
        val antardashas: List<CharaAntardasha>,
        val specialSignificance: String,
        val interpretation: MahadashaInterpretation
    ) {
        val durationDays: Long
            get() = ChronoUnit.DAYS.between(startDate, endDate)

        fun isActiveOn(date: LocalDate): Boolean {
            return !date.isBefore(startDate) && !date.isAfter(endDate)
        }

        val isActive: Boolean
            get() = isActiveOn(LocalDate.now())

        fun getAntardashaOn(date: LocalDate): CharaAntardasha? {
            return antardashas.find { it.isActiveOn(date) }
        }

        fun getProgressPercent(asOf: LocalDate = LocalDate.now()): Double {
            if (durationDays <= 0) return 0.0
            val elapsed = ChronoUnit.DAYS.between(startDate, asOf.coerceIn(startDate, endDate))
            return ((elapsed.toDouble() / durationDays) * 100).coerceIn(0.0, 100.0)
        }

        fun getRemainingDays(asOf: LocalDate = LocalDate.now()): Long {
            if (asOf.isAfter(endDate)) return 0
            if (asOf.isBefore(startDate)) return durationDays
            return ChronoUnit.DAYS.between(asOf, endDate)
        }
    }

    /**
     * Chara Antardasha (sub-period)
     */
    data class CharaAntardasha(
        val sign: ZodiacSign,
        val mahadashaSign: ZodiacSign,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val durationDays: Long,
        val signLord: Planet,
        val interpretation: String
    ) {
        val durationMonths: Double
            get() = durationDays / 30.4375

        fun isActiveOn(date: LocalDate): Boolean {
            return !date.isBefore(startDate) && !date.isAfter(endDate)
        }

        val isActive: Boolean
            get() = isActiveOn(LocalDate.now())

        fun getProgressPercent(asOf: LocalDate = LocalDate.now()): Double {
            if (durationDays <= 0) return 0.0
            val elapsed = ChronoUnit.DAYS.between(startDate, asOf.coerceIn(startDate, endDate))
            return ((elapsed.toDouble() / durationDays) * 100).coerceIn(0.0, 100.0)
        }
    }

    /**
     * Mahadasha interpretation
     */
    data class MahadashaInterpretation(
        val generalEffects: String,
        val signLordEffects: String,
        val karakaActivation: List<String>,
        val favorableAreas: List<String>,
        val challengingAreas: List<String>,
        val recommendations: List<String>
    )

    /**
     * Overall Chara Dasha interpretation
     */
    data class CharaDashaInterpretation(
        val systemOverview: String,
        val karakamshaAnalysis: String,
        val currentPhaseAnalysis: String,
        val futureOutlook: String,
        val generalGuidance: List<String>
    )

    // ============================================
    // ENUMS
    // ============================================

    enum class CountDirection {
        FORWARD,    // For odd Lagnas
        BACKWARD    // For even Lagnas
    }

    enum class KarakaType(val displayName: String, val sankritName: String, val signification: String) {
        ATMAKARAKA("Atmakaraka", "आत्मकारक", "Soul, Self, King of the chart"),
        AMATYAKARAKA("Amatyakaraka", "अमात्यकारक", "Career, Minister, Profession"),
        BHRATRIKARAKA("Bhratrikaraka", "भ्रातृकारक", "Siblings, Courage, Efforts"),
        MATRIKARAKA("Matrikaraka", "मातृकारक", "Mother, Nurturing, Emotions"),
        PITRIKARAKA("Pitrikaraka", "पितृकारक", "Father, Authority, Guidance"),
        PUTRAKARAKA("Putrakaraka", "पुत्रकारक", "Children, Creativity, Intelligence"),
        GNATIKARAKA("Gnatikaraka", "ज्ञातिकारक", "Cousins, Competition, Conflicts"),
        DARAKARAKA("Darakaraka", "दारकारक", "Spouse, Partnership, Marriage")
    }

    // ============================================
    // MAIN CALCULATION METHODS
    // ============================================

    /**
     * Calculate complete Chara Dasha from a Vedic chart
     *
     * @param chart The VedicChart to analyze
     * @param numberOfCycles Number of complete 12-sign cycles to calculate
     * @return Complete CharaDashaResult
     */
    fun calculateCharaDasha(
        chart: VedicChart,
        numberOfCycles: Int = 2
    ): CharaDashaResult {
        val birthDate = chart.birthData.dateTime.toLocalDate()

        // Determine Lagna characteristics
        val lagnaSign = ZodiacSign.fromLongitude(chart.ascendant)
        val isOddLagna = isOddSign(lagnaSign)
        val countDirection = if (isOddLagna) CountDirection.FORWARD else CountDirection.BACKWARD

        // Calculate Chara Karakas
        val charaKarakas = calculateCharaKarakas(chart)

        // Calculate Karakamsha (Navamsa sign of Atmakaraka)
        val karakamsha = calculateKarakamsha(charaKarakas.atmakaraka, chart)

        // Calculate all Mahadashas
        val mahadashas = calculateMahadashas(
            lagnaSign, isOddLagna, countDirection, birthDate, numberOfCycles, chart
        )

        // Find current periods
        val today = LocalDate.now()
        val currentMahadasha = mahadashas.find { it.isActiveOn(today) }
        val currentAntardasha = currentMahadasha?.getAntardashaOn(today)

        // Generate interpretation
        val interpretation = generateCharaDashaInterpretation(
            lagnaSign, charaKarakas, karakamsha, currentMahadasha, chart
        )

        return CharaDashaResult(
            lagnaSign = lagnaSign,
            isOddLagna = isOddLagna,
            startingSign = lagnaSign,
            countDirection = countDirection,
            charaKarakas = charaKarakas,
            karakamsha = karakamsha,
            mahadashas = mahadashas,
            currentMahadasha = currentMahadasha,
            currentAntardasha = currentAntardasha,
            interpretation = interpretation
        )
    }

    /**
     * Calculate Chara Karakas (Jaimini significators)
     *
     * The Karakas are determined by the degree of planets within their signs.
     * The planet with the highest degree (0-30) becomes Atmakaraka,
     * and so on in descending order.
     */
    fun calculateCharaKarakas(chart: VedicChart): CharaKarakas {
        // Get only the 7 classical planets (Rahu is excluded in traditional Jaimini)
        // Some schools include Rahu - we'll use the 8-planet system
        val karakaPlanets = chart.planetPositions
            .filter { it.planet in listOf(
                Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
                Planet.JUPITER, Planet.VENUS, Planet.SATURN, Planet.RAHU
            )}
            .map { pos ->
                val degreeInSign = pos.longitude % 30.0
                Triple(pos.planet, degreeInSign, pos)
            }
            .sortedByDescending { it.second }

        // Assign Karakas based on descending degree
        val karakaTypes = KarakaType.entries

        val karakaInfoList = karakaPlanets.mapIndexed { index, (planet, degree, pos) ->
            if (index < karakaTypes.size) {
                val karakaType = karakaTypes[index]
                val navamsaSign = calculateNavamsaSign(pos.longitude)
                KarakaInfo(
                    karakaType = karakaType,
                    planet = planet,
                    longitude = pos.longitude,
                    degreeInSign = degree,
                    sign = pos.sign,
                    navamsaSign = navamsaSign,
                    description = "${karakaType.displayName}: ${planet.displayName} at ${String.format("%.2f", degree)}° in ${pos.sign.displayName}"
                )
            } else null
        }.filterNotNull()

        // If we have fewer than 8 planets, fill with defaults
        val filledKarakas = karakaInfoList.toMutableList()
        while (filledKarakas.size < 8) {
            val missingType = karakaTypes[filledKarakas.size]
            filledKarakas.add(
                KarakaInfo(
                    karakaType = missingType,
                    planet = Planet.SUN,
                    longitude = 0.0,
                    degreeInSign = 0.0,
                    sign = ZodiacSign.ARIES,
                    navamsaSign = ZodiacSign.ARIES,
                    description = "${missingType.displayName}: Not determined"
                )
            )
        }

        return CharaKarakas(
            atmakaraka = filledKarakas[0],
            amatyakaraka = filledKarakas[1],
            bhratrikaraka = filledKarakas[2],
            matrikaraka = filledKarakas[3],
            pitrikaraka = filledKarakas[4],
            putrakaraka = filledKarakas[5],
            gnatikaraka = filledKarakas[6],
            darakaraka = filledKarakas[7]
        )
    }

    /**
     * Calculate Karakamsha - the Navamsa sign of Atmakaraka
     */
    private fun calculateKarakamsha(atmakaraka: KarakaInfo, chart: VedicChart): ZodiacSign {
        return atmakaraka.navamsaSign
    }

    /**
     * Calculate Navamsa sign from longitude
     */
    private fun calculateNavamsaSign(longitude: Double): ZodiacSign {
        val normalizedLong = ((longitude % 360.0) + 360.0) % 360.0
        val navamsaIndex = ((normalizedLong / 3.333333333333333) % 12).toInt()
        return ZodiacSign.entries[navamsaIndex]
    }

    /**
     * Calculate all Mahadashas
     */
    private fun calculateMahadashas(
        lagnaSign: ZodiacSign,
        isOddLagna: Boolean,
        countDirection: CountDirection,
        birthDate: LocalDate,
        numberOfCycles: Int,
        chart: VedicChart
    ): List<CharaMahadasha> {
        val mahadashas = mutableListOf<CharaMahadasha>()
        var currentDate = birthDate

        // Generate sign sequence based on count direction
        val signSequence = generateSignSequence(lagnaSign, countDirection, numberOfCycles * 12)

        for (sign in signSequence) {
            val durationYears = calculateSignDashaPeriod(sign, chart)
            val durationDays = yearsToRoundedDays(durationYears.toDouble())
            val endDate = currentDate.plusDays(durationDays)

            val antardashas = calculateAntardashas(
                sign, countDirection, currentDate, endDate, chart
            )

            val signLord = getSignLord(sign)
            val specialSignificance = getSpecialSignificance(sign, chart)
            val interpretation = generateMahadashaInterpretation(sign, signLord, chart)

            mahadashas.add(
                CharaMahadasha(
                    sign = sign,
                    startDate = currentDate,
                    endDate = endDate,
                    durationYears = durationYears,
                    signLord = signLord,
                    antardashas = antardashas,
                    specialSignificance = specialSignificance,
                    interpretation = interpretation
                )
            )

            currentDate = endDate.plusDays(1)
        }

        return mahadashas
    }

    /**
     * Generate sign sequence based on Lagna type and direction
     */
    private fun generateSignSequence(
        startSign: ZodiacSign,
        direction: CountDirection,
        count: Int
    ): List<ZodiacSign> {
        val sequence = mutableListOf<ZodiacSign>()
        var currentIndex = startSign.ordinal

        repeat(count) {
            sequence.add(ZodiacSign.entries[currentIndex])
            currentIndex = when (direction) {
                CountDirection.FORWARD -> (currentIndex + 1) % 12
                CountDirection.BACKWARD -> (currentIndex - 1 + 12) % 12
            }
        }

        return sequence
    }

    /**
     * Calculate Dasha period for a sign
     *
     * The period is determined by counting from the sign to its lord
     * (or vice versa for even signs), with special rules:
     * - Lord in own sign: 12 years
     * - Otherwise: Count houses from sign to lord (adjusting for even/odd)
     * - Maximum: 12 years, Minimum: 1 year
     */
    private fun calculateSignDashaPeriod(sign: ZodiacSign, chart: VedicChart): Int {
        val signLord = getSignLord(sign)
        val lordPosition = chart.planetPositions.find { it.planet == signLord }
            ?: return 12 // Default if lord not found

        val lordSign = lordPosition.sign

        // If lord is in its own sign
        if (lordSign == sign) {
            return 12
        }

        // Count from sign to lord sign
        val signIndex = sign.number - 1
        val lordSignIndex = lordSign.number - 1

        // For odd signs, count forward; for even signs, count backward
        val period = if (isOddSign(sign)) {
            // Forward count
            ((lordSignIndex - signIndex + 12) % 12)
        } else {
            // Backward count
            ((signIndex - lordSignIndex + 12) % 12)
        }

        // Period should be at least 1 year and at most 12
        return period.coerceIn(1, 12)
    }

    /**
     * Calculate Antardashas within a Mahadasha
     */
    private fun calculateAntardashas(
        mahadashaSign: ZodiacSign,
        direction: CountDirection,
        mahaStart: LocalDate,
        mahaEnd: LocalDate,
        chart: VedicChart
    ): List<CharaAntardasha> {
        val antardashas = mutableListOf<CharaAntardasha>()
        val mahaDurationDays = ChronoUnit.DAYS.between(mahaStart, mahaEnd)

        if (mahaDurationDays <= 0) return antardashas

        // Generate antardasha sign sequence
        val antarSignSequence = generateSignSequence(mahadashaSign, direction, 12)

        // Calculate total years for proportional division
        val totalYears = antarSignSequence.sumOf { calculateSignDashaPeriod(it, chart) }

        var currentDate = mahaStart

        for (antarSign in antarSignSequence) {
            val antarYears = calculateSignDashaPeriod(antarSign, chart)
            val proportion = antarYears.toDouble() / totalYears
            val antarDurationDays = (mahaDurationDays * proportion).toLong().coerceAtLeast(1L)

            val endDate = if (antardashas.size == 11) {
                mahaEnd // Last antardasha ends at mahadasha end
            } else {
                currentDate.plusDays(antarDurationDays).let {
                    if (it.isAfter(mahaEnd)) mahaEnd else it
                }
            }

            val interpretation = generateAntardashaInterpretation(
                mahadashaSign, antarSign, chart
            )

            antardashas.add(
                CharaAntardasha(
                    sign = antarSign,
                    mahadashaSign = mahadashaSign,
                    startDate = currentDate,
                    endDate = endDate,
                    durationDays = ChronoUnit.DAYS.between(currentDate, endDate).coerceAtLeast(1L),
                    signLord = getSignLord(antarSign),
                    interpretation = interpretation
                )
            )

            if (endDate >= mahaEnd) break
            currentDate = endDate.plusDays(1)
        }

        return antardashas
    }

    // ============================================
    // INTERPRETATION METHODS
    // ============================================

    private fun generateMahadashaInterpretation(
        sign: ZodiacSign,
        signLord: Planet,
        chart: VedicChart
    ): MahadashaInterpretation {
        val generalEffects = getSignGeneralEffects(sign)
        val signLordEffects = getSignLordEffects(signLord, sign, chart)
        val karakaActivation = getKarakaActivation(sign, chart)
        val favorableAreas = getFavorableAreas(sign)
        val challengingAreas = getChallengingAreas(sign)
        val recommendations = getRecommendations(sign, signLord)

        return MahadashaInterpretation(
            generalEffects = generalEffects,
            signLordEffects = signLordEffects,
            karakaActivation = karakaActivation,
            favorableAreas = favorableAreas,
            challengingAreas = challengingAreas,
            recommendations = recommendations
        )
    }

    private fun generateAntardashaInterpretation(
        mahaSign: ZodiacSign,
        antarSign: ZodiacSign,
        chart: VedicChart
    ): String {
        val houseFrom = getHouseFromSign(antarSign, mahaSign)
        val antarLord = getSignLord(antarSign)

        return buildString {
            append("${mahaSign.displayName}-${antarSign.displayName}: ")
            append("Antardasha of ${antarSign.displayName} ($houseFrom house from Mahadasha). ")
            append("Lord ${antarLord.displayName} activates ${antarSign.displayName} themes. ")
            when (houseFrom) {
                1 -> append("Self-focused period, personal initiatives.")
                2 -> append("Focus on wealth, family, speech.")
                3 -> append("Courage, siblings, communication active.")
                4 -> append("Home, mother, property matters.")
                5 -> append("Children, creativity, romance highlighted.")
                6 -> append("Health, competition, service matters.")
                7 -> append("Partnerships, marriage, public dealings.")
                8 -> append("Transformation, inheritance, research.")
                9 -> append("Fortune, higher learning, spirituality.")
                10 -> append("Career, status, public recognition.")
                11 -> append("Gains, friendships, aspirations fulfilled.")
                12 -> append("Expenses, spirituality, foreign connections.")
            }
        }
    }

    private fun generateCharaDashaInterpretation(
        lagnaSign: ZodiacSign,
        karakas: CharaKarakas,
        karakamsha: ZodiacSign,
        currentMaha: CharaMahadasha?,
        chart: VedicChart
    ): CharaDashaInterpretation {
        val systemOverview = buildString {
            append("Chara Dasha for ${lagnaSign.displayName} Lagna operates in ")
            append(if (isOddSign(lagnaSign)) "FORWARD" else "BACKWARD")
            append(" direction. Your Atmakaraka is ${karakas.atmakaraka.planet.displayName}, ")
            append("indicating your soul's primary lesson involves ${karakas.atmakaraka.karakaType.signification}.")
        }

        val karakamshaAnalysis = buildString {
            append("Karakamsha in ${karakamsha.displayName}: ")
            append(getKarakamshaInterpretation(karakamsha))
        }

        val currentPhaseAnalysis = if (currentMaha != null) {
            buildString {
                append("Currently in ${currentMaha.sign.displayName} Mahadasha ")
                append("(${currentMaha.durationYears} years, ")
                append("${String.format("%.1f", currentMaha.getProgressPercent())}% complete). ")
                append(currentMaha.interpretation.generalEffects)
            }
        } else {
            "Dasha calculation extends beyond current date."
        }

        val futureOutlook = "Monitor transits of sign lords for timing events within each dasha period."

        val generalGuidance = listOf(
            "Strengthen your Atmakaraka (${karakas.atmakaraka.planet.displayName}) through appropriate remedies",
            "Pay attention when Mahadasha sign is activated by major transits",
            "Use Chara Dasha alongside Vimsottari for comprehensive timing analysis",
            "Karakamsha house from Moon and Lagna provides additional insights"
        )

        return CharaDashaInterpretation(
            systemOverview = systemOverview,
            karakamshaAnalysis = karakamshaAnalysis,
            currentPhaseAnalysis = currentPhaseAnalysis,
            futureOutlook = futureOutlook,
            generalGuidance = generalGuidance
        )
    }

    // ============================================
    // HELPER METHODS
    // ============================================

    /**
     * Check if a sign is odd (masculine)
     */
    private fun isOddSign(sign: ZodiacSign): Boolean {
        return sign in listOf(
            ZodiacSign.ARIES, ZodiacSign.GEMINI, ZodiacSign.LEO,
            ZodiacSign.LIBRA, ZodiacSign.SAGITTARIUS, ZodiacSign.AQUARIUS
        )
    }

    /**
     * Get the lord of a sign (considering dual lordship for Scorpio and Aquarius)
     */
    private fun getSignLord(sign: ZodiacSign): Planet {
        return sign.ruler
    }

    /**
     * Convert years to days
     */
    private fun yearsToRoundedDays(years: Double): Long {
        return BigDecimal(years.toString())
            .multiply(DAYS_PER_YEAR, MATH_CONTEXT)
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()
            .coerceAtLeast(1L)
    }

    /**
     * Get house position from base sign
     */
    private fun getHouseFromSign(targetSign: ZodiacSign, baseSign: ZodiacSign): Int {
        val diff = targetSign.number - baseSign.number
        return if (diff >= 0) diff + 1 else diff + 13
    }

    /**
     * Get general effects for a sign dasha
     */
    private fun getSignGeneralEffects(sign: ZodiacSign): String {
        return when (sign) {
            ZodiacSign.ARIES -> "Period of initiative, leadership, and new beginnings. Mars-ruled energy brings action and courage."
            ZodiacSign.TAURUS -> "Focus on wealth, stability, and material comforts. Venus brings beauty and relationships."
            ZodiacSign.GEMINI -> "Communication, learning, and intellectual pursuits highlighted. Mercury brings versatility."
            ZodiacSign.CANCER -> "Emotional growth, home, and mother-related matters. Moon brings nurturing energy."
            ZodiacSign.LEO -> "Recognition, authority, and creative expression. Sun brings confidence and leadership."
            ZodiacSign.VIRGO -> "Analysis, health, and service matters. Mercury brings discrimination and healing."
            ZodiacSign.LIBRA -> "Partnerships, balance, and justice. Venus brings harmony and relationships."
            ZodiacSign.SCORPIO -> "Transformation, research, and hidden matters. Mars/Ketu bring intensity."
            ZodiacSign.SAGITTARIUS -> "Higher learning, philosophy, and expansion. Jupiter brings wisdom."
            ZodiacSign.CAPRICORN -> "Career, discipline, and long-term goals. Saturn brings structure."
            ZodiacSign.AQUARIUS -> "Innovation, groups, and humanitarian concerns. Saturn/Rahu bring change."
            ZodiacSign.PISCES -> "Spirituality, imagination, and liberation. Jupiter brings transcendence."
        }
    }

    /**
     * Get sign lord effects
     */
    private fun getSignLordEffects(lord: Planet, sign: ZodiacSign, chart: VedicChart): String {
        val lordPosition = chart.planetPositions.find { it.planet == lord }
        return if (lordPosition != null) {
            "Sign lord ${lord.displayName} is in ${lordPosition.sign.displayName} (House ${lordPosition.house}), " +
            "influencing this dasha through those house themes."
        } else {
            "Sign lord ${lord.displayName} governs this period's outcomes."
        }
    }

    /**
     * Get Karaka activation for a sign
     */
    private fun getKarakaActivation(sign: ZodiacSign, chart: VedicChart): List<String> {
        val activations = mutableListOf<String>()
        val karakas = calculateCharaKarakas(chart)

        karakas.toList().forEach { karaka ->
            if (karaka.sign == sign) {
                activations.add("${karaka.karakaType.displayName} (${karaka.planet.displayName}) is activated - ${karaka.karakaType.signification}")
            }
        }

        return activations.ifEmpty { listOf("No major Karaka directly in this sign") }
    }

    /**
     * Get favorable areas for a sign dasha
     */
    private fun getFavorableAreas(sign: ZodiacSign): List<String> {
        return when (sign.element) {
            "Fire" -> listOf("Leadership", "Initiative", "Sports", "Entrepreneurship")
            "Earth" -> listOf("Finance", "Property", "Career", "Practical matters")
            "Air" -> listOf("Communication", "Learning", "Social activities", "Travel")
            "Water" -> listOf("Emotions", "Creativity", "Spirituality", "Intuition")
            else -> listOf("General growth")
        }
    }

    /**
     * Get challenging areas for a sign dasha
     */
    private fun getChallengingAreas(sign: ZodiacSign): List<String> {
        return when (sign) {
            ZodiacSign.ARIES -> listOf("Patience", "Diplomacy", "Details")
            ZodiacSign.TAURUS -> listOf("Flexibility", "Change", "Risk-taking")
            ZodiacSign.GEMINI -> listOf("Focus", "Commitment", "Depth")
            ZodiacSign.CANCER -> listOf("Objectivity", "Independence", "Tough decisions")
            ZodiacSign.LEO -> listOf("Humility", "Teamwork", "Accepting criticism")
            ZodiacSign.VIRGO -> listOf("Perfectionism", "Over-analysis", "Relaxation")
            ZodiacSign.LIBRA -> listOf("Decision-making", "Independence", "Confrontation")
            ZodiacSign.SCORPIO -> listOf("Trust", "Letting go", "Forgiveness")
            ZodiacSign.SAGITTARIUS -> listOf("Details", "Patience", "Routine")
            ZodiacSign.CAPRICORN -> listOf("Emotions", "Spontaneity", "Fun")
            ZodiacSign.AQUARIUS -> listOf("Tradition", "One-on-one connection", "Practicality")
            ZodiacSign.PISCES -> listOf("Boundaries", "Practicality", "Discipline")
        }
    }

    /**
     * Get recommendations for a sign dasha
     */
    private fun getRecommendations(sign: ZodiacSign, lord: Planet): List<String> {
        val recommendations = mutableListOf<String>()

        recommendations.add("Strengthen ${lord.displayName} through appropriate mantras and gemstones")
        recommendations.add("Honor the deity associated with ${sign.displayName}")

        when (sign.element) {
            "Fire" -> recommendations.add("Channel energy through physical activity and creative expression")
            "Earth" -> recommendations.add("Focus on building stable foundations and practical achievements")
            "Air" -> recommendations.add("Engage in learning, communication, and social connections")
            "Water" -> recommendations.add("Honor emotions through meditation and artistic expression")
        }

        return recommendations
    }

    /**
     * Get special significance of a sign in the chart
     */
    private fun getSpecialSignificance(sign: ZodiacSign, chart: VedicChart): String {
        val planetsInSign = chart.planetPositions.filter { it.sign == sign }
        return if (planetsInSign.isNotEmpty()) {
            "Contains: ${planetsInSign.joinToString(", ") { it.planet.displayName }}"
        } else {
            "No planets in natal chart"
        }
    }

    /**
     * Get Karakamsha interpretation
     */
    private fun getKarakamshaInterpretation(karakamsha: ZodiacSign): String {
        return when (karakamsha) {
            ZodiacSign.ARIES -> "Soul's purpose involves leadership, initiative, and pioneering activities."
            ZodiacSign.TAURUS -> "Soul seeks stability, beauty, and material security."
            ZodiacSign.GEMINI -> "Soul's evolution through communication, learning, and versatility."
            ZodiacSign.CANCER -> "Soul's path involves nurturing, emotions, and homeland."
            ZodiacSign.LEO -> "Soul seeks recognition, authority, and creative expression."
            ZodiacSign.VIRGO -> "Soul evolves through service, analysis, and healing."
            ZodiacSign.LIBRA -> "Soul's purpose in balance, relationships, and justice."
            ZodiacSign.SCORPIO -> "Deep transformation and uncovering hidden truths is the soul's journey."
            ZodiacSign.SAGITTARIUS -> "Soul seeks higher knowledge, philosophy, and spiritual expansion."
            ZodiacSign.CAPRICORN -> "Soul's path through discipline, achievement, and social responsibility."
            ZodiacSign.AQUARIUS -> "Soul evolves through innovation, humanitarian work, and group consciousness."
            ZodiacSign.PISCES -> "Soul seeks liberation, spirituality, and divine connection."
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get current period summary
     */
    fun getCurrentPeriodSummary(result: CharaDashaResult): String {
        val maha = result.currentMahadasha ?: return "No active Chara Dasha period"
        val antar = result.currentAntardasha

        return buildString {
            appendLine("=== CHARA DASHA - CURRENT PERIOD ===")
            appendLine()
            appendLine("Mahadasha: ${maha.sign.displayName} (${maha.durationYears} years)")
            appendLine("Lord: ${maha.signLord.displayName}")
            appendLine("Progress: ${String.format("%.1f", maha.getProgressPercent())}%")
            appendLine("Remaining: ${maha.getRemainingDays()} days")
            appendLine()
            if (antar != null) {
                appendLine("Antardasha: ${antar.sign.displayName}")
                appendLine("Progress: ${String.format("%.1f", antar.getProgressPercent())}%")
            }
            appendLine()
            appendLine("Atmakaraka: ${result.charaKarakas.atmakaraka.planet.displayName}")
            appendLine("Karakamsha: ${result.karakamsha.displayName}")
        }
    }

    /**
     * Get Karaka summary
     */
    fun getKarakaSummary(result: CharaDashaResult): String {
        return buildString {
            appendLine("=== CHARA KARAKAS (Jaimini Significators) ===")
            appendLine()
            result.charaKarakas.toList().forEach { karaka ->
                appendLine("${karaka.karakaType.displayName.padEnd(15)}: ${karaka.planet.displayName.padEnd(10)} (${String.format("%.2f", karaka.degreeInSign)}° in ${karaka.sign.displayName})")
            }
        }
    }
}
