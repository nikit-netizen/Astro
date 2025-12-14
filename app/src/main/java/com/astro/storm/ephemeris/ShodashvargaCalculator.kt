package com.astro.storm.ephemeris

import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.PlanetPosition
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign

/**
 * Shodashvarga (16 Divisional Charts) Strength Calculator
 *
 * Shodashvarga Bala (16-fold divisional chart strength) is a comprehensive system
 * for evaluating planetary strength based on their positions across 16 divisional charts.
 *
 * ## The 16 Vargas (Divisional Charts)
 *
 * | Varga | Division | Sanskrit | Domain |
 * |-------|----------|----------|--------|
 * | D-1   | Rashi    | राशि     | General life, body |
 * | D-2   | Hora     | होरा     | Wealth |
 * | D-3   | Drekkana | द्रेष्काण | Siblings, courage |
 * | D-4   | Chaturthamsha | चतुर्थांश | Fortune, property |
 * | D-7   | Saptamsha | सप्तांश | Children, progeny |
 * | D-9   | Navamsa  | नवांश   | Marriage, dharma |
 * | D-10  | Dashamsha | दशांश  | Career, profession |
 * | D-12  | Dwadashamsha | द्वादशांश | Parents |
 * | D-16  | Shodashamsha | षोडशांश | Vehicles, comforts |
 * | D-20  | Vimshamsha | विंशांश | Spiritual progress |
 * | D-24  | Chaturvimshamsha | चतुर्विंशांश | Education, learning |
 * | D-27  | Saptavimshamsha | सप्तविंशांश | Strength, vitality |
 * | D-30  | Trimshamsha | त्रिंशांश | Evils, misfortunes |
 * | D-40  | Khavedamsha | खवेदांश | Auspicious effects |
 * | D-45  | Akshavedamsha | अक्षवेदांश | General indications |
 * | D-60  | Shashtiamsha | षष्ट्यंश | Past life karma |
 *
 * ## Vargottama
 * A planet in the same sign in D-1 and a divisional chart gains additional strength.
 * D-9 Vargottama is particularly significant.
 *
 * ## Vimsopaka Bala (20-Point Strength)
 * Different texts prescribe different weightages for the 16 Vargas.
 * We implement the commonly used Parashari system.
 *
 * ## References
 * - Brihat Parashara Hora Shastra (BPHS) Chapters 7-8
 * - Hora Ratna on Shodashvarga analysis
 * - Phaladeepika
 *
 * @author AstroStorm
 */
object ShodashvargaCalculator {

    // ============================================
    // DATA CLASSES
    // ============================================

    /**
     * Complete Shodashvarga analysis for a chart
     */
    data class ShodashvargaAnalysis(
        val planetStrengths: Map<Planet, PlanetShodashvargaStrength>,
        val vargaCharts: Map<VargaType, VargaChart>,
        val vargottamaPlanets: List<VargottamaInfo>,
        val overallAssessment: OverallVargaAssessment
    )

    /**
     * Shodashvarga strength for a single planet
     */
    data class PlanetShodashvargaStrength(
        val planet: Planet,
        val vargaPositions: Map<VargaType, VargaPosition>,
        val shadvargaBala: Double,         // 6-varga strength (max 6)
        val saptavargaBala: Double,        // 7-varga strength (max 7)
        val dashavargaBala: Double,        // 10-varga strength (max 10)
        val shodashvargaBala: Double,      // 16-varga strength (max 16)
        val vimsopakaBalaPoorva: Double,   // 20-point strength (Poorva scheme)
        val vimsopakaBalaMadhya: Double,   // 20-point strength (Madhya scheme)
        val vimsopakaBalaPara: Double,     // 20-point strength (Para scheme)
        val strengthGrade: StrengthGrade,
        val vargottamaCount: Int,
        val interpretation: String
    )

    /**
     * Position in a specific Varga chart
     */
    data class VargaPosition(
        val vargaType: VargaType,
        val sign: ZodiacSign,
        val dignity: VargaDignity,
        val dignityPoints: Double,
        val isVargottama: Boolean
    )

    /**
     * Varga chart data
     */
    data class VargaChart(
        val vargaType: VargaType,
        val planetPositions: Map<Planet, ZodiacSign>,
        val ascendantSign: ZodiacSign
    )

    /**
     * Vargottama information
     */
    data class VargottamaInfo(
        val planet: Planet,
        val sign: ZodiacSign,
        val vargaType: VargaType,
        val significance: String
    )

    /**
     * Overall Varga assessment
     */
    data class OverallVargaAssessment(
        val strongestPlanet: Planet?,
        val weakestPlanet: Planet?,
        val averageStrength: Double,
        val strengthDistribution: Map<Planet, StrengthGrade>,
        val keyInsights: List<String>,
        val recommendations: List<String>
    )

    // ============================================
    // ENUMS
    // ============================================

    /**
     * The 16 Varga types with their division factors and domains
     */
    enum class VargaType(
        val division: Int,
        val sanskritName: String,
        val domain: String,
        val poorvaWeight: Double,
        val madhyaWeight: Double,
        val paraWeight: Double
    ) {
        D1_RASHI(1, "राशि", "General life", 3.5, 3.5, 3.5),
        D2_HORA(2, "होरा", "Wealth", 1.0, 0.5, 0.5),
        D3_DREKKANA(3, "द्रेष्काण", "Siblings", 1.0, 1.0, 1.0),
        D4_CHATURTHAMSHA(4, "चतुर्थांश", "Fortune", 0.5, 0.5, 0.5),
        D7_SAPTAMSHA(7, "सप्तांश", "Children", 0.5, 1.0, 0.5),
        D9_NAVAMSA(9, "नवांश", "Marriage/Dharma", 3.0, 3.0, 4.5),
        D10_DASHAMSHA(10, "दशांश", "Career", 0.5, 1.0, 0.5),
        D12_DWADASHAMSHA(12, "द्वादशांश", "Parents", 0.5, 0.5, 0.5),
        D16_SHODASHAMSHA(16, "षोडशांश", "Vehicles", 2.0, 1.5, 2.0),
        D20_VIMSHAMSHA(20, "विंशांश", "Spiritual", 0.5, 1.0, 0.5),
        D24_CHATURVIMSHAMSHA(24, "चतुर्विंशांश", "Education", 0.5, 1.0, 0.5),
        D27_SAPTAVIMSHAMSHA(27, "सप्तविंशांश", "Strength", 0.5, 0.5, 1.0),
        D30_TRIMSHAMSHA(30, "त्रिंशांश", "Evils", 1.0, 1.0, 1.0),
        D40_KHAVEDAMSHA(40, "खवेदांश", "Auspicious", 0.5, 1.0, 0.5),
        D45_AKSHAVEDAMSHA(45, "अक्षवेदांश", "General", 0.5, 0.5, 0.5),
        D60_SHASHTIAMSHA(60, "षष्ट्यंश", "Past Karma", 4.0, 2.0, 4.0)
    }

    /**
     * Dignity in a Varga chart
     */
    enum class VargaDignity(val points: Double) {
        EXALTED(4.0),
        MOOLATRIKONA(3.5),
        OWN_SIGN(3.0),
        GREAT_FRIEND(2.5),
        FRIEND(2.0),
        NEUTRAL(1.5),
        ENEMY(1.0),
        GREAT_ENEMY(0.5),
        DEBILITATED(0.0)
    }

    /**
     * Overall strength grade
     */
    enum class StrengthGrade(val minPoints: Double, val description: String) {
        EXCELLENT(15.0, "Excellent - Planet is very strong"),
        GOOD(12.0, "Good - Planet has adequate strength"),
        AVERAGE(9.0, "Average - Planet has moderate strength"),
        WEAK(6.0, "Weak - Planet needs strengthening"),
        VERY_WEAK(0.0, "Very Weak - Planet requires remedies")
    }

    // ============================================
    // MAIN CALCULATION METHODS
    // ============================================

    /**
     * Perform complete Shodashvarga analysis
     */
    fun analyzeShodashvarga(chart: VedicChart): ShodashvargaAnalysis {
        // Calculate all 16 Varga charts
        val vargaCharts = calculateAllVargaCharts(chart)

        // Calculate strength for each planet
        val planetStrengths = Planet.entries
            .filter { it in listOf(
                Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
                Planet.JUPITER, Planet.VENUS, Planet.SATURN
            )}
            .associateWith { planet ->
                calculatePlanetShodashvargaStrength(planet, chart, vargaCharts)
            }

        // Find Vargottama planets
        val vargottamaPlanets = findVargottamaPlanets(chart, vargaCharts)

        // Generate overall assessment
        val overallAssessment = generateOverallAssessment(planetStrengths, vargottamaPlanets)

        return ShodashvargaAnalysis(
            planetStrengths = planetStrengths,
            vargaCharts = vargaCharts,
            vargottamaPlanets = vargottamaPlanets,
            overallAssessment = overallAssessment
        )
    }

    /**
     * Calculate all 16 Varga charts
     */
    private fun calculateAllVargaCharts(chart: VedicChart): Map<VargaType, VargaChart> {
        return VargaType.entries.associateWith { vargaType ->
            val planetPositions = chart.planetPositions
                .filter { it.planet in listOf(
                    Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
                    Planet.JUPITER, Planet.VENUS, Planet.SATURN
                )}
                .associate { pos ->
                    pos.planet to calculateVargaSign(pos.longitude, vargaType)
                }

            val ascendantSign = calculateVargaSign(chart.ascendant, vargaType)

            VargaChart(
                vargaType = vargaType,
                planetPositions = planetPositions,
                ascendantSign = ascendantSign
            )
        }
    }

    /**
     * Calculate sign in a specific Varga chart
     */
    private fun calculateVargaSign(longitude: Double, vargaType: VargaType): ZodiacSign {
        val normalizedLong = ((longitude % 360.0) + 360.0) % 360.0
        val signIndex = (normalizedLong / 30.0).toInt()
        val degreeInSign = normalizedLong % 30.0

        return when (vargaType) {
            VargaType.D1_RASHI -> ZodiacSign.entries[signIndex]

            VargaType.D2_HORA -> {
                // Hora: Each sign divided into 2 parts (15° each)
                // Odd signs: First 15° = Sun, Second 15° = Moon (Leo/Cancer)
                // Even signs: First 15° = Moon, Second 15° = Sun
                val isOddSign = signIndex % 2 == 0 // 0-indexed
                val isFirstHalf = degreeInSign < 15.0
                when {
                    isOddSign && isFirstHalf -> ZodiacSign.LEO      // Sun
                    isOddSign && !isFirstHalf -> ZodiacSign.CANCER  // Moon
                    !isOddSign && isFirstHalf -> ZodiacSign.CANCER  // Moon
                    else -> ZodiacSign.LEO                           // Sun
                }
            }

            VargaType.D3_DREKKANA -> {
                // Drekkana: Each sign divided into 3 parts (10° each)
                // 1st = same sign, 2nd = 5th from it, 3rd = 9th from it
                val drekkana = (degreeInSign / 10.0).toInt()
                val offset = when (drekkana) {
                    0 -> 0
                    1 -> 4
                    else -> 8
                }
                ZodiacSign.entries[(signIndex + offset) % 12]
            }

            VargaType.D4_CHATURTHAMSHA -> {
                // Chaturthamsha: Each sign divided into 4 parts (7.5° each)
                val chaturthamsha = (degreeInSign / 7.5).toInt()
                val offset = chaturthamsha * 3 // 0, 3, 6, 9 (1st, 4th, 7th, 10th)
                ZodiacSign.entries[(signIndex + offset) % 12]
            }

            VargaType.D7_SAPTAMSHA -> {
                // Saptamsha: Each sign divided into 7 parts
                val saptamsha = (degreeInSign / (30.0 / 7)).toInt().coerceIn(0, 6)
                val startSign = if (signIndex % 2 == 0) signIndex else signIndex + 6 // Odd signs start from sign, even from 7th
                ZodiacSign.entries[(startSign + saptamsha) % 12]
            }

            VargaType.D9_NAVAMSA -> {
                // Navamsa: Each sign divided into 9 parts (3.333° each)
                val navamsa = (degreeInSign / (30.0 / 9)).toInt().coerceIn(0, 8)
                // Fire signs start from Aries, Earth from Capricorn, Air from Libra, Water from Cancer
                val startSign = when (signIndex % 4) {
                    0 -> 0  // Fire signs (Aries, Leo, Sag) start from Aries
                    1 -> 9  // Earth signs (Taurus, Virgo, Cap) start from Capricorn
                    2 -> 6  // Air signs (Gemini, Libra, Aqua) start from Libra
                    else -> 3 // Water signs (Cancer, Scorpio, Pisces) start from Cancer
                }
                ZodiacSign.entries[(startSign + navamsa) % 12]
            }

            VargaType.D10_DASHAMSHA -> {
                // Dashamsha: Each sign divided into 10 parts (3° each)
                val dashamsha = (degreeInSign / 3.0).toInt().coerceIn(0, 9)
                val startSign = if (signIndex % 2 == 0) signIndex else signIndex + 9 // Odd start from sign, even from 9th
                ZodiacSign.entries[(startSign + dashamsha) % 12]
            }

            VargaType.D12_DWADASHAMSHA -> {
                // Dwadashamsha: Each sign divided into 12 parts (2.5° each)
                val dwadashamsha = (degreeInSign / 2.5).toInt().coerceIn(0, 11)
                ZodiacSign.entries[(signIndex + dwadashamsha) % 12]
            }

            VargaType.D16_SHODASHAMSHA -> {
                // Shodashamsha: Each sign divided into 16 parts
                val shodashamsha = (degreeInSign / (30.0 / 16)).toInt().coerceIn(0, 15)
                val startSign = when (signIndex % 3) {
                    0 -> 0  // Cardinal signs start from Aries
                    1 -> 4  // Fixed signs start from Leo
                    else -> 8 // Mutable signs start from Sagittarius
                }
                ZodiacSign.entries[(startSign + shodashamsha) % 12]
            }

            VargaType.D20_VIMSHAMSHA -> {
                // Vimshamsha: Each sign divided into 20 parts
                val vimshamsha = (degreeInSign / 1.5).toInt().coerceIn(0, 19)
                val startSign = when (signIndex % 3) {
                    0 -> 0  // Cardinal start from Aries
                    1 -> 8  // Fixed start from Sagittarius
                    else -> 4 // Mutable start from Leo
                }
                ZodiacSign.entries[(startSign + vimshamsha) % 12]
            }

            VargaType.D24_CHATURVIMSHAMSHA -> {
                // Chaturvimshamsha: Each sign divided into 24 parts
                val chaturvimshamsha = (degreeInSign / 1.25).toInt().coerceIn(0, 23)
                val startSign = if (signIndex % 2 == 0) 4 else 3 // Odd from Leo, Even from Cancer
                ZodiacSign.entries[(startSign + chaturvimshamsha) % 12]
            }

            VargaType.D27_SAPTAVIMSHAMSHA -> {
                // Saptavimshamsha: Each sign divided into 27 parts
                val saptavimshamsha = (degreeInSign / (30.0 / 27)).toInt().coerceIn(0, 26)
                val startSign = when (signIndex % 3) {
                    0 -> 0  // Fire signs from Aries
                    1 -> 3  // Earth signs from Cancer
                    else -> 6 // Air/Water signs from Libra
                }
                ZodiacSign.entries[(startSign + saptavimshamsha) % 12]
            }

            VargaType.D30_TRIMSHAMSHA -> {
                // Trimshamsha: Unequal division ruled by Mars, Saturn, Jupiter, Mercury, Venus
                // Different for odd and even signs
                val trimsamsha = when {
                    degreeInSign < 5 -> 0
                    degreeInSign < 10 -> 1
                    degreeInSign < 18 -> 2
                    degreeInSign < 25 -> 3
                    else -> 4
                }
                // Odd signs: Mars(Aries), Saturn(Aqua), Jupiter(Sag), Mercury(Gemini), Venus(Libra)
                // Even signs: Venus(Taurus), Mercury(Virgo), Jupiter(Pisces), Saturn(Cap), Mars(Scorpio)
                val trimsamshaSigns = if (signIndex % 2 == 0) {
                    listOf(ZodiacSign.ARIES, ZodiacSign.AQUARIUS, ZodiacSign.SAGITTARIUS, ZodiacSign.GEMINI, ZodiacSign.LIBRA)
                } else {
                    listOf(ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.PISCES, ZodiacSign.CAPRICORN, ZodiacSign.SCORPIO)
                }
                trimsamshaSigns[trimsamsha]
            }

            VargaType.D40_KHAVEDAMSHA -> {
                // Khavedamsha: Each sign divided into 40 parts
                val khavedamsha = (degreeInSign / 0.75).toInt().coerceIn(0, 39)
                val startSign = if (signIndex % 2 == 0) 0 else 6 // Odd from Aries, Even from Libra
                ZodiacSign.entries[(startSign + khavedamsha) % 12]
            }

            VargaType.D45_AKSHAVEDAMSHA -> {
                // Akshavedamsha: Each sign divided into 45 parts
                val akshavedamsha = (degreeInSign / (30.0 / 45)).toInt().coerceIn(0, 44)
                val startSign = when (signIndex % 3) {
                    0 -> 0  // Cardinal from Aries
                    1 -> 4  // Fixed from Leo
                    else -> 8 // Mutable from Sagittarius
                }
                ZodiacSign.entries[(startSign + akshavedamsha) % 12]
            }

            VargaType.D60_SHASHTIAMSHA -> {
                // Shashtiamsha: Each sign divided into 60 parts (0.5° each)
                val shashtiamsha = (degreeInSign / 0.5).toInt().coerceIn(0, 59)
                ZodiacSign.entries[(shashtiamsha) % 12]
            }
        }
    }

    /**
     * Calculate Shodashvarga strength for a planet
     */
    private fun calculatePlanetShodashvargaStrength(
        planet: Planet,
        chart: VedicChart,
        vargaCharts: Map<VargaType, VargaChart>
    ): PlanetShodashvargaStrength {
        val vargaPositions = vargaCharts.mapValues { (vargaType, vargaChart) ->
            val sign = vargaChart.planetPositions[planet] ?: ZodiacSign.ARIES
            val d1Sign = vargaCharts[VargaType.D1_RASHI]?.planetPositions?.get(planet) ?: ZodiacSign.ARIES
            val dignity = calculateVargaDignity(planet, sign)
            val isVargottama = sign == d1Sign && vargaType != VargaType.D1_RASHI

            VargaPosition(
                vargaType = vargaType,
                sign = sign,
                dignity = dignity,
                dignityPoints = dignity.points,
                isVargottama = isVargottama
            )
        }

        // Calculate different Varga Balas
        val shadvargaTypes = listOf(VargaType.D1_RASHI, VargaType.D2_HORA, VargaType.D3_DREKKANA,
            VargaType.D9_NAVAMSA, VargaType.D12_DWADASHAMSHA, VargaType.D30_TRIMSHAMSHA)
        val shadvargaBala = shadvargaTypes.sumOf { vargaPositions[it]?.dignityPoints ?: 0.0 } / 4.0

        val saptavargaTypes = shadvargaTypes + VargaType.D7_SAPTAMSHA
        val saptavargaBala = saptavargaTypes.sumOf { vargaPositions[it]?.dignityPoints ?: 0.0 } / 4.0

        val dashavargaTypes = saptavargaTypes + listOf(VargaType.D10_DASHAMSHA, VargaType.D16_SHODASHAMSHA, VargaType.D60_SHASHTIAMSHA)
        val dashavargaBala = dashavargaTypes.sumOf { vargaPositions[it]?.dignityPoints ?: 0.0 } / 4.0

        val shodashvargaBala = vargaPositions.values.sumOf { it.dignityPoints } / 4.0

        // Calculate Vimsopaka Bala for different schemes
        val vimsopakaBalaPoorva = VargaType.entries.sumOf { varga ->
            (vargaPositions[varga]?.dignityPoints ?: 0.0) * varga.poorvaWeight / 4.0
        }
        val vimsopakaBalaMadhya = VargaType.entries.sumOf { varga ->
            (vargaPositions[varga]?.dignityPoints ?: 0.0) * varga.madhyaWeight / 4.0
        }
        val vimsopakaBalaPara = VargaType.entries.sumOf { varga ->
            (vargaPositions[varga]?.dignityPoints ?: 0.0) * varga.paraWeight / 4.0
        }

        val vargottamaCount = vargaPositions.values.count { it.isVargottama }

        val strengthGrade = when {
            shodashvargaBala >= 15.0 -> StrengthGrade.EXCELLENT
            shodashvargaBala >= 12.0 -> StrengthGrade.GOOD
            shodashvargaBala >= 9.0 -> StrengthGrade.AVERAGE
            shodashvargaBala >= 6.0 -> StrengthGrade.WEAK
            else -> StrengthGrade.VERY_WEAK
        }

        val interpretation = generatePlanetInterpretation(planet, strengthGrade, vargottamaCount, vargaPositions)

        return PlanetShodashvargaStrength(
            planet = planet,
            vargaPositions = vargaPositions,
            shadvargaBala = shadvargaBala,
            saptavargaBala = saptavargaBala,
            dashavargaBala = dashavargaBala,
            shodashvargaBala = shodashvargaBala,
            vimsopakaBalaPoorva = vimsopakaBalaPoorva,
            vimsopakaBalaMadhya = vimsopakaBalaMadhya,
            vimsopakaBalaPara = vimsopakaBalaPara,
            strengthGrade = strengthGrade,
            vargottamaCount = vargottamaCount,
            interpretation = interpretation
        )
    }

    /**
     * Calculate dignity of a planet in a sign
     */
    private fun calculateVargaDignity(planet: Planet, sign: ZodiacSign): VargaDignity {
        // Check exaltation
        if (AstrologicalConstants.EXALTATION_SIGNS[planet] == sign) {
            return VargaDignity.EXALTED
        }

        // Check debilitation
        if (AstrologicalConstants.DEBILITATION_SIGNS[planet] == sign) {
            return VargaDignity.DEBILITATED
        }

        // Check own sign
        if (AstrologicalConstants.OWN_SIGNS[planet]?.contains(sign) == true || sign.ruler == planet) {
            return VargaDignity.OWN_SIGN
        }

        // Check relationship with sign lord
        val signLord = sign.ruler
        return when {
            VedicAstrologyUtils.areNaturalFriends(planet, signLord) &&
            VedicAstrologyUtils.areNaturalFriends(signLord, planet) -> VargaDignity.GREAT_FRIEND
            VedicAstrologyUtils.areNaturalFriends(planet, signLord) -> VargaDignity.FRIEND
            VedicAstrologyUtils.areNaturalEnemies(planet, signLord) &&
            VedicAstrologyUtils.areNaturalEnemies(signLord, planet) -> VargaDignity.GREAT_ENEMY
            VedicAstrologyUtils.areNaturalEnemies(planet, signLord) -> VargaDignity.ENEMY
            else -> VargaDignity.NEUTRAL
        }
    }

    /**
     * Find Vargottama planets
     */
    private fun findVargottamaPlanets(
        chart: VedicChart,
        vargaCharts: Map<VargaType, VargaChart>
    ): List<VargottamaInfo> {
        val vargottamas = mutableListOf<VargottamaInfo>()
        val d1Chart = vargaCharts[VargaType.D1_RASHI] ?: return vargottamas

        for ((planet, d1Sign) in d1Chart.planetPositions) {
            for (vargaType in VargaType.entries.filter { it != VargaType.D1_RASHI }) {
                val vargaSign = vargaCharts[vargaType]?.planetPositions?.get(planet)
                if (vargaSign == d1Sign) {
                    val significance = when (vargaType) {
                        VargaType.D9_NAVAMSA -> "Highly significant - Planet gains strong Navamsa strength for dharma and marriage"
                        VargaType.D10_DASHAMSHA -> "Career matters receive additional support"
                        VargaType.D7_SAPTAMSHA -> "Children and progeny matters strengthened"
                        else -> "${vargaType.domain} matters receive additional support"
                    }
                    vargottamas.add(
                        VargottamaInfo(
                            planet = planet,
                            sign = d1Sign,
                            vargaType = vargaType,
                            significance = significance
                        )
                    )
                }
            }
        }

        return vargottamas.sortedBy { it.planet.ordinal }
    }

    /**
     * Generate overall Varga assessment
     */
    private fun generateOverallAssessment(
        planetStrengths: Map<Planet, PlanetShodashvargaStrength>,
        vargottamas: List<VargottamaInfo>
    ): OverallVargaAssessment {
        val strongestPlanet = planetStrengths.maxByOrNull { it.value.shodashvargaBala }?.key
        val weakestPlanet = planetStrengths.minByOrNull { it.value.shodashvargaBala }?.key
        val averageStrength = planetStrengths.values.map { it.shodashvargaBala }.average()

        val strengthDistribution = planetStrengths.mapValues { it.value.strengthGrade }

        val keyInsights = mutableListOf<String>()

        // Analyze strength patterns
        if (strongestPlanet != null) {
            keyInsights.add("${strongestPlanet.displayName} is your strongest planet across divisional charts")
        }
        if (weakestPlanet != null) {
            keyInsights.add("${weakestPlanet.displayName} may need strengthening through remedies")
        }

        // Vargottama insights
        val navamsaVargottamas = vargottamas.filter { it.vargaType == VargaType.D9_NAVAMSA }
        if (navamsaVargottamas.isNotEmpty()) {
            keyInsights.add("Navamsa Vargottama: ${navamsaVargottamas.joinToString(", ") { it.planet.displayName }} - exceptional strength")
        }

        // Generate recommendations
        val recommendations = mutableListOf<String>()

        planetStrengths.filter { it.value.strengthGrade == StrengthGrade.WEAK ||
                                  it.value.strengthGrade == StrengthGrade.VERY_WEAK }
            .forEach { (planet, _) ->
                recommendations.add("Strengthen ${planet.displayName} through its specific mantras and gemstone (if suitable)")
            }

        if (averageStrength < 10.0) {
            recommendations.add("Consider general spiritual practices to uplift overall planetary strength")
        }

        return OverallVargaAssessment(
            strongestPlanet = strongestPlanet,
            weakestPlanet = weakestPlanet,
            averageStrength = averageStrength,
            strengthDistribution = strengthDistribution,
            keyInsights = keyInsights,
            recommendations = recommendations
        )
    }

    /**
     * Generate interpretation for a planet's Shodashvarga strength
     */
    private fun generatePlanetInterpretation(
        planet: Planet,
        grade: StrengthGrade,
        vargottamaCount: Int,
        positions: Map<VargaType, VargaPosition>
    ): String {
        return buildString {
            append("${planet.displayName}: ${grade.description}. ")

            if (vargottamaCount > 0) {
                append("Vargottama in $vargottamaCount chart(s). ")
            }

            // Check key divisional charts
            val navamsaDignity = positions[VargaType.D9_NAVAMSA]?.dignity
            if (navamsaDignity == VargaDignity.EXALTED || navamsaDignity == VargaDignity.OWN_SIGN) {
                append("Strong in Navamsa for dharma and marriage. ")
            }

            val dashamsaDignity = positions[VargaType.D10_DASHAMSHA]?.dignity
            if (dashamsaDignity == VargaDignity.EXALTED || dashamsaDignity == VargaDignity.OWN_SIGN) {
                append("Strong in Dashamsha for career. ")
            }

            if (grade == StrengthGrade.WEAK || grade == StrengthGrade.VERY_WEAK) {
                append("Consider remedial measures for ${planet.displayName}.")
            }
        }
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    /**
     * Get summary of Shodashvarga analysis
     */
    fun getSummary(analysis: ShodashvargaAnalysis): String {
        return buildString {
            appendLine("=== SHODASHVARGA STRENGTH ANALYSIS ===")
            appendLine()
            appendLine("PLANET STRENGTHS (Shodashvarga Bala)")
            appendLine("─────────────────────────────────────")
            analysis.planetStrengths.entries.sortedByDescending { it.value.shodashvargaBala }
                .forEach { (planet, strength) ->
                    appendLine("${planet.displayName.padEnd(10)}: ${String.format("%5.2f", strength.shodashvargaBala)} (${strength.strengthGrade.name})")
                }
            appendLine()

            if (analysis.vargottamaPlanets.isNotEmpty()) {
                appendLine("VARGOTTAMA PLANETS")
                appendLine("──────────────────")
                analysis.vargottamaPlanets.forEach { vargottama ->
                    appendLine("${vargottama.planet.displayName} in ${vargottama.sign.displayName} (${vargottama.vargaType.name})")
                }
                appendLine()
            }

            appendLine("KEY INSIGHTS")
            appendLine("────────────")
            analysis.overallAssessment.keyInsights.forEach { insight ->
                appendLine("• $insight")
            }
        }
    }

    /**
     * Get Vimsopaka Bala for a specific planet
     */
    fun getVimsopakaBalaSummary(planet: Planet, analysis: ShodashvargaAnalysis): String {
        val strength = analysis.planetStrengths[planet] ?: return "Planet not found"
        return buildString {
            appendLine("Vimsopaka Bala for ${planet.displayName}:")
            appendLine("  Poorva Scheme:  ${String.format("%.2f", strength.vimsopakaBalaPoorva)}/20")
            appendLine("  Madhya Scheme:  ${String.format("%.2f", strength.vimsopakaBalaMadhya)}/20")
            appendLine("  Para Scheme:    ${String.format("%.2f", strength.vimsopakaBalaPara)}/20")
        }
    }
}
