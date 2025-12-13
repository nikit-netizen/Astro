package com.astro.storm.ephemeris

import com.astro.storm.data.model.Planet
import com.astro.storm.data.model.VedicChart
import com.astro.storm.data.model.ZodiacSign
import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.StringKeyMatch
import com.astro.storm.data.localization.StringKeyDosha
import com.astro.storm.data.localization.StringResources
import java.util.UUID
import kotlin.math.abs
import kotlin.math.min

object RemediesCalculator {

    enum class PlanetaryStrength(val displayName: String, val severity: Int) {
        VERY_STRONG("Very Strong", 0),
        STRONG("Strong", 1),
        MODERATE("Moderate", 2),
        WEAK("Weak", 3),
        VERY_WEAK("Very Weak", 4),
        AFFLICTED("Afflicted", 5);

        fun getLocalizedName(language: Language): String {
            val key = when (this) {
                VERY_STRONG -> StringKeyMatch.PLANETARY_STRENGTH_VERY_STRONG
                STRONG -> StringKeyMatch.PLANETARY_STRENGTH_STRONG
                MODERATE -> StringKeyMatch.PLANETARY_STRENGTH_MODERATE
                WEAK -> StringKeyMatch.PLANETARY_STRENGTH_WEAK
                VERY_WEAK -> StringKeyMatch.PLANETARY_STRENGTH_VERY_WEAK
                AFFLICTED -> StringKeyMatch.PLANETARY_STRENGTH_AFFLICTED
            }
            return StringResources.get(key, language)
        }
    }

    enum class RemedyCategory(val displayName: String) {
        GEMSTONE("Gemstone"),
        MANTRA("Mantra"),
        YANTRA("Yantra"),
        CHARITY("Charity"),
        FASTING("Fasting"),
        COLOR("Color Therapy"),
        METAL("Metal"),
        RUDRAKSHA("Rudraksha"),
        DEITY("Deity Worship"),
        LIFESTYLE("Lifestyle");

        fun getLocalizedName(language: Language): String {
            val key = when (this) {
                GEMSTONE -> StringKeyMatch.REMEDY_CAT_GEMSTONE
                MANTRA -> StringKeyMatch.REMEDY_CAT_MANTRA
                YANTRA -> StringKeyMatch.REMEDY_CAT_YANTRA
                CHARITY -> StringKeyMatch.REMEDY_CAT_CHARITY
                FASTING -> StringKeyMatch.REMEDY_CAT_FASTING
                COLOR -> StringKeyMatch.REMEDY_CAT_COLOR
                METAL -> StringKeyMatch.REMEDY_CAT_METAL
                RUDRAKSHA -> StringKeyMatch.REMEDY_CAT_RUDRAKSHA
                DEITY -> StringKeyMatch.REMEDY_CAT_DEITY
                LIFESTYLE -> StringKeyMatch.REMEDY_CAT_LIFESTYLE
            }
            return StringResources.get(key, language)
        }
    }

    enum class RemedyPriority(val displayName: String, val level: Int) {
        ESSENTIAL("Essential", 1),
        HIGHLY_RECOMMENDED("Highly Recommended", 2),
        RECOMMENDED("Recommended", 3),
        OPTIONAL("Optional", 4);

        fun getLocalizedName(language: Language): String {
            val key = when (this) {
                ESSENTIAL -> StringKeyMatch.REMEDY_PRIORITY_ESSENTIAL
                HIGHLY_RECOMMENDED -> StringKeyMatch.REMEDY_PRIORITY_HIGHLY_RECOMMENDED
                RECOMMENDED -> StringKeyMatch.REMEDY_PRIORITY_RECOMMENDED
                OPTIONAL -> StringKeyMatch.REMEDY_PRIORITY_OPTIONAL
            }
            return StringResources.get(key, language)
        }
    }

    enum class PlanetaryRelationship {
        BEST_FRIEND,
        FRIEND,
        NEUTRAL,
        ENEMY,
        BITTER_ENEMY
    }

    data class Remedy(
        val id: String = UUID.randomUUID().toString(),
        val category: RemedyCategory,
        val title: String,
        val description: String,
        val method: String,
        val timing: String,
        val duration: String,
        val planet: Planet?,
        val priority: RemedyPriority,
        val benefits: List<String>,
        val cautions: List<String>,
        val mantraText: String? = null,
        val mantraSanskrit: String? = null,
        val mantraCount: Int? = null,
        val alternativeGemstone: String? = null,
        val nakshatraSpecific: Boolean = false
    )

    data class PlanetaryAnalysis(
        val planet: Planet,
        val strength: PlanetaryStrength,
        val strengthScore: Int,
        val issues: List<String>,
        val positives: List<String>,
        val needsRemedy: Boolean,
        val housePosition: Int,
        val sign: ZodiacSign,
        val nakshatra: Nakshatra,
        val nakshatraPada: Int,
        val longitude: Double,
        val isRetrograde: Boolean,
        val isCombust: Boolean,
        val isDebilitated: Boolean,
        val isExalted: Boolean,
        val isOwnSign: Boolean,
        val isMooltrikona: Boolean,
        val isFriendlySign: Boolean,
        val isEnemySign: Boolean,
        val isNeutralSign: Boolean,
        val hasNeechaBhangaRajaYoga: Boolean,
        val isInGandanta: Boolean,
        val isInMrityuBhaga: Boolean,
        val isInPushkarNavamsha: Boolean,
        val isFunctionalBenefic: Boolean,
        val isFunctionalMalefic: Boolean,
        val isYogakaraka: Boolean,
        val aspectingPlanets: List<Planet>,
        val aspectedByBenefics: Boolean,
        val aspectedByMalefics: Boolean,
        val shadbalaStrength: Double,
        val dignityDescription: String
    )

    data class RemediesResult(
        val chart: VedicChart,
        val planetaryAnalyses: List<PlanetaryAnalysis>,
        val weakestPlanets: List<Planet>,
        val remedies: List<Remedy>,
        val generalRecommendations: List<String>,
        val dashaRemedies: List<Remedy>,
        val lifeAreaFocus: Map<String, List<Remedy>>,
        val prioritizedRemedies: List<Remedy>,
        val summary: String,
        val ascendantSign: ZodiacSign,
        val moonSign: ZodiacSign,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val totalRemediesCount: Int get() = remedies.size
        val essentialRemediesCount: Int get() = remedies.count { it.priority == RemedyPriority.ESSENTIAL }

        fun toPlainText(language: Language = Language.ENGLISH): String = buildString {
            val reportTitle = StringResources.get(StringKeyMatch.REPORT_REMEDIES, language)
            val strengthAnalysisTitle = StringResources.get(StringKeyMatch.REPORT_PLANETARY_STRENGTH_ANALYSIS, language)
            val planetsAttentionTitle = StringResources.get(StringKeyMatch.REPORT_PLANETS_REQUIRING_ATTENTION, language)
            val recommendedRemediesTitle = StringResources.get(StringKeyMatch.REPORT_RECOMMENDED_REMEDIES, language)
            val generalRecommendationsTitle = StringResources.get(StringKeyMatch.REPORT_GENERAL_RECOMMENDATIONS, language)
            val summaryTitle = StringResources.get(StringKeyMatch.REPORT_SUMMARY, language)
            val generatedBy = StringResources.get(StringKeyMatch.REPORT_GENERATED_BY, language)
            val nameLabel = StringResources.get(StringKeyDosha.REPORT_NAME_LABEL, language)
            val ascendantLabel = StringResources.get(StringKeyDosha.REPORT_ASCENDANT_LABEL, language)
            val moonSignLabel = StringResources.get(StringKeyMatch.REPORT_MOON_SIGN_LABEL, language)
            val categoryLabel = StringResources.get(StringKeyMatch.REPORT_CATEGORY, language)
            val planetLabel = StringResources.get(StringKeyMatch.REPORT_PLANET, language)
            val methodLabel = StringResources.get(StringKeyMatch.REPORT_METHOD, language)
            val timingLabel = StringResources.get(StringKeyMatch.REPORT_TIMING, language)
            val mantraLabel = StringResources.get(StringKeyDosha.REPORT_MANTRA_LABEL, language)

            appendLine("═══════════════════════════════════════════════════════════")
            appendLine("              $reportTitle")
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine()
            appendLine("$nameLabel ${chart.birthData.name}")
            appendLine("$ascendantLabel ${ascendantSign.getLocalizedName(language)}")
            appendLine("$moonSignLabel ${moonSign.getLocalizedName(language)}")
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                $strengthAnalysisTitle")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine()
            planetaryAnalyses.forEach { analysis ->
                appendLine("${analysis.planet.getLocalizedName(language)}: ${analysis.strength.getLocalizedName(language)} (${analysis.strengthScore}%)")
                appendLine("  ${analysis.dignityDescription}")
                if (analysis.issues.isNotEmpty()) {
                    analysis.issues.forEach { appendLine("  ⚠ $it") }
                }
                if (analysis.positives.isNotEmpty()) {
                    analysis.positives.forEach { appendLine("  ✓ $it") }
                }
                appendLine()
            }
            if (weakestPlanets.isNotEmpty()) {
                appendLine("$planetsAttentionTitle")
                weakestPlanets.forEach { appendLine("  • ${it.getLocalizedName(language)}") }
            }
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                    $recommendedRemediesTitle")
            appendLine("─────────────────────────────────────────────────────────")
            prioritizedRemedies.take(15).forEachIndexed { index, remedy ->
                appendLine()
                appendLine("${index + 1}. ${remedy.title} [${remedy.priority.getLocalizedName(language)}]")
                appendLine("   $categoryLabel: ${remedy.category.getLocalizedName(language)}")
                remedy.planet?.let { appendLine("   $planetLabel: ${it.getLocalizedName(language)}") }
                appendLine("   ${remedy.description}")
                appendLine("   $methodLabel: ${remedy.method}")
                appendLine("   $timingLabel: ${remedy.timing}")
                remedy.mantraText?.let { appendLine("   $mantraLabel $it") }
            }
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                  $generalRecommendationsTitle")
            appendLine("─────────────────────────────────────────────────────────")
            generalRecommendations.forEach { appendLine("• $it") }
            appendLine()
            appendLine("─────────────────────────────────────────────────────────")
            appendLine("                        $summaryTitle")
            appendLine("─────────────────────────────────────────────────────────")
            appendLine(summary)
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════")
            appendLine(generatedBy)
            appendLine("═══════════════════════════════════════════════════════════")
        }
    }

    private data class GemstoneInfo(
        val primaryName: String,
        val hindiName: String,
        val colors: String,
        val metal: String,
        val minCarat: Double,
        val maxCarat: Double,
        val alternativeName: String,
        val alternativeHindiName: String,
        val fingerName: String,
        val dayToWear: String,
        val muhurtaTiming: String
    )

    private data class MantraInfo(
        val beejMantra: String,
        val beejMantraSanskrit: String,
        val gayatriMantra: String,
        val gayatriMantraSanskrit: String,
        val minimumCount: Int,
        val timing: String,
        val direction: String
    )

    private data class CharityInfo(
        val items: List<String>,
        val day: String,
        val recipients: String,
        val timing: String,
        val specialInstructions: String
    )

    private data class ExaltationDebilitationInfo(
        val exaltationSign: ZodiacSign,
        val exaltationDegree: Double,
        val debilitationSign: ZodiacSign,
        val debilitationDegree: Double,
        val mooltrikonaSign: ZodiacSign,
        val mooltrikonaStartDegree: Double,
        val mooltrikonaEndDegree: Double
    )

    private val exaltationDebilitationData = mapOf(
        Planet.SUN to ExaltationDebilitationInfo(
            ZodiacSign.ARIES, 10.0,
            ZodiacSign.LIBRA, 10.0,
            ZodiacSign.LEO, 0.0, 20.0
        ),
        Planet.MOON to ExaltationDebilitationInfo(
            ZodiacSign.TAURUS, 3.0,
            ZodiacSign.SCORPIO, 3.0,
            ZodiacSign.TAURUS, 3.0, 30.0
        ),
        Planet.MARS to ExaltationDebilitationInfo(
            ZodiacSign.CAPRICORN, 28.0,
            ZodiacSign.CANCER, 28.0,
            ZodiacSign.ARIES, 0.0, 12.0
        ),
        Planet.MERCURY to ExaltationDebilitationInfo(
            ZodiacSign.VIRGO, 15.0,
            ZodiacSign.PISCES, 15.0,
            ZodiacSign.VIRGO, 15.0, 20.0
        ),
        Planet.JUPITER to ExaltationDebilitationInfo(
            ZodiacSign.CANCER, 5.0,
            ZodiacSign.CAPRICORN, 5.0,
            ZodiacSign.SAGITTARIUS, 0.0, 10.0
        ),
        Planet.VENUS to ExaltationDebilitationInfo(
            ZodiacSign.PISCES, 27.0,
            ZodiacSign.VIRGO, 27.0,
            ZodiacSign.LIBRA, 0.0, 15.0
        ),
        Planet.SATURN to ExaltationDebilitationInfo(
            ZodiacSign.LIBRA, 20.0,
            ZodiacSign.ARIES, 20.0,
            ZodiacSign.AQUARIUS, 0.0, 20.0
        ),
        Planet.RAHU to ExaltationDebilitationInfo(
            ZodiacSign.TAURUS, 20.0,
            ZodiacSign.SCORPIO, 20.0,
            ZodiacSign.VIRGO, 0.0, 30.0
        ),
        Planet.KETU to ExaltationDebilitationInfo(
            ZodiacSign.SCORPIO, 20.0,
            ZodiacSign.TAURUS, 20.0,
            ZodiacSign.PISCES, 0.0, 30.0
        )
    )

    private val mrityuBhagaDegrees = mapOf(
        ZodiacSign.ARIES to mapOf(Planet.SUN to 20.0, Planet.MOON to 26.0, Planet.MARS to 19.0, Planet.MERCURY to 15.0, Planet.JUPITER to 18.0, Planet.VENUS to 28.0, Planet.SATURN to 10.0),
        ZodiacSign.TAURUS to mapOf(Planet.SUN to 9.0, Planet.MOON to 12.0, Planet.MARS to 28.0, Planet.MERCURY to 14.0, Planet.JUPITER to 20.0, Planet.VENUS to 15.0, Planet.SATURN to 23.0),
        ZodiacSign.GEMINI to mapOf(Planet.SUN to 12.0, Planet.MOON to 13.0, Planet.MARS to 25.0, Planet.MERCURY to 13.0, Planet.JUPITER to 19.0, Planet.VENUS to 13.0, Planet.SATURN to 22.0),
        ZodiacSign.CANCER to mapOf(Planet.SUN to 6.0, Planet.MOON to 25.0, Planet.MARS to 23.0, Planet.MERCURY to 12.0, Planet.JUPITER to 10.0, Planet.VENUS to 6.0, Planet.SATURN to 21.0),
        ZodiacSign.LEO to mapOf(Planet.SUN to 8.0, Planet.MOON to 24.0, Planet.MARS to 23.0, Planet.MERCURY to 11.0, Planet.JUPITER to 9.0, Planet.VENUS to 4.0, Planet.SATURN to 20.0),
        ZodiacSign.VIRGO to mapOf(Planet.SUN to 24.0, Planet.MOON to 11.0, Planet.MARS to 22.0, Planet.MERCURY to 10.0, Planet.JUPITER to 8.0, Planet.VENUS to 1.0, Planet.SATURN to 19.0),
        ZodiacSign.LIBRA to mapOf(Planet.SUN to 17.0, Planet.MOON to 26.0, Planet.MARS to 21.0, Planet.MERCURY to 9.0, Planet.JUPITER to 11.0, Planet.VENUS to 29.0, Planet.SATURN to 18.0),
        ZodiacSign.SCORPIO to mapOf(Planet.SUN to 22.0, Planet.MOON to 27.0, Planet.MARS to 20.0, Planet.MERCURY to 8.0, Planet.JUPITER to 12.0, Planet.VENUS to 5.0, Planet.SATURN to 17.0),
        ZodiacSign.SAGITTARIUS to mapOf(Planet.SUN to 21.0, Planet.MOON to 6.0, Planet.MARS to 10.0, Planet.MERCURY to 7.0, Planet.JUPITER to 20.0, Planet.VENUS to 8.0, Planet.SATURN to 16.0),
        ZodiacSign.CAPRICORN to mapOf(Planet.SUN to 16.0, Planet.MOON to 25.0, Planet.MARS to 11.0, Planet.MERCURY to 6.0, Planet.JUPITER to 22.0, Planet.VENUS to 14.0, Planet.SATURN to 15.0),
        ZodiacSign.AQUARIUS to mapOf(Planet.SUN to 15.0, Planet.MOON to 5.0, Planet.MARS to 12.0, Planet.MERCURY to 5.0, Planet.JUPITER to 2.0, Planet.VENUS to 20.0, Planet.SATURN to 14.0),
        ZodiacSign.PISCES to mapOf(Planet.SUN to 10.0, Planet.MOON to 12.0, Planet.MARS to 13.0, Planet.MERCURY to 4.0, Planet.JUPITER to 1.0, Planet.VENUS to 26.0, Planet.SATURN to 13.0)
    )

    private val naturalFriendships = mapOf(
        Planet.SUN to Triple(
            listOf(Planet.MOON, Planet.MARS, Planet.JUPITER),
            listOf(Planet.MERCURY),
            listOf(Planet.VENUS, Planet.SATURN, Planet.RAHU, Planet.KETU)
        ),
        Planet.MOON to Triple(
            listOf(Planet.SUN, Planet.MERCURY),
            listOf(Planet.MARS, Planet.JUPITER, Planet.VENUS, Planet.SATURN),
            emptyList<Planet>()
        ),
        Planet.MARS to Triple(
            listOf(Planet.SUN, Planet.MOON, Planet.JUPITER),
            listOf(Planet.VENUS, Planet.SATURN),
            listOf(Planet.MERCURY)
        ),
        Planet.MERCURY to Triple(
            listOf(Planet.SUN, Planet.VENUS),
            listOf(Planet.MARS, Planet.JUPITER, Planet.SATURN),
            listOf(Planet.MOON)
        ),
        Planet.JUPITER to Triple(
            listOf(Planet.SUN, Planet.MOON, Planet.MARS),
            listOf(Planet.SATURN),
            listOf(Planet.MERCURY, Planet.VENUS)
        ),
        Planet.VENUS to Triple(
            listOf(Planet.MERCURY, Planet.SATURN),
            listOf(Planet.MARS, Planet.JUPITER),
            listOf(Planet.SUN, Planet.MOON)
        ),
        Planet.SATURN to Triple(
            listOf(Planet.MERCURY, Planet.VENUS),
            listOf(Planet.JUPITER),
            listOf(Planet.SUN, Planet.MOON, Planet.MARS)
        ),
        Planet.RAHU to Triple(
            listOf(Planet.MERCURY, Planet.VENUS, Planet.SATURN),
            listOf(Planet.JUPITER),
            listOf(Planet.SUN, Planet.MOON, Planet.MARS)
        ),
        Planet.KETU to Triple(
            listOf(Planet.MARS, Planet.VENUS, Planet.SATURN),
            listOf(Planet.JUPITER, Planet.MERCURY),
            listOf(Planet.SUN, Planet.MOON)
        )
    )

    private val combustionDegrees = mapOf(
        Planet.MOON to 12.0,
        Planet.MARS to 17.0,
        Planet.MERCURY to Pair(14.0, 12.0),
        Planet.JUPITER to 11.0,
        Planet.VENUS to Pair(10.0, 8.0),
        Planet.SATURN to 15.0
    )

    private val planetaryGemstones = mapOf(
        Planet.SUN to GemstoneInfo(
            "Ruby", "Manikya", "Pigeon blood red, Pink-red", "Gold (22K)", 3.0, 5.0,
            "Red Garnet/Red Spinel", "Lal", "Ring finger", "Sunday",
            "Sunrise, during Sun Hora"
        ),
        Planet.MOON to GemstoneInfo(
            "Natural Pearl", "Moti", "White, Cream with orient", "Silver", 4.0, 7.0,
            "Moonstone", "Chandrakant Mani", "Little finger", "Monday",
            "Evening, during Moon Hora, Shukla Paksha"
        ),
        Planet.MARS to GemstoneInfo(
            "Red Coral", "Moonga", "Ox-blood red, Orange-red", "Gold/Copper", 5.0, 9.0,
            "Carnelian/Red Jasper", "Lal Hakik", "Ring finger", "Tuesday",
            "Morning, during Mars Hora"
        ),
        Planet.MERCURY to GemstoneInfo(
            "Emerald", "Panna", "Deep green with jardine", "Gold", 3.0, 6.0,
            "Peridot/Green Tourmaline", "Zabarjad", "Little finger", "Wednesday",
            "Morning, during Mercury Hora"
        ),
        Planet.JUPITER to GemstoneInfo(
            "Yellow Sapphire", "Pukhraj", "Golden yellow, Canary", "Gold (22K)", 3.0, 5.0,
            "Yellow Topaz/Citrine", "Sunehla", "Index finger", "Thursday",
            "Morning, during Jupiter Hora"
        ),
        Planet.VENUS to GemstoneInfo(
            "Diamond", "Heera", "Colorless, D-F color", "Platinum/White Gold", 0.5, 1.5,
            "White Sapphire/White Zircon", "Safed Pukhraj", "Middle/Little finger", "Friday",
            "Morning, during Venus Hora"
        ),
        Planet.SATURN to GemstoneInfo(
            "Blue Sapphire", "Neelam", "Cornflower blue, Royal blue", "Gold/Panch Dhatu", 3.0, 5.0,
            "Amethyst/Lapis Lazuli", "Jamunia", "Middle finger", "Saturday",
            "Evening, during Saturn Hora"
        ),
        Planet.RAHU to GemstoneInfo(
            "Hessonite Garnet", "Gomed", "Honey-colored, Cinnamon", "Silver/Ashtadhatu", 5.0, 8.0,
            "Orange Zircon", "Zarkon", "Middle finger", "Saturday",
            "Night, during Rahu Kaal (for propitiation)"
        ),
        Planet.KETU to GemstoneInfo(
            "Cat's Eye Chrysoberyl", "Lahsuniya", "Greenish-yellow with chatoyancy", "Silver/Gold", 3.0, 5.0,
            "Tiger's Eye", "Billori", "Middle finger", "Tuesday",
            "During Ketu's nakshatra days"
        )
    )

    private val planetaryMantras = mapOf(
        Planet.SUN to MantraInfo(
            "Om Hraam Hreem Hraum Sah Suryaya Namaha",
            "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः",
            "Om Bhaskaraya Vidmahe Divyakaraya Dhimahi Tanno Surya Prachodayat",
            "ॐ भास्कराय विद्महे दिव्यकाराय धीमहि तन्नो सूर्यः प्रचोदयात्",
            7000,
            "Sunday at sunrise, facing East",
            "East"
        ),
        Planet.MOON to MantraInfo(
            "Om Shraam Shreem Shraum Sah Chandraya Namaha",
            "ॐ श्रां श्रीं श्रौं सः चन्द्राय नमः",
            "Om Kshirputraya Vidmahe Amrittatvaya Dhimahi Tanno Chandra Prachodayat",
            "ॐ क्षीरपुत्राय विद्महे अमृतत्त्वाय धीमहि तन्नो चन्द्रः प्रचोदयात्",
            11000,
            "Monday evening, during Shukla Paksha",
            "North-West"
        ),
        Planet.MARS to MantraInfo(
            "Om Kraam Kreem Kraum Sah Bhaumaya Namaha",
            "ॐ क्रां क्रीं क्रौं सः भौमाय नमः",
            "Om Angarakaya Vidmahe Shakti Hastaya Dhimahi Tanno Bhauma Prachodayat",
            "ॐ अंगारकाय विद्महे शक्तिहस्ताय धीमहि तन्नो भौमः प्रचोदयात्",
            10000,
            "Tuesday morning, facing South",
            "South"
        ),
        Planet.MERCURY to MantraInfo(
            "Om Braam Breem Braum Sah Budhaya Namaha",
            "ॐ ब्रां ब्रीं ब्रौं सः बुधाय नमः",
            "Om Gajadhvajaya Vidmahe Graha Rajaya Dhimahi Tanno Budha Prachodayat",
            "ॐ गजध्वजाय विद्महे ग्रहराजाय धीमहि तन्नो बुधः प्रचोदयात्",
            9000,
            "Wednesday morning, facing North",
            "North"
        ),
        Planet.JUPITER to MantraInfo(
            "Om Graam Greem Graum Sah Gurave Namaha",
            "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः",
            "Om Vrishabadhvajaya Vidmahe Kruni Hastaya Dhimahi Tanno Guru Prachodayat",
            "ॐ वृषभध्वजाय विद्महे क्रुणिहस्ताय धीमहि तन्नो गुरुः प्रचोदयात्",
            19000,
            "Thursday morning, facing North-East",
            "North-East"
        ),
        Planet.VENUS to MantraInfo(
            "Om Draam Dreem Draum Sah Shukraya Namaha",
            "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः",
            "Om Rajadabaaya Vidmahe Brigusuthaya Dhimahi Tanno Shukra Prachodayat",
            "ॐ राजदाबाय विद्महे भृगुसुताय धीमहि तन्नो शुक्रः प्रचोदयात्",
            16000,
            "Friday morning, facing East",
            "South-East"
        ),
        Planet.SATURN to MantraInfo(
            "Om Praam Preem Praum Sah Shanaischaraya Namaha",
            "ॐ प्रां प्रीं प्रौं सः शनैश्चराय नमः",
            "Om Kakadvajaya Vidmahe Khadga Hastaya Dhimahi Tanno Manda Prachodayat",
            "ॐ काकध्वजाय विद्महे खड्गहस्ताय धीमहि तन्नो मन्दः प्रचोदयात्",
            23000,
            "Saturday evening, facing West",
            "West"
        ),
        Planet.RAHU to MantraInfo(
            "Om Bhraam Bhreem Bhraum Sah Rahave Namaha",
            "ॐ भ्रां भ्रीं भ्रौं सः राहवे नमः",
            "Om Naakadhvajaya Vidmahe Padma Hastaya Dhimahi Tanno Rahu Prachodayat",
            "ॐ नाकध्वजाय विद्महे पद्महस्ताय धीमहि तन्नो राहुः प्रचोदयात्",
            18000,
            "Saturday night or during Rahu Kaal",
            "South-West"
        ),
        Planet.KETU to MantraInfo(
            "Om Sraam Sreem Sraum Sah Ketave Namaha",
            "ॐ स्रां स्रीं स्रौं सः केतवे नमः",
            "Om Chitravarnaya Vidmahe Sarpa Roopaya Dhimahi Tanno Ketu Prachodayat",
            "ॐ चित्रवर्णाय विद्महे सर्परूपाय धीमहि तन्नो केतुः प्रचोदयात्",
            17000,
            "Tuesday or during Ketu's nakshatra",
            "South-West"
        )
    )

    private val planetaryCharity = mapOf(
        Planet.SUN to CharityInfo(
            listOf("Wheat", "Jaggery (Gur)", "Copper vessel", "Red/Orange cloth", "Gold"),
            "Sunday",
            "Father figures, government servants, temples",
            "Before sunset",
            "Offer water to Sun at sunrise with copper vessel"
        ),
        Planet.MOON to CharityInfo(
            listOf("Rice", "White cloth", "Silver", "Milk", "Curd", "White flowers"),
            "Monday",
            "Mother figures, elderly women, pilgrims",
            "Evening",
            "Donate near water bodies; offer milk to Shivling"
        ),
        Planet.MARS to CharityInfo(
            listOf("Red lentils (Masoor dal)", "Red cloth", "Copper", "Jaggery", "Wheat bread"),
            "Tuesday",
            "Young men, soldiers, brothers, Hanuman temples",
            "Morning",
            "Donate at Hanuman temple; feed monkeys"
        ),
        Planet.MERCURY to CharityInfo(
            listOf("Green gram (Moong dal)", "Green cloth", "Emerald green items", "Books", "Writing materials"),
            "Wednesday",
            "Students, scholars, young children, educational institutions",
            "Morning",
            "Donate to schools; feed birds with green gram"
        ),
        Planet.JUPITER to CharityInfo(
            listOf("Chana dal", "Yellow cloth", "Turmeric", "Gold", "Yellow flowers", "Books"),
            "Thursday",
            "Teachers, priests (Brahmins), temples, religious institutions",
            "Morning",
            "Donate to Vishnu/Jupiter temples; feed cows with chana dal"
        ),
        Planet.VENUS to CharityInfo(
            listOf("Rice", "White cloth", "Silk", "Perfumes", "Sweets", "Ghee"),
            "Friday",
            "Young women, artists, Lakshmi temples, cow shelters",
            "Morning",
            "Donate to women's welfare; offer white flowers to Lakshmi"
        ),
        Planet.SATURN to CharityInfo(
            listOf("Black gram (Urad dal)", "Iron", "Sesame oil", "Blue/Black cloth", "Mustard oil"),
            "Saturday",
            "Poor and needy, servants, elderly, disabled, Shani temples",
            "Evening",
            "Feed crows; offer mustard oil at Shani temple; serve the disabled"
        ),
        Planet.RAHU to CharityInfo(
            listOf("Coconut", "Blue cloth", "Sesame seeds", "Lead", "Blanket"),
            "Saturday",
            "Outcasts, sweepers, Durga temples",
            "Night",
            "Donate at crossroads; offer to Durga temple"
        ),
        Planet.KETU to CharityInfo(
            listOf("Mixed seven grains", "Gray/Brown blanket", "Sesame seeds", "Dog food"),
            "Tuesday or Saturday",
            "Spiritual seekers, sadhus, dog shelters, Ganesha temples",
            "Before sunrise or after sunset",
            "Feed dogs; donate blankets to homeless; offer at Ganesha temple"
        )
    )

    private val nakshatraDeities = mapOf(
        Nakshatra.ASHWINI to "Ashwini Kumaras",
        Nakshatra.BHARANI to "Yama",
        Nakshatra.KRITTIKA to "Agni",
        Nakshatra.ROHINI to "Brahma/Prajapati",
        Nakshatra.MRIGASHIRA to "Soma/Chandra",
        Nakshatra.ARDRA to "Rudra",
        Nakshatra.PUNARVASU to "Aditi",
        Nakshatra.PUSHYA to "Brihaspati",
        Nakshatra.ASHLESHA to "Nagas/Serpent deities",
        Nakshatra.MAGHA to "Pitris (Ancestors)",
        Nakshatra.PURVA_PHALGUNI to "Bhaga",
        Nakshatra.UTTARA_PHALGUNI to "Aryaman",
        Nakshatra.HASTA to "Savitar",
        Nakshatra.CHITRA to "Vishwakarma",
        Nakshatra.SWATI to "Vayu",
        Nakshatra.VISHAKHA to "Indra-Agni",
        Nakshatra.ANURADHA to "Mitra",
        Nakshatra.JYESHTHA to "Indra",
        Nakshatra.MULA to "Nirriti/Kali",
        Nakshatra.PURVA_ASHADHA to "Apas (Waters)",
        Nakshatra.UTTARA_ASHADHA to "Vishvadevas",
        Nakshatra.SHRAVANA to "Vishnu",
        Nakshatra.DHANISHTHA to "Vasus",
        Nakshatra.SHATABHISHA to "Varuna",
        Nakshatra.PURVA_BHADRAPADA to "Aja Ekapad",
        Nakshatra.UTTARA_BHADRAPADA to "Ahir Budhnya",
        Nakshatra.REVATI to "Pushan"
    )

    fun calculateRemedies(chart: VedicChart): RemediesResult {
        val ascendantLongitude = chart.ascendant ?: 0.0
val ascendantSign: ZodiacSign = ZodiacSign.values()[(ascendantLongitude / 30.0).toInt() % 12]
        val moonPosition = chart.planetPositions.find { it.planet == Planet.MOON }
        val moonSign = moonPosition?.sign ?: ZodiacSign.ARIES

        val planetaryAnalyses = Planet.MAIN_PLANETS.map { planet ->
            analyzePlanet(planet, chart, ascendantSign)
        }

        val weakestPlanets = planetaryAnalyses
            .filter { it.needsRemedy }
            .sortedWith(compareBy({ -it.strength.severity }, { it.strengthScore }))
            .map { it.planet }

        val allRemedies = mutableListOf<Remedy>()

        planetaryAnalyses.filter { it.needsRemedy }.forEach { analysis ->
            if (analysis.isFunctionalBenefic || analysis.isYogakaraka) {
                getGemstoneRemedy(analysis)?.let { allRemedies.add(it) }
            }

            getMantraRemedy(analysis.planet)?.let { allRemedies.add(it) }
            getCharityRemedy(analysis.planet)?.let { allRemedies.add(it) }
        }

        weakestPlanets.take(3).forEach { planet ->
            getFastingRemedy(planet)?.let { allRemedies.add(it) }
        }

        weakestPlanets.take(3).forEach { planet ->
            getColorRemedy(planet)?.let { allRemedies.add(it) }
            getLifestyleRemedy(planet)?.let { allRemedies.add(it) }
            getRudrakshaRemedy(planet)?.let { allRemedies.add(it) }
        }

        weakestPlanets.take(2).forEach { planet ->
            getYantraRemedy(planet)?.let { allRemedies.add(it) }
        }

        planetaryAnalyses.filter { it.needsRemedy }.forEach { analysis ->
            getDeityRemedy(analysis.planet)?.let { allRemedies.add(it) }
        }

        planetaryAnalyses.forEach { analysis ->
            getNakshatraRemedy(analysis)?.let { allRemedies.add(it) }
        }

        planetaryAnalyses
            .filter { it.isInGandanta && it.needsRemedy }
            .forEach { analysis ->
                getGandantaRemedy(analysis)?.let { allRemedies.add(it) }
            }

        val generalRecommendations = generateGeneralRecommendations(chart, planetaryAnalyses, ascendantSign)
        val dashaRemedies = generateDashaRemedies(chart, planetaryAnalyses)
        val lifeAreaFocus = categorizeByLifeArea(allRemedies, planetaryAnalyses)
        val prioritizedRemedies = prioritizeRemedies(allRemedies, planetaryAnalyses, chart)
        val summary = generateSummary(planetaryAnalyses, weakestPlanets, ascendantSign)

        return RemediesResult(
            chart = chart,
            planetaryAnalyses = planetaryAnalyses,
            weakestPlanets = weakestPlanets,
            remedies = allRemedies,
            generalRecommendations = generalRecommendations,
            dashaRemedies = dashaRemedies,
            lifeAreaFocus = lifeAreaFocus,
            prioritizedRemedies = prioritizedRemedies,
            summary = summary,
            ascendantSign = ascendantSign,
            moonSign = moonSign
        )
    }

    private fun analyzePlanet(
        planet: Planet,
        chart: VedicChart,
        ascendantSign: ZodiacSign
    ): PlanetaryAnalysis {
        val position = chart.planetPositions.find { it.planet == planet }
            ?: return createDefaultAnalysis(planet)

        val issues = mutableListOf<String>()
        val positives = mutableListOf<String>()
        var strengthScore = 50.0

        val sign = position.sign
        val house = position.house
        val longitude = position.longitude
        val signLongitude = longitude % 30.0

        val exDebInfo = exaltationDebilitationData[planet]
        val isDebilitated = exDebInfo?.debilitationSign == sign
        val isExalted = exDebInfo?.exaltationSign == sign

        val deepExaltation = if (isExalted && exDebInfo != null) {
            abs(signLongitude - exDebInfo.exaltationDegree) <= 1.0
        } else false

        val deepDebilitation = if (isDebilitated && exDebInfo != null) {
            abs(signLongitude - exDebInfo.debilitationDegree) <= 1.0
        } else false

        if (isDebilitated) {
            if (deepDebilitation) {
                issues.add("Deeply debilitated at ${String.format("%.1f", signLongitude)}° ${sign.displayName}")
                strengthScore -= 35
            } else {
                issues.add("Debilitated in ${sign.displayName}")
                strengthScore -= 25
            }
        }

        if (isExalted) {
            if (deepExaltation) {
                positives.add("Deeply exalted at ${String.format("%.1f", signLongitude)}° ${sign.displayName}")
                strengthScore += 35
            } else {
                positives.add("Exalted in ${sign.displayName}")
                strengthScore += 25
            }
        }

        val hasNeechaBhanga = if (isDebilitated) {
            checkNeechaBhangaRajaYoga(planet, chart, ascendantSign)
        } else false

        if (hasNeechaBhanga) {
            positives.add("Neecha Bhanga Raja Yoga - Debilitation cancelled")
            strengthScore += 20
        }

        val isOwnSign = isInOwnSign(planet, sign)
        if (isOwnSign) {
            positives.add("In own sign ${sign.displayName}")
            strengthScore += 15
        }

        val isMooltrikona = isInMooltrikona(planet, sign, signLongitude)
        if (isMooltrikona && !isOwnSign) {
            positives.add("In Mooltrikona")
            strengthScore += 12
        }

        val relationship = getPlanetaryRelationship(planet, sign.ruler)
        val isFriendlySign = relationship in listOf(PlanetaryRelationship.FRIEND, PlanetaryRelationship.BEST_FRIEND)
        val isEnemySign = relationship in listOf(PlanetaryRelationship.ENEMY, PlanetaryRelationship.BITTER_ENEMY)
        val isNeutralSign = relationship == PlanetaryRelationship.NEUTRAL

        if (isFriendlySign && !isOwnSign && !isExalted) {
            positives.add("In friend's sign (${sign.ruler.displayName})")
            strengthScore += 8
        }

        if (isEnemySign && !isDebilitated) {
            issues.add("In enemy sign (${sign.ruler.displayName})")
            strengthScore -= 10
        }

        val houseCategory = when (house) {
            1, 4, 7, 10 -> {
                positives.add("Placed in Kendra (angle) house $house")
                strengthScore += 10
                "kendra"
            }
            5, 9 -> {
                positives.add("Placed in Trikona (trine) house $house")
                strengthScore += 10
                "trikona"
            }
            6, 8, 12 -> {
                issues.add("Placed in Dusthana house $house")
                strengthScore -= 15
                "dusthana"
            }
            2, 11 -> {
                positives.add("Placed in wealth house $house")
                strengthScore += 5
                "wealth"
            }
            3 -> {
                "upachaya"
            }
            else -> "other"
        }

        val isRetrograde = position.isRetrograde
        if (isRetrograde && planet != Planet.SUN && planet != Planet.MOON) {
            if (planet in listOf(Planet.SATURN, Planet.JUPITER)) {
                positives.add("Retrograde (strengthened)")
                strengthScore += 5
            } else if (planet == Planet.MERCURY) {
                issues.add("Retrograde (review communication matters)")
            } else if (planet in listOf(Planet.MARS, Planet.VENUS)) {
                issues.add("Retrograde (internalized energy)")
                strengthScore -= 5
            }
        }

        val isCombust = checkCombustion(planet, chart, isRetrograde)
        if (isCombust) {
            val combustStrength = if (planet == Planet.MOON) "severely" else "moderately"
            issues.add("Combust by Sun ($combustStrength weakened)")
            strengthScore -= if (planet == Planet.MOON) 25 else 20
        }

        val conjunctMalefics = checkMaleficConjunction(planet, chart)
        if (conjunctMalefics.isNotEmpty()) {
            issues.add("Conjunct malefics: ${conjunctMalefics.joinToString { it.displayName }}")
            strengthScore -= conjunctMalefics.size * 7
        }

        val conjunctBenefics = checkBeneficConjunction(planet, chart, ascendantSign)
        if (conjunctBenefics.isNotEmpty()) {
            positives.add("Conjunct benefics: ${conjunctBenefics.joinToString { it.displayName }}")
            strengthScore += conjunctBenefics.size * 5
        }

        val aspectingPlanets = getAspectingPlanets(planet, chart)
        val aspectedByBenefics = aspectingPlanets.any { it in listOf(Planet.JUPITER, Planet.VENUS) }
        val aspectedByMalefics = aspectingPlanets.any { it in listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU) }

        if (aspectedByBenefics && Planet.JUPITER in aspectingPlanets) {
            positives.add("Aspected by Jupiter (protected)")
            strengthScore += 8
        }
        if (aspectedByMalefics && Planet.SATURN in aspectingPlanets) {
            issues.add("Aspected by Saturn")
            strengthScore -= 5
        }

        val isInGandanta = checkGandanta(sign, signLongitude)
        if (isInGandanta) {
            issues.add("In Gandanta (karmic knot at water-fire sign junction)")
            strengthScore -= 12
        }

        val isInMrityuBhaga = checkMrityuBhaga(planet, sign, signLongitude)
        if (isInMrityuBhaga) {
            issues.add("In Mrityu Bhaga (critical degree)")
            strengthScore -= 8
        }

        val isInPushkara = checkPushkaraNavamsha(signLongitude, sign)
        if (isInPushkara) {
            positives.add("In Pushkara Navamsha (auspicious degree)")
            strengthScore += 5
        }

        val isFunctionalBenefic = isFunctionalBeneficForLagna(planet, ascendantSign)
        val isFunctionalMalefic = isFunctionalMaleficForLagna(planet, ascendantSign)
        val isYogakaraka = isYogakarakaPlanet(planet, ascendantSign)

        if (isYogakaraka) {
            positives.add("Yogakaraka planet for ${ascendantSign.displayName} Lagna")
            strengthScore += 10
        }

        if (planet == Planet.MOON) {
            val moonStrength = checkMoonPaksha(chart)
            if (moonStrength < 0) {
                issues.add("Dark Moon (Krishna Paksha) - emotionally challenged")
                strengthScore += moonStrength
            } else if (moonStrength > 0) {
                positives.add("Bright Moon (Shukla Paksha)")
                strengthScore += moonStrength
            }
        }

        strengthScore = strengthScore.coerceIn(0.0, 100.0)

        val strength = when {
            strengthScore >= 80 -> PlanetaryStrength.VERY_STRONG
            strengthScore >= 65 -> PlanetaryStrength.STRONG
            strengthScore >= 45 -> PlanetaryStrength.MODERATE
            strengthScore >= 30 -> PlanetaryStrength.WEAK
            strengthScore >= 15 -> PlanetaryStrength.VERY_WEAK
            else -> PlanetaryStrength.AFFLICTED
        }

        val needsRemedy = strength.severity >= 3 || issues.size >= 2 || isDebilitated || isCombust || isInGandanta

        val (nakshatra, pada) = Nakshatra.fromLongitude(longitude)

        val dignityDescription = buildDignityDescription(
            planet, sign, isExalted, isDebilitated, isOwnSign, isMooltrikona,
            isFriendlySign, isEnemySign, isNeutralSign, isRetrograde, isCombust
        )

        return PlanetaryAnalysis(
            planet = planet,
            strength = strength,
            strengthScore = strengthScore.toInt(),
            issues = issues,
            positives = positives,
            needsRemedy = needsRemedy,
            housePosition = house,
            sign = sign,
            nakshatra = nakshatra,
            nakshatraPada = pada,
            longitude = longitude,
            isRetrograde = isRetrograde,
            isCombust = isCombust,
            isDebilitated = isDebilitated,
            isExalted = isExalted,
            isOwnSign = isOwnSign,
            isMooltrikona = isMooltrikona,
            isFriendlySign = isFriendlySign,
            isEnemySign = isEnemySign,
            isNeutralSign = isNeutralSign,
            hasNeechaBhangaRajaYoga = hasNeechaBhanga,
            isInGandanta = isInGandanta,
            isInMrityuBhaga = isInMrityuBhaga,
            isInPushkarNavamsha = isInPushkara,
            isFunctionalBenefic = isFunctionalBenefic,
            isFunctionalMalefic = isFunctionalMalefic,
            isYogakaraka = isYogakaraka,
            aspectingPlanets = aspectingPlanets,
            aspectedByBenefics = aspectedByBenefics,
            aspectedByMalefics = aspectedByMalefics,
            shadbalaStrength = strengthScore / 100.0,
            dignityDescription = dignityDescription
        )
    }

    private fun createDefaultAnalysis(planet: Planet): PlanetaryAnalysis {
        val (nakshatra, pada) = Nakshatra.fromLongitude(0.0)
        return PlanetaryAnalysis(
            planet = planet,
            strength = PlanetaryStrength.MODERATE,
            strengthScore = 50,
            issues = emptyList(),
            positives = emptyList(),
            needsRemedy = false,
            housePosition = 1,
            sign = ZodiacSign.ARIES,
            nakshatra = nakshatra,
            nakshatraPada = pada,
            longitude = 0.0,
            isRetrograde = false,
            isCombust = false,
            isDebilitated = false,
            isExalted = false,
            isOwnSign = false,
            isMooltrikona = false,
            isFriendlySign = false,
            isEnemySign = false,
            isNeutralSign = true,
            hasNeechaBhangaRajaYoga = false,
            isInGandanta = false,
            isInMrityuBhaga = false,
            isInPushkarNavamsha = false,
            isFunctionalBenefic = false,
            isFunctionalMalefic = false,
            isYogakaraka = false,
            aspectingPlanets = emptyList(),
            aspectedByBenefics = false,
            aspectedByMalefics = false,
            shadbalaStrength = 0.5,
            dignityDescription = "Position unknown"
        )
    }

    private fun buildDignityDescription(
        planet: Planet,
        sign: ZodiacSign,
        isExalted: Boolean,
        isDebilitated: Boolean,
        isOwnSign: Boolean,
        isMooltrikona: Boolean,
        isFriendlySign: Boolean,
        isEnemySign: Boolean,
        isNeutralSign: Boolean,
        isRetrograde: Boolean,
        isCombust: Boolean
    ): String {
        val parts = mutableListOf<String>()
        parts.add("${planet.displayName} in ${sign.displayName}")

        when {
            isExalted -> parts.add("(Exalted)")
            isDebilitated -> parts.add("(Debilitated)")
            isMooltrikona -> parts.add("(Mooltrikona)")
            isOwnSign -> parts.add("(Own Sign)")
            isFriendlySign -> parts.add("(Friendly)")
            isEnemySign -> parts.add("(Enemy)")
            isNeutralSign -> parts.add("(Neutral)")
        }

        if (isRetrograde) parts.add("Retrograde")
        if (isCombust) parts.add("Combust")

        return parts.joinToString(" ")
    }

    private fun isInOwnSign(planet: Planet, sign: ZodiacSign): Boolean {
        return when (planet) {
            Planet.SUN -> sign == ZodiacSign.LEO
            Planet.MOON -> sign == ZodiacSign.CANCER
            Planet.MARS -> sign in listOf(ZodiacSign.ARIES, ZodiacSign.SCORPIO)
            Planet.MERCURY -> sign in listOf(ZodiacSign.GEMINI, ZodiacSign.VIRGO)
            Planet.JUPITER -> sign in listOf(ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES)
            Planet.VENUS -> sign in listOf(ZodiacSign.TAURUS, ZodiacSign.LIBRA)
            Planet.SATURN -> sign in listOf(ZodiacSign.CAPRICORN, ZodiacSign.AQUARIUS)
            Planet.RAHU -> sign == ZodiacSign.AQUARIUS
            Planet.KETU -> sign == ZodiacSign.SCORPIO
            else -> false
        }
    }

    private fun isInMooltrikona(planet: Planet, sign: ZodiacSign, signDegree: Double): Boolean {
        val info = exaltationDebilitationData[planet] ?: return false
        return sign == info.mooltrikonaSign &&
                signDegree >= info.mooltrikonaStartDegree &&
                signDegree <= info.mooltrikonaEndDegree
    }

    private fun getPlanetaryRelationship(planet1: Planet, planet2: Planet): PlanetaryRelationship {
        if (planet1 == planet2) return PlanetaryRelationship.BEST_FRIEND

        val friendships = naturalFriendships[planet1] ?: return PlanetaryRelationship.NEUTRAL
        val (friends, neutrals, enemies) = friendships

        return when (planet2) {
            in friends -> PlanetaryRelationship.FRIEND
            in enemies -> PlanetaryRelationship.ENEMY
            in neutrals -> PlanetaryRelationship.NEUTRAL
            else -> PlanetaryRelationship.NEUTRAL
        }
    }

    private fun checkNeechaBhangaRajaYoga(
        planet: Planet,
        chart: VedicChart,
        ascendantSign: ZodiacSign
    ): Boolean {
        val debInfo = exaltationDebilitationData[planet] ?: return false
        val debSign = debInfo.debilitationSign
        val exaltSign = debInfo.exaltationSign

        val debLord = debSign.ruler
        val exaltLord = exaltSign.ruler

        val ascendantHouse = 1
        val moonPosition = chart.planetPositions.find { it.planet == Planet.MOON }
        val moonHouse = moonPosition?.house ?: 1

        val kendraHouses = listOf(1, 4, 7, 10)

        val debLordPosition = chart.planetPositions.find { it.planet == debLord }
        if (debLordPosition != null) {
            val debLordHouseFromAsc = debLordPosition.house
            val debLordHouseFromMoon = ((debLordPosition.house - moonHouse + 12) % 12) + 1

            if (debLordHouseFromAsc in kendraHouses || debLordHouseFromMoon in kendraHouses) {
                return true
            }
        }

        val exaltLordPosition = chart.planetPositions.find { it.planet == exaltLord }
        val planetPosition = chart.planetPositions.find { it.planet == planet }

        if (exaltLordPosition != null && planetPosition != null) {
            if (exaltLordPosition.house == planetPosition.house) {
                return true
            }
        }

        val exaltedPlanets = chart.planetPositions.filter { pos ->
            val exInfo = exaltationDebilitationData[pos.planet]
            exInfo?.exaltationSign == pos.sign
        }

        if (exaltedPlanets.any { it.house == planetPosition?.house }) {
            return true
        }

        return false
    }

    private fun checkCombustion(planet: Planet, chart: VedicChart, isRetrograde: Boolean): Boolean {
        if (planet == Planet.SUN || planet == Planet.RAHU || planet == Planet.KETU) return false

        val sunPos = chart.planetPositions.find { it.planet == Planet.SUN } ?: return false
        val planetPos = chart.planetPositions.find { it.planet == planet } ?: return false

        val combustDegree = when (val deg = combustionDegrees[planet]) {
            is Double -> deg
            is Pair<*, *> -> if (isRetrograde) (deg.second as Double) else (deg.first as Double)
            else -> return false
        }

        val diff = abs(sunPos.longitude - planetPos.longitude)
        val normalizedDiff = if (diff > 180) 360 - diff else diff

        return normalizedDiff <= combustDegree
    }

    private fun checkMaleficConjunction(planet: Planet, chart: VedicChart): List<Planet> {
        val malefics = listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU, Planet.SUN)
        val planetPos = chart.planetPositions.find { it.planet == planet } ?: return emptyList()

        return chart.planetPositions
            .filter { it.planet in malefics && it.planet != planet }
            .filter { it.house == planetPos.house }
            .map { it.planet }
    }

    private fun checkBeneficConjunction(
        planet: Planet,
        chart: VedicChart,
        ascendantSign: ZodiacSign
    ): List<Planet> {
        val benefics = mutableListOf(Planet.JUPITER, Planet.VENUS)

        val moonPosition = chart.planetPositions.find { it.planet == Planet.MOON }
        if (moonPosition != null) {
            val paksha = checkMoonPaksha(chart)
            if (paksha > 0) benefics.add(Planet.MOON)
        }

        val mercuryPosition = chart.planetPositions.find { it.planet == Planet.MERCURY }
        if (mercuryPosition != null) {
            val mercuryConjuncts = chart.planetPositions
                .filter { it.house == mercuryPosition.house && it.planet != Planet.MERCURY }
                .map { it.planet }
            val maleficCount = mercuryConjuncts.count { it in listOf(Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU) }
            val beneficCount = mercuryConjuncts.count { it in listOf(Planet.JUPITER, Planet.VENUS) }
            if (beneficCount >= maleficCount) benefics.add(Planet.MERCURY)
        }

        val planetPos = chart.planetPositions.find { it.planet == planet } ?: return emptyList()

        return chart.planetPositions
            .filter { it.planet in benefics && it.planet != planet }
            .filter { it.house == planetPos.house }
            .map { it.planet }
    }

    private fun getAspectingPlanets(planet: Planet, chart: VedicChart): List<Planet> {
        val planetPos = chart.planetPositions.find { it.planet == planet } ?: return emptyList()
        val planetHouse = planetPos.house

        val aspectingPlanets = mutableListOf<Planet>()

        chart.planetPositions.forEach { pos ->
            if (pos.planet == planet) return@forEach

            val aspecterHouse = pos.house
            val houseDiff = ((planetHouse - aspecterHouse + 12) % 12)

            val aspects = when (pos.planet) {
                Planet.MARS -> listOf(4, 7, 8)
                Planet.JUPITER -> listOf(5, 7, 9)
                Planet.SATURN -> listOf(3, 7, 10)
                Planet.RAHU, Planet.KETU -> listOf(5, 7, 9)
                else -> listOf(7)
            }

            if (houseDiff in aspects) {
                aspectingPlanets.add(pos.planet)
            }
        }

        return aspectingPlanets
    }

    private fun checkGandanta(sign: ZodiacSign, signDegree: Double): Boolean {
        val gandantaJunctions = listOf(
            ZodiacSign.CANCER to ZodiacSign.LEO,
            ZodiacSign.SCORPIO to ZodiacSign.SAGITTARIUS,
            ZodiacSign.PISCES to ZodiacSign.ARIES
        )

        for ((waterSign, fireSign) in gandantaJunctions) {
            if (sign == waterSign && signDegree >= 26.40) return true
            if (sign == fireSign && signDegree <= 3.20) return true
        }

        return false
    }

    private fun checkMrityuBhaga(planet: Planet, sign: ZodiacSign, signDegree: Double): Boolean {
        val mrityuDegree = mrityuBhagaDegrees[sign]?.get(planet) ?: return false
        return abs(signDegree - mrityuDegree) <= 1.0
    }

    private fun checkPushkaraNavamsha(signDegree: Double, sign: ZodiacSign): Boolean {
        val navamshaSize = 3.333333
        val navamshaIndex = (signDegree / navamshaSize).toInt()

        val pushkaraNavamshas = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.LEO, ZodiacSign.SAGITTARIUS -> listOf(2, 5, 8)
            ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.CAPRICORN -> listOf(1, 4, 7)
            ZodiacSign.GEMINI, ZodiacSign.LIBRA, ZodiacSign.AQUARIUS -> listOf(0, 3, 6)
            ZodiacSign.CANCER, ZodiacSign.SCORPIO, ZodiacSign.PISCES -> listOf(2, 5, 8)
        }

        return navamshaIndex in pushkaraNavamshas
    }

    private fun checkMoonPaksha(chart: VedicChart): Int {
        val sunPos = chart.planetPositions.find { it.planet == Planet.SUN } ?: return 0
        val moonPos = chart.planetPositions.find { it.planet == Planet.MOON } ?: return 0

        var diff = moonPos.longitude - sunPos.longitude
        if (diff < 0) diff += 360

        val tithi = (diff / 12).toInt() + 1

        return when {
            tithi in 1..5 -> -10
            tithi in 6..10 -> -5
            tithi in 11..15 -> 5
            tithi in 16..20 -> 10
            tithi in 21..25 -> 5
            tithi in 26..30 -> -5
            else -> 0
        }
    }

    private fun isFunctionalBeneficForLagna(planet: Planet, lagna: ZodiacSign): Boolean {
        return when (lagna) {
            ZodiacSign.ARIES -> planet in listOf(Planet.SUN, Planet.MARS, Planet.JUPITER)
            ZodiacSign.TAURUS -> planet in listOf(Planet.SATURN, Planet.MERCURY, Planet.VENUS)
            ZodiacSign.GEMINI -> planet in listOf(Planet.VENUS, Planet.SATURN)
            ZodiacSign.CANCER -> planet in listOf(Planet.MARS, Planet.JUPITER, Planet.MOON)
            ZodiacSign.LEO -> planet in listOf(Planet.MARS, Planet.JUPITER, Planet.SUN)
            ZodiacSign.VIRGO -> planet in listOf(Planet.MERCURY, Planet.VENUS)
            ZodiacSign.LIBRA -> planet in listOf(Planet.SATURN, Planet.MERCURY, Planet.VENUS)
            ZodiacSign.SCORPIO -> planet in listOf(Planet.JUPITER, Planet.MOON, Planet.SUN)
            ZodiacSign.SAGITTARIUS -> planet in listOf(Planet.SUN, Planet.MARS, Planet.JUPITER)
            ZodiacSign.CAPRICORN -> planet in listOf(Planet.VENUS, Planet.MERCURY, Planet.SATURN)
            ZodiacSign.AQUARIUS -> planet in listOf(Planet.VENUS, Planet.SATURN)
            ZodiacSign.PISCES -> planet in listOf(Planet.MARS, Planet.MOON, Planet.JUPITER)
        }
    }

    private fun isFunctionalMaleficForLagna(planet: Planet, lagna: ZodiacSign): Boolean {
        return when (lagna) {
            ZodiacSign.ARIES -> planet in listOf(Planet.MERCURY, Planet.SATURN)
            ZodiacSign.TAURUS -> planet in listOf(Planet.JUPITER, Planet.MARS)
            ZodiacSign.GEMINI -> planet in listOf(Planet.MARS, Planet.JUPITER)
            ZodiacSign.CANCER -> planet in listOf(Planet.SATURN, Planet.MERCURY)
            ZodiacSign.LEO -> planet in listOf(Planet.SATURN, Planet.MERCURY)
            ZodiacSign.VIRGO -> planet in listOf(Planet.MARS, Planet.MOON)
            ZodiacSign.LIBRA -> planet in listOf(Planet.MARS, Planet.JUPITER, Planet.SUN)
            ZodiacSign.SCORPIO -> planet in listOf(Planet.VENUS, Planet.MERCURY)
            ZodiacSign.SAGITTARIUS -> planet in listOf(Planet.VENUS, Planet.SATURN)
            ZodiacSign.CAPRICORN -> planet in listOf(Planet.MARS, Planet.JUPITER, Planet.MOON)
            ZodiacSign.AQUARIUS -> planet in listOf(Planet.MARS, Planet.JUPITER, Planet.MOON)
            ZodiacSign.PISCES -> planet in listOf(Planet.SATURN, Planet.VENUS, Planet.SUN, Planet.MERCURY)
        }
    }

    private fun isYogakarakaPlanet(planet: Planet, lagna: ZodiacSign): Boolean {
        return when (lagna) {
            ZodiacSign.ARIES -> planet == Planet.SATURN
            ZodiacSign.TAURUS -> planet == Planet.SATURN
            ZodiacSign.CANCER -> planet == Planet.MARS
            ZodiacSign.LEO -> planet == Planet.MARS
            ZodiacSign.LIBRA -> planet == Planet.SATURN
            ZodiacSign.SCORPIO -> planet == Planet.MOON
            ZodiacSign.CAPRICORN -> planet == Planet.VENUS
            ZodiacSign.AQUARIUS -> planet == Planet.VENUS
            else -> false
        }
    }

    private fun getGemstoneRemedy(analysis: PlanetaryAnalysis): Remedy? {
        val planet = analysis.planet
        val gemInfo = planetaryGemstones[planet] ?: return null

        val shouldRecommend = analysis.isFunctionalBenefic || analysis.isYogakaraka

        if (!shouldRecommend) {
            return Remedy(
                category = RemedyCategory.GEMSTONE,
                title = "${gemInfo.primaryName} - Consult Before Wearing",
                description = "${planet.displayName} is not a functional benefic for your Lagna. Wearing ${gemInfo.primaryName} may increase challenges. Consider alternative remedies.",
                method = "CAUTION: Get expert consultation before wearing. If advised, wear ${gemInfo.minCarat}-${gemInfo.maxCarat} carat ${gemInfo.primaryName} in ${gemInfo.metal} on ${gemInfo.fingerName} finger.",
                timing = "Only on ${gemInfo.dayToWear} during ${gemInfo.muhurtaTiming}",
                duration = "Trial period of 3-7 days strongly recommended",
                planet = planet,
                priority = RemedyPriority.OPTIONAL,
                benefits = listOf(
                    "May strengthen ${planet.displayName} if properly advised",
                    "Professional guidance essential"
                ),
                cautions = listOf(
                    "NOT automatically recommended - expert consultation required",
                    "${planet.displayName} is a challenging planet for your Lagna",
                    "Consider mantra and charity as safer alternatives",
                    "Monitor carefully during trial period"
                ),
                alternativeGemstone = gemInfo.alternativeName
            )
        }

        val priority = when {
            analysis.isYogakaraka -> RemedyPriority.ESSENTIAL
            analysis.strengthScore < 30 -> RemedyPriority.HIGHLY_RECOMMENDED
            else -> RemedyPriority.RECOMMENDED
        }

        return Remedy(
            category = RemedyCategory.GEMSTONE,
            title = "${gemInfo.primaryName} (${gemInfo.hindiName})",
            description = "Primary gemstone for strengthening ${planet.displayName}. As a functional benefic for your Lagna, this gemstone is suitable.",
            method = "Wear ${gemInfo.minCarat}-${gemInfo.maxCarat} carat natural, untreated ${gemInfo.primaryName} (${gemInfo.colors}) set in ${gemInfo.metal} on the ${gemInfo.fingerName} finger of right hand.",
            timing = "${gemInfo.dayToWear} during ${gemInfo.muhurtaTiming}. Purify with raw milk and Ganga water before wearing.",
            duration = "Continuous wear recommended. Remove during impure activities.",
            planet = planet,
            priority = priority,
            benefits = listOf(
                "Strengthens ${planet.displayName}'s positive influences",
                "Enhances ${getPlanetLifeArea(planet)}",
                "Provides continuous energy balancing",
                "Suitable for ${planet.displayName} as functional benefic"
            ),
            cautions = listOf(
                "Ensure the gemstone is natural and untreated",
                "Buy from reputable dealer with certification",
                "Trial period of 3-7 days recommended",
                "Remove if experiencing adverse effects"
            ),
            alternativeGemstone = "${gemInfo.alternativeName} (${gemInfo.alternativeHindiName}) - More affordable substitute"
        )
    }

    private fun getMantraRemedy(planet: Planet): Remedy? {
        val mantraInfo = planetaryMantras[planet] ?: return null

        return Remedy(
            category = RemedyCategory.MANTRA,
            title = "${planet.displayName} Beej Mantra",
            description = "Sacred sound vibration to invoke and strengthen ${planet.displayName}. Mantras are the safest and most universally beneficial remedy.",
            method = "Sit facing ${mantraInfo.direction}. Chant the beej mantra ${mantraInfo.minimumCount} times:\n\n\"${mantraInfo.beejMantra}\"\n\nAlternatively, chant Gayatri Mantra of ${planet.displayName}.",
            timing = mantraInfo.timing,
            duration = "Complete one mala (108 times) daily for minimum 40 days. Full count of ${mantraInfo.minimumCount} for maximum benefit.",
            planet = planet,
            priority = RemedyPriority.ESSENTIAL,
            benefits = listOf(
                "No side effects - safe for everyone",
                "Directly invokes ${planet.displayName}'s blessings",
                "Reduces karmic obstacles",
                "Creates positive mental vibrations",
                "Can be done regardless of Lagna"
            ),
            cautions = listOf(
                "Maintain physical and mental purity during practice",
                "Use Rudraksha, Tulsi, or crystal mala",
                "Avoid eating non-vegetarian food during the practice period",
                "Complete the sankalpa (vow) once started"
            ),
            mantraText = mantraInfo.beejMantra,
            mantraSanskrit = mantraInfo.beejMantraSanskrit,
            mantraCount = mantraInfo.minimumCount
        )
    }

    private fun getCharityRemedy(planet: Planet): Remedy? {
        val charityInfo = planetaryCharity[planet] ?: return null

        return Remedy(
            category = RemedyCategory.CHARITY,
            title = "${planet.displayName} Daan (Charity)",
            description = "Charitable acts to reduce ${planet.displayName}'s karmic debts and negative effects.",
            method = "Donate the following items: ${charityInfo.items.joinToString(", ")}\n\nRecipients: ${charityInfo.recipients}\n\n${charityInfo.specialInstructions}",
            timing = "Every ${charityInfo.day}, preferably ${charityInfo.timing}",
            duration = "Regular practice for lasting benefits. Especially important during ${planet.displayName}'s Dasha/Antardasha.",
            planet = planet,
            priority = RemedyPriority.HIGHLY_RECOMMENDED,
            benefits = listOf(
                "Reduces negative karma associated with ${planet.displayName}",
                "Creates positive merit (punya)",
                "No side effects - universally beneficial",
                "Helps both giver and receiver"
            ),
            cautions = listOf(
                "Donate with pure intentions and humility",
                "Give without expecting returns or recognition",
                "Choose genuine recipients in need",
                "Quality matters more than quantity"
            )
        )
    }

    private fun getFastingRemedy(planet: Planet): Remedy? {
        val fastingDay = getPlanetaryWeekday(planet)

        val foodRecommendations = when (planet) {
            Planet.SUN -> "Consume wheat-based items, jaggery, and avoid salt"
            Planet.MOON -> "Consume rice, milk, white foods"
            Planet.MARS -> "Consume red lentils, avoid salt"
            Planet.MERCURY -> "Consume green gram, green vegetables"
            Planet.JUPITER -> "Consume chana dal, yellow foods, avoid salt"
            Planet.VENUS -> "Consume rice, milk, sweet foods"
            Planet.SATURN -> "Consume black gram, sesame, avoid salt"
            Planet.RAHU -> "Observe complete fast or consume only fruits"
            Planet.KETU -> "Observe complete fast or consume only milk"
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.FASTING,
            title = "$fastingDay Vrat (Fast)",
            description = "Fasting to appease ${planet.displayName} and reduce its negative influences.",
            method = "Observe fast from sunrise to sunset on $fastingDay. $foodRecommendations. Break fast after seeing the relevant symbol (Moon for Monday, etc.).",
            timing = "Every $fastingDay or for 21 continuous ${fastingDay}s",
            duration = "Minimum 11-21 weeks, ideally 40 days continuously",
            planet = planet,
            priority = RemedyPriority.RECOMMENDED,
            benefits = listOf(
                "Purifies body, mind, and spirit",
                "Increases willpower and discipline",
                "Pleases ${planet.displayName}",
                "Reduces karmic burden"
            ),
            cautions = listOf(
                "Consult a doctor if you have diabetes, low BP, or other health conditions",
                "Pregnant women should avoid strict fasting",
                "Break fast gently with light food",
                "Stay hydrated with water"
            )
        )
    }

    private fun getColorRemedy(planet: Planet): Remedy? {
        val (colors, avoidColors) = when (planet) {
            Planet.SUN -> "Orange, Red, Ruby Red, Gold, Saffron" to "Blue, Black"
            Planet.MOON -> "White, Silver, Light Blue, Cream, Pearl White" to "Red, Black"
            Planet.MARS -> "Red, Maroon, Coral Red, Scarlet" to "Blue, Green"
            Planet.MERCURY -> "Green, Light Green, Emerald Green, Parrot Green" to "Red"
            Planet.JUPITER -> "Yellow, Gold, Turmeric, Saffron, Orange-Yellow" to "Blue, Black"
            Planet.VENUS -> "White, Pink, Light Colors, Cream, Pastel shades" to "Red, Blue"
            Planet.SATURN -> "Blue, Black, Dark Blue, Navy, Violet" to "Red, Orange"
            Planet.RAHU -> "Smoke Blue, Electric Blue, Multi-colored" to "Pure White"
            Planet.KETU -> "Gray, Brown, Smoky, Earthy tones" to "Bright colors"
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.COLOR,
            title = "${planet.displayName} Color Therapy",
            description = "Use colors associated with ${planet.displayName} to create favorable energy vibrations.",
            method = "Wear clothes of these colors: $colors\n\nUse these colors in your room, workspace, and personal accessories.\n\nAvoid: $avoidColors on ${getPlanetaryWeekday(planet)}",
            timing = "Especially on ${getPlanetaryWeekday(planet)}",
            duration = "Daily practice, particularly during ${planet.displayName}'s Dasha period",
            planet = planet,
            priority = RemedyPriority.OPTIONAL,
            benefits = listOf(
                "Subtle but effective energy enhancement",
                "Easy to implement daily",
                "No cost or side effects",
                "Creates favorable vibrations"
            ),
            cautions = listOf(
                "Balance with colors for other weak planets",
                "Avoid if contraindicated by other planetary positions"
            )
        )
    }

    private fun getLifestyleRemedy(planet: Planet): Remedy? {
        val (practices, avoid) = when (planet) {
            Planet.SUN -> Pair(
                listOf(
                    "Wake before sunrise and perform Surya Namaskar",
                    "Offer water (Arghya) to Sun at sunrise",
                    "Spend time in natural sunlight (morning hours)",
                    "Respect father, authority figures, and government officials",
                    "Develop leadership qualities with humility",
                    "Eat meals before sunset when possible"
                ),
                                listOf("Avoid disrespecting father or superiors", "Avoid excessive ego or arrogance")
            )
            Planet.MOON -> Pair(
                listOf(
                    "Drink plenty of water and milk",
                    "Meditate near water bodies (rivers, lakes, sea)",
                    "Respect mother and maternal figures",
                    "Practice emotional balance and mindfulness",
                    "Keep white flowers in your room",
                    "Observe Poornima (full moon) by fasting or worship",
                    "Avoid arguments, especially with mother"
                ),
                listOf("Avoid suppressing emotions", "Avoid staying awake late at night")
            )
            Planet.MARS -> Pair(
                listOf(
                    "Exercise regularly (physical activity is essential)",
                    "Practice martial arts, yoga, or sports",
                    "Channel energy positively through constructive work",
                    "Help younger siblings and protect the weak",
                    "Donate blood if medically fit",
                    "Recite Hanuman Chalisa daily",
                    "Maintain celibacy on Tuesdays"
                ),
                listOf("Avoid anger and aggression", "Avoid violence and conflicts", "Avoid excessive spicy food")
            )
            Planet.MERCURY -> Pair(
                listOf(
                    "Read books and acquire new knowledge daily",
                    "Practice clear and honest communication",
                    "Learn a new skill or language",
                    "Write daily - journal, notes, or creative writing",
                    "Help students and support education",
                    "Keep your living space organized",
                    "Practice numeracy - solve puzzles or calculations"
                ),
                listOf("Avoid lies and gossip", "Avoid scattering energy on too many tasks")
            )
            Planet.JUPITER -> Pair(
                listOf(
                    "Study scriptures and philosophical texts",
                    "Respect teachers, elders, and Brahmins",
                    "Practice generosity and charity",
                    "Maintain ethical conduct in all dealings",
                    "Visit temples, especially on Thursdays",
                    "Apply turmeric tilak on forehead",
                    "Teach or mentor others when possible"
                ),
                listOf("Avoid disrespecting teachers or elders", "Avoid unethical financial dealings")
            )
            Planet.VENUS -> Pair(
                listOf(
                    "Appreciate arts, music, and beauty",
                    "Maintain harmony in relationships",
                    "Practice gratitude daily",
                    "Keep your surroundings beautiful and clean",
                    "Respect women and feminine energy",
                    "Wear clean, pleasant-smelling clothes",
                    "Use perfumes and flowers in your space"
                ),
                listOf("Avoid excessive sensual indulgence", "Avoid disrespecting spouse or partner")
            )
            Planet.SATURN -> Pair(
                listOf(
                    "Practice discipline and routine strictly",
                    "Serve the elderly, poor, and disabled",
                    "Be patient - avoid shortcuts",
                    "Work hard and consistently",
                    "Help servants and laborers",
                    "Feed crows, especially on Saturdays",
                    "Visit Shani temple or recite Shani Stotram",
                    "Keep your workspace clean and organized"
                ),
                listOf("Avoid laziness and procrastination", "Avoid disrespecting servants or workers", "Avoid alcohol and intoxicants")
            )
            Planet.RAHU -> Pair(
                listOf(
                    "Avoid shortcuts and get-rich-quick schemes",
                    "Practice honesty in all dealings",
                    "Reduce materialistic desires",
                    "Avoid intoxicants completely",
                    "Stay grounded - avoid getting carried away",
                    "Donate to outcasts and marginalized people",
                    "Keep your thoughts positive"
                ),
                listOf("Avoid deception and manipulation", "Avoid excessive ambition", "Avoid foreign travel during Rahu Dasha without proper remedies")
            )
            Planet.KETU -> Pair(
                listOf(
                    "Meditate daily - at least 15-30 minutes",
                    "Practice detachment from material outcomes",
                    "Pursue spiritual knowledge",
                    "Feed dogs, especially stray dogs",
                    "Visit Ganesha temples",
                    "Study ancient wisdom and occult sciences",
                    "Practice solitude and introspection"
                ),
                listOf("Avoid excessive attachment to outcomes", "Avoid ignoring spiritual calling")
            )
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.LIFESTYLE,
            title = "${planet.displayName} Lifestyle Practices",
            description = "Daily habits and lifestyle modifications to strengthen ${planet.displayName} and reduce its negative effects.",
            method = buildString {
                appendLine("RECOMMENDED PRACTICES:")
                practices.forEachIndexed { index, practice ->
                    appendLine("${index + 1}. $practice")
                }
                appendLine()
                appendLine("THINGS TO AVOID:")
                avoid.forEach { item ->
                    appendLine("• $item")
                }
            },
            timing = "Daily practice, especially on ${getPlanetaryWeekday(planet)}",
            duration = "Ongoing lifestyle adoption",
            planet = planet,
            priority = RemedyPriority.RECOMMENDED,
            benefits = listOf(
                "Holistic and sustainable improvement",
                "No cost involved",
                "Creates positive karma",
                "Aligns daily life with planetary energies"
            ),
            cautions = emptyList()
        )
    }

    private fun getRudrakshaRemedy(planet: Planet): Remedy? {
        val (mukhi, deity, benefits) = when (planet) {
            Planet.SUN -> Triple(
                "12 Mukhi (12 faces)",
                "Lord Surya, Lord Vishnu",
                listOf("Enhances leadership", "Boosts confidence", "Improves health", "Success in government matters")
            )
            Planet.MOON -> Triple(
                "2 Mukhi",
                "Ardhanarishwara (Shiva-Shakti)",
                listOf("Emotional balance", "Mental peace", "Harmonious relationships", "Cures mood disorders")
            )
            Planet.MARS -> Triple(
                "3 Mukhi",
                "Agni Dev (Fire God)",
                listOf("Removes lethargy", "Increases energy", "Boosts courage", "Overcomes fear and guilt")
            )
            Planet.MERCURY -> Triple(
                "4 Mukhi",
                "Lord Brahma",
                listOf("Enhances intelligence", "Improves communication", "Academic success", "Creative abilities")
            )
            Planet.JUPITER -> Triple(
                "5 Mukhi",
                "Lord Shiva (Kalagni Rudra)",
                listOf("Spiritual growth", "Wisdom enhancement", "Good fortune", "Removes sins")
            )
            Planet.VENUS -> Triple(
                "6 Mukhi",
                "Lord Kartikeya",
                listOf("Love and relationships", "Artistic abilities", "Material comforts", "Removes obstacles in marriage")
            )
            Planet.SATURN -> Triple(
                "7 Mukhi",
                "Goddess Mahalakshmi",
                listOf("Wealth and prosperity", "Removes poverty", "Good fortune", "Success after struggle")
            )
            Planet.RAHU -> Triple(
                "8 Mukhi",
                "Lord Ganesha",
                listOf("Removes obstacles", "Protection from enemies", "Success in endeavors", "Removes Rahu dosha")
            )
            Planet.KETU -> Triple(
                "9 Mukhi",
                "Goddess Durga",
                listOf("Spiritual liberation", "Removes fear", "Protection from negativity", "Removes Ketu dosha")
            )
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.RUDRAKSHA,
            title = "$mukhi Rudraksha",
            description = "Sacred Rudraksha bead associated with ${planet.displayName} and blessed by $deity.",
            method = buildString {
                appendLine("1. Obtain authentic $mukhi Rudraksha from a reputable source")
                appendLine("2. Purify with Panchamrit (milk, curd, honey, ghee, sugar) or Gangajal")
                appendLine("3. Energize by chanting: 'Om Namah Shivaya' 108 times")
                appendLine("4. Chant ${planet.displayName} mantra 108 times before first wearing")
                appendLine("5. Wear on silk/wool thread or cap in silver/gold")
                appendLine("6. Wear around neck or on right wrist")
            },
            timing = "Wear on ${getPlanetaryWeekday(planet)} during Shukla Paksha, preferably on ${planet.displayName} Hora",
            duration = "Continuous wear. Remove during impure activities.",
            planet = planet,
            priority = RemedyPriority.RECOMMENDED,
            benefits = benefits + listOf(
                "Natural remedy with no side effects",
                "Balances ${planet.displayName}'s energy",
                "Provides spiritual protection",
                "Can be worn by anyone regardless of Lagna"
            ),
            cautions = listOf(
                "Ensure authenticity - buy from certified dealers",
                "Remove during sleep if uncomfortable",
                "Clean regularly with water",
                "Avoid wearing during cremation or birth events"
            )
        )
    }

    private fun getYantraRemedy(planet: Planet): Remedy? {
        val (yantraName, description, material) = when (planet) {
            Planet.SUN -> Triple(
                "Surya Yantra",
                "Geometric representation of Sun's energy, brings fame, authority, and government favor",
                "Copper or Gold"
            )
            Planet.MOON -> Triple(
                "Chandra Yantra",
                "Represents lunar energy, brings mental peace, emotional stability, and public favor",
                "Silver"
            )
            Planet.MARS -> Triple(
                "Mangal Yantra",
                "Represents Mars energy, brings courage, property success, and victory over enemies",
                "Copper"
            )
            Planet.MERCURY -> Triple(
                "Budh Yantra",
                "Represents Mercury's energy, brings intelligence, business success, and communication skills",
                "Bronze or Copper"
            )
            Planet.JUPITER -> Triple(
                "Brihaspati Yantra / Guru Yantra",
                "Represents Jupiter's energy, brings wisdom, wealth, children, and spiritual growth",
                "Gold or Brass"
            )
            Planet.VENUS -> Triple(
                "Shukra Yantra",
                "Represents Venus energy, brings love, beauty, luxury, and artistic success",
                "Silver or Copper"
            )
            Planet.SATURN -> Triple(
                "Shani Yantra",
                "Represents Saturn's energy, removes obstacles, brings discipline and success through persistence",
                "Iron or Steel (Panch Dhatu)"
            )
            Planet.RAHU -> Triple(
                "Rahu Yantra",
                "Represents Rahu's energy, brings worldly success, removes fear, and protects from black magic",
                "Lead or Ashtadhatu"
            )
            Planet.KETU -> Triple(
                "Ketu Yantra",
                "Represents Ketu's energy, brings spiritual liberation, removes past karma, and enhances intuition",
                "Ashtadhatu or Copper"
            )
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.YANTRA,
            title = yantraName,
            description = description,
            method = buildString {
                appendLine("INSTALLATION PROCEDURE:")
                appendLine("1. Choose a $material $yantraName from authentic source")
                appendLine("2. Select auspicious day: ${getPlanetaryWeekday(planet)} during ${planet.displayName} Hora")
                appendLine("3. Bathe the Yantra with Panchamrit and Gangajal")
                appendLine("4. Place on clean altar facing East or direction of ${planet.displayName}")
                appendLine("5. Light incense and lamp")
                appendLine("6. Chant ${planet.displayName} mantra 108 times")
                appendLine("7. Offer flowers, fruits, and sandalwood")
                appendLine("8. Perform daily worship with incense and lamp")
            },
            timing = "Install on ${getPlanetaryWeekday(planet)} during ${planet.displayName} Hora in Shukla Paksha",
            duration = "Permanent installation with daily worship",
            planet = planet,
            priority = RemedyPriority.OPTIONAL,
            benefits = listOf(
                "Creates continuous positive energy field",
                "Provides 24/7 remedy effect when worshipped",
                "Enhances meditation and spiritual practice",
                "Protects home and family from negative influences"
            ),
            cautions = listOf(
                "Requires daily worship commitment",
                "Must be installed with proper Vedic rituals",
                "Keep the Yantra clean and respected",
                "If unable to worship daily, consider other remedies"
            )
        )
    }

    private fun getDeityRemedy(planet: Planet): Remedy? {
        val (primaryDeity, secondaryDeities, temple, offerings) = when (planet) {
            Planet.SUN -> Quadruple(
                "Lord Surya Narayan",
                listOf("Lord Vishnu", "Lord Ram", "Lord Shiva (as Mrityunjaya)"),
                "Surya Mandir, Konark Sun Temple, any Sun temple",
                "Red flowers, wheat, jaggery, copper items"
            )
            Planet.MOON -> Quadruple(
                "Lord Shiva",
                listOf("Goddess Parvati", "Goddess Ganga", "Lord Chandra"),
                "Any Shiva temple, especially Jyotirlingas",
                "White flowers, milk, water, rice, silver items"
            )
            Planet.MARS -> Quadruple(
                "Lord Hanuman",
                listOf("Lord Kartikeya (Murugan)", "Goddess Durga", "Lord Bhairav"),
                "Hanuman temple, Mangalnath Mandir Ujjain",
                "Red flowers, red sindoor, jaggery, coconut"
            )
            Planet.MERCURY -> Quadruple(
                "Lord Vishnu",
                listOf("Lord Krishna", "Lord Buddha", "Goddess Saraswati"),
                "Vishnu temple, any Krishna temple",
                "Green items, green gram, tulsi, flowers"
            )
            Planet.JUPITER -> Quadruple(
                "Lord Brihaspati / Dakshinamurthy",
                listOf("Lord Vishnu", "Lord Shiva (as Guru)", "Lord Dattatreya"),
                "Guru temple, Vishnu temple",
                "Yellow flowers, turmeric, chana dal, bananas"
            )
            Planet.VENUS -> Quadruple(
                "Goddess Lakshmi",
                listOf("Goddess Saraswati", "Lord Krishna", "Goddess Annapurna"),
                "Lakshmi temple, any Devi temple",
                "White flowers, rice, sweets, silk, perfumes"
            )
            Planet.SATURN -> Quadruple(
                "Lord Shani Dev",
                listOf("Lord Hanuman", "Lord Bhairav", "Goddess Kali"),
                "Shani temple, Shani Shingnapur, any Shani shrine",
                "Black sesame, mustard oil, iron items, blue flowers"
            )
            Planet.RAHU -> Quadruple(
                "Goddess Durga",
                listOf("Lord Bhairav", "Goddess Kali", "Naga Devatas"),
                "Durga temple, Naga temples, Rahu shrine",
                "Coconut, blue flowers, hessonite, sandalwood"
            )
            Planet.KETU -> Quadruple(
                "Lord Ganesha",
                listOf("Chitragupta", "Lord Kartikeya", "Goddess Kali"),
                "Ganesha temple, any Ketu shrine",
                "Mixed grains, brown/gray items, dog food for charity"
            )
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.DEITY,
            title = "${planet.displayName} Deity Worship",
            description = "Invoke divine blessings by worshipping deities associated with ${planet.displayName}.",
            method = buildString {
                appendLine("PRIMARY DEITY: $primaryDeity")
                appendLine()
                appendLine("SECONDARY DEITIES: ${secondaryDeities.joinToString(", ")}")
                appendLine()
                appendLine("RECOMMENDED TEMPLES: $temple")
                appendLine()
                appendLine("WORSHIP PROCEDURE:")
                appendLine("1. Visit temple on ${getPlanetaryWeekday(planet)}")
                appendLine("2. Offer: $offerings")
                appendLine("3. Recite ${planet.displayName} stotra or mantra")
                appendLine("4. Light lamp with ghee or appropriate oil")
                appendLine("5. Perform pradakshina (circumambulation)")
                appendLine("6. Seek blessings with pure intention")
            },
            timing = "Every ${getPlanetaryWeekday(planet)}, especially during ${planet.displayName} Hora",
            duration = "Regular weekly practice, more during ${planet.displayName} Dasha",
            planet = planet,
            priority = RemedyPriority.HIGHLY_RECOMMENDED,
            benefits = listOf(
                "Divine grace and protection",
                "Karmic relief and blessings",
                "Spiritual growth and merit",
                "Direct connection with planetary deity"
            ),
            cautions = listOf(
                "Worship with pure devotion and clean body/mind",
                "Follow proper temple etiquette",
                "If unable to visit temple, perform home worship with image/idol"
            )
        )
    }

    private fun getNakshatraRemedy(analysis: PlanetaryAnalysis): Remedy? {
        if (!analysis.needsRemedy) return null

        val nakshatra = analysis.nakshatra
        val deity = nakshatraDeities[nakshatra] ?: return null

        val nakshatraRemedy = when (nakshatra) {
            Nakshatra.ASHWINI -> "Worship Ashwini Kumaras, offer white items, avoid surgery on this nakshatra days"
            Nakshatra.BHARANI -> "Worship Yama, practice dharma, donate to funeral services"
            Nakshatra.KRITTIKA -> "Worship Agni, perform homa/fire rituals, offer ghee"
            Nakshatra.ROHINI -> "Worship Brahma, offer white flowers and milk, practice creativity"
            Nakshatra.MRIGASHIRA -> "Worship Soma/Chandra, offer white items, stay near water"
            Nakshatra.ARDRA -> "Worship Lord Rudra, perform Rudra Abhisheka, offer blue flowers"
            Nakshatra.PUNARVASU -> "Worship Goddess Aditi, practice hospitality, help the needy"
            Nakshatra.PUSHYA -> "Worship Brihaspati, donate to teachers, study scriptures"
            Nakshatra.ASHLESHA -> "Worship Naga deities, offer milk to snake idols, avoid harming snakes"
            Nakshatra.MAGHA -> "Worship ancestors (Pitris), perform Shraddha rituals, respect elders"
            Nakshatra.PURVA_PHALGUNI -> "Worship Bhaga, practice generosity, enjoy arts and creativity"
            Nakshatra.UTTARA_PHALGUNI -> "Worship Aryaman, honor contracts and promises, practice charity"
            Nakshatra.HASTA -> "Worship Savitar (Sun), practice healing arts, offer sunflowers"
            Nakshatra.CHITRA -> "Worship Vishwakarma, practice craftsmanship, donate tools"
            Nakshatra.SWATI -> "Worship Vayu, practice pranayama, offer incense"
            Nakshatra.VISHAKHA -> "Worship Indra-Agni, practice determination, perform fire rituals"
            Nakshatra.ANURADHA -> "Worship Mitra, cultivate friendships, practice devotion"
            Nakshatra.JYESHTHA -> "Worship Indra, develop leadership, protect the weak"
            Nakshatra.MULA -> "Worship Goddess Nirriti/Kali, practice detachment, donate roots/herbs"
            Nakshatra.PURVA_ASHADHA -> "Worship Apas (water deities), stay near water, practice purification"
            Nakshatra.UTTARA_ASHADHA -> "Worship Vishvadevas, practice universal service, be helpful to all"
            Nakshatra.SHRAVANA -> "Worship Lord Vishnu, practice listening and learning, chant Vishnu mantras"
            Nakshatra.DHANISHTHA -> "Worship Vasus, practice music, donate musical instruments"
            Nakshatra.SHATABHISHA -> "Worship Varuna, practice healing, work with herbs and medicines"
            Nakshatra.PURVA_BHADRAPADA -> "Worship Aja Ekapad, practice yoga and meditation, seek spiritual liberation"
            Nakshatra.UTTARA_BHADRAPADA -> "Worship Ahir Budhnya, practice deep meditation, seek wisdom"
            Nakshatra.REVATI -> "Worship Pushan, practice guidance and protection, help travelers"
        }

        return Remedy(
            category = RemedyCategory.DEITY,
            title = "${nakshatra.displayName} Nakshatra Remedy",
            description = "${analysis.planet.displayName} is placed in ${nakshatra.displayName} nakshatra. This nakshatra is ruled by $deity.",
            method = nakshatraRemedy,
            timing = "Perform especially when Moon transits ${nakshatra.displayName}",
            duration = "Regular practice, especially during relevant nakshatra days",
            planet = analysis.planet,
            priority = RemedyPriority.RECOMMENDED,
            benefits = listOf(
                "Addresses nakshatra-specific influences",
                "Invokes blessing of nakshatra deity",
                "Complements planetary remedies",
                "Fine-tunes remedy approach"
            ),
            cautions = listOf(
                "Combine with main planetary remedies for best results",
                "Check Panchang for nakshatra transit days"
            ),
            nakshatraSpecific = true
        )
    }

    private fun getGandantaRemedy(analysis: PlanetaryAnalysis): Remedy? {
        if (!analysis.isInGandanta) return null

        val gandantaType = when (analysis.sign) {
            ZodiacSign.CANCER, ZodiacSign.LEO -> "Cancer-Leo (Ashlesha-Magha)"
            ZodiacSign.SCORPIO, ZodiacSign.SAGITTARIUS -> "Scorpio-Sagittarius (Jyeshtha-Mula)"
            ZodiacSign.PISCES, ZodiacSign.ARIES -> "Pisces-Aries (Revati-Ashwini)"
            else -> return null
        }

        return Remedy(
            category = RemedyCategory.DEITY,
            title = "Gandanta Dosha Remedy",
            description = "${analysis.planet.displayName} is in Gandanta zone ($gandantaType). This is a karmic junction requiring special attention.",
            method = buildString {
                appendLine("GANDANTA REMEDIES:")
                appendLine("1. Perform Shanti Puja for ${analysis.planet.displayName}")
                appendLine("2. Chant ${analysis.planet.displayName} mantra 11,000 times")
                appendLine("3. Donate items related to ${analysis.planet.displayName}")
                appendLine("4. Perform Mrityunjaya Japa (108 times daily for 40 days)")
                appendLine("5. Donate to orphanages or old age homes")
                appendLine("6. Feed Brahmins on ${getPlanetaryWeekday(analysis.planet)}")
                appendLine("7. Worship Lord Ganesha to remove obstacles")
                appendLine()
                appendLine("SPECIAL: Light a ghee lamp daily before the planet's deity")
            },
            timing = "Begin on auspicious day, preferably ${getPlanetaryWeekday(analysis.planet)}",
            duration = "40 days minimum, ideally during ${analysis.planet.displayName} Dasha",
            planet = analysis.planet,
            priority = RemedyPriority.ESSENTIAL,
            benefits = listOf(
                "Removes karmic blockages from past lives",
                "Reduces difficulties in the area ruled by ${analysis.planet.displayName}",
                "Transforms challenging energy into spiritual growth",
                "Provides protection during difficult periods"
            ),
            cautions = listOf(
                "Gandanta requires consistent long-term remedies",
                "Consult a qualified astrologer for personalized guidance",
                "Do not skip or abandon the remedy once started"
            )
        )
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun generateGeneralRecommendations(
        chart: VedicChart,
        analyses: List<PlanetaryAnalysis>,
        ascendantSign: ZodiacSign
    ): List<String> {
        val recommendations = mutableListOf<String>()

        val weakCount = analyses.count { it.needsRemedy }
        if (weakCount >= 5) {
            recommendations.add("Multiple planets (${weakCount}) need attention. Focus on the 2-3 most afflicted planets first. Start with mantras and charity as they have no side effects.")
        }

        val sunAnalysis = analyses.find { it.planet == Planet.SUN }
        val moonAnalysis = analyses.find { it.planet == Planet.MOON }

        if (sunAnalysis?.needsRemedy == true && moonAnalysis?.needsRemedy == true) {
            recommendations.add("Both luminaries (Sun and Moon) are weak. Prioritize: Surya Namaskar at sunrise, drink water from silver glass, and practice meditation for emotional balance.")
        }

        if (moonAnalysis?.needsRemedy == true) {
            recommendations.add("Moon remedies are essential for mental peace. Keep a bowl of water near your bed at night and offer it to plants in the morning.")
        }

        analyses.filter { it.isInGandanta }.forEach { analysis ->
            recommendations.add("${analysis.planet.displayName} is in Gandanta (karmic knot). This requires dedicated long-term remedial measures including regular Mrityunjaya Japa.")
        }

        analyses.filter { it.hasNeechaBhangaRajaYoga }.forEach { analysis ->
            recommendations.add("${analysis.planet.displayName} has Neecha Bhanga Raja Yoga - initial struggles will transform into success. Stay patient and persistent.")
        }

        analyses.filter { it.isYogakaraka }.forEach { analysis ->
            recommendations.add("${analysis.planet.displayName} is your Yogakaraka planet. Strengthening it through gemstone, mantra, and lifestyle will bring significant positive results.")
        }

        recommendations.add("Practice daily meditation for at least 15 minutes to harmonize all planetary energies.")
        recommendations.add("Maintain a clean and organized living space, especially the puja area and entrance of your home.")
        recommendations.add("Respect elders, teachers, and parents - this strengthens Jupiter and reduces many karmic burdens.")
        recommendations.add("Perform regular charity based on your means - even small acts of giving create positive karma.")
        recommendations.add("Avoid non-vegetarian food and alcohol on days when performing planetary remedies.")
        recommendations.add("Keep a dream journal - dreams often provide insights about which planets need attention.")

        val ketuAnalysis = analyses.find { it.planet == Planet.KETU }
        if (ketuAnalysis?.housePosition == 12 || ketuAnalysis?.housePosition == 4) {
            recommendations.add("Strong spiritual potential indicated. Consider regular meditation retreats and spiritual study.")
        }

        return recommendations
    }

    private fun generateDashaRemedies(
        chart: VedicChart,
        analyses: List<PlanetaryAnalysis>
    ): List<Remedy> {
        val dashaRemedies = mutableListOf<Remedy>()

        dashaRemedies.add(
            Remedy(
                category = RemedyCategory.LIFESTYLE,
                title = "Dasha Period Awareness",
                description = "General guidance for navigating planetary Dasha periods effectively.",
                method = buildString {
                    appendLine("DURING ANY DASHA PERIOD:")
                    appendLine("1. Strengthen the Dasha lord through appropriate remedies")
                    appendLine("2. If Dasha lord is weak in your chart, increase mantra and charity")
                    appendLine("3. Observe the weekday of the Dasha lord with fasting or worship")
                    appendLine("4. Avoid major decisions during Dasha lord's retrograde")
                    appendLine("5. Wear the appropriate color on the Dasha lord's day")
                    appendLine()
                    appendLine("ANTARDASHA (Sub-period) GUIDANCE:")
                    appendLine("• Check the relationship between Dasha and Antardasha lords")
                    appendLine("• If they are friends - period will be favorable")
                    appendLine("• If they are enemies - increase remedies for both planets")
                    appendLine("• Pratyantar Dasha (sub-sub period) shows fine-tuned timing")
                },
                timing = "Throughout the Dasha period",
                duration = "Entire Dasha duration",
                planet = null,
                priority = RemedyPriority.RECOMMENDED,
                benefits = listOf(
                    "Maximizes positive effects of favorable Dashas",
                    "Minimizes challenges during difficult Dashas",
                    "Provides timing awareness for important decisions"
                ),
                cautions = listOf(
                    "Consult a Vedic astrologer for specific Dasha analysis",
                    "Dasha effects are modified by transit influences"
                )
            )
        )

        analyses.filter { it.strength.severity >= 4 }.forEach { analysis ->
            dashaRemedies.add(
                Remedy(
                    category = RemedyCategory.MANTRA,
                    title = "During ${analysis.planet.displayName} Dasha/Antardasha",
                    description = "Special remedies to perform when ${analysis.planet.displayName} period is running.",
                    method = "Since ${analysis.planet.displayName} is weak in your chart (${analysis.strength.displayName}), during its Dasha:\n\n1. Chant its mantra daily (108 times minimum)\n2. Perform charity on ${getPlanetaryWeekday(analysis.planet)}\n3. Wear ${analysis.planet.displayName}'s colors\n4. Fast on ${getPlanetaryWeekday(analysis.planet)} if possible\n5. Worship the ruling deity regularly",
                    timing = "During ${analysis.planet.displayName} Mahadasha or Antardasha",
                    duration = "Throughout the Dasha period",
                    planet = analysis.planet,
                    priority = RemedyPriority.ESSENTIAL,
                    benefits = listOf(
                        "Reduces difficulties during challenging Dasha",
                        "Transforms karmic lessons into growth opportunities"
                    ),
                    cautions = listOf(
                        "Be especially careful during Sade Sati (Saturn's 7.5 year transit over Moon)"
                    ),
                    mantraText = planetaryMantras[analysis.planet]?.beejMantra,
                    mantraSanskrit = planetaryMantras[analysis.planet]?.beejMantraSanskrit
                )
            )
        }

        return dashaRemedies
    }

    private fun categorizeByLifeArea(
        remedies: List<Remedy>,
        analyses: List<PlanetaryAnalysis>
    ): Map<String, List<Remedy>> {
        return mapOf(
            "Career & Authority" to remedies.filter {
                it.planet in listOf(Planet.SUN, Planet.SATURN, Planet.JUPITER, Planet.MARS)
            },
            "Relationships & Marriage" to remedies.filter {
                it.planet in listOf(Planet.VENUS, Planet.MOON, Planet.JUPITER)
            },
            "Health & Vitality" to remedies.filter {
                it.planet in listOf(Planet.SUN, Planet.MOON, Planet.MARS, Planet.SATURN)
            },
            "Wealth & Finance" to remedies.filter {
                it.planet in listOf(Planet.JUPITER, Planet.VENUS, Planet.MERCURY, Planet.MOON)
            },
            "Education & Communication" to remedies.filter {
                it.planet in listOf(Planet.MERCURY, Planet.JUPITER)
            },
            "Spiritual Growth" to remedies.filter {
                it.planet in listOf(Planet.KETU, Planet.JUPITER, Planet.MOON, Planet.SUN)
            },
            "Property & Fixed Assets" to remedies.filter {
                it.planet in listOf(Planet.MARS, Planet.SATURN, Planet.MOON)
            },
            "Foreign & Travel" to remedies.filter {
                it.planet in listOf(Planet.RAHU, Planet.KETU, Planet.MOON)
            }
        ).filterValues { it.isNotEmpty() }
    }

    private fun prioritizeRemedies(
        remedies: List<Remedy>,
        analyses: List<PlanetaryAnalysis>,
        chart: VedicChart
    ): List<Remedy> {
        return remedies.sortedWith(
            compareBy<Remedy> { it.priority.level }
                .thenByDescending { remedy ->
                    val analysis = analyses.find { it.planet == remedy.planet }
                    analysis?.strength?.severity ?: 0
                }
                .thenBy { remedy ->
                    when (remedy.category) {
                        RemedyCategory.MANTRA -> 1
                        RemedyCategory.CHARITY -> 2
                        RemedyCategory.DEITY -> 3
                        RemedyCategory.FASTING -> 4
                        RemedyCategory.LIFESTYLE -> 5
                        RemedyCategory.RUDRAKSHA -> 6
                        RemedyCategory.COLOR -> 7
                        RemedyCategory.GEMSTONE -> 8
                        RemedyCategory.YANTRA -> 9
                        RemedyCategory.METAL -> 10
                    }
                }
        )
    }

    private fun generateSummary(
        analyses: List<PlanetaryAnalysis>,
        weakPlanets: List<Planet>,
        ascendantSign: ZodiacSign
    ): String {
        return buildString {
            appendLine("REMEDIES SUMMARY FOR ${ascendantSign.displayName} ASCENDANT")
            appendLine()

            if (weakPlanets.isEmpty()) {
                appendLine("Your planetary positions are generally favorable. Continue with positive practices to maintain balance.")
                appendLine()
                appendLine("RECOMMENDED MAINTENANCE PRACTICES:")
                appendLine("• Daily meditation and pranayama")
                appendLine("• Weekly temple visits")
                appendLine("• Regular charity and service")
                appendLine("• Ethical living and positive thinking")
            } else {
                appendLine("PRIMARY FOCUS PLANETS: ${weakPlanets.take(3).joinToString { it.displayName }}")
                appendLine()

                appendLine("REMEDY PRIORITY ORDER:")
                appendLine()
                appendLine("1. MANTRAS (Start here - safest, most effective)")
                appendLine("   • No side effects, can be done by anyone")
                appendLine("   • Chant daily, preferably 108 times or more")
                appendLine("   • Best performed during brahma muhurta (before sunrise)")
                appendLine()
                appendLine("2. CHARITY (Universal benefit)")
                appendLine("   • Donate on the planet's weekday")
                appendLine("   • Give with pure intention without expectation")
                appendLine()
                appendLine("3. FASTING (Powerful purification)")
                appendLine("   • Observe on the planet's day")
                appendLine("   • Even partial fasting is beneficial")
                appendLine()
                appendLine("4. DEITY WORSHIP (Divine connection)")
                appendLine("   • Visit relevant temples regularly")
                appendLine("   • Perform home worship with devotion")
                appendLine()
                appendLine("5. GEMSTONES (Use with caution)")
                appendLine("   • Only for functional benefics of your Lagna")
                appendLine("   • Consult an expert before wearing")
                appendLine("   • Always observe trial period first")
                appendLine()

                val yogakarakaPlanets = analyses.filter { it.isYogakaraka }
                if (yogakarakaPlanets.isNotEmpty()) {
                    appendLine("IMPORTANT: ${yogakarakaPlanets.map { it.planet.displayName }.joinToString()} is/are your Yogakaraka planet(s). Strengthening ${if (yogakarakaPlanets.size == 1) "this planet" else "these planets"} will bring maximum positive results.")
                    appendLine()
                }

                appendLine("GENERAL GUIDANCE:")
                appendLine("• Start with one or two remedies, then gradually add more")
                appendLine("• Consistency is key - regular practice beats occasional intensity")
                appendLine("• Remedies work best with positive attitude and good deeds")
                appendLine("• Results manifest according to karmic timing")
                appendLine("• During difficult Dashas, increase remedy intensity")
            }
        }
    }

    private fun getPlanetaryWeekday(planet: Planet): String {
        return when (planet) {
            Planet.SUN -> "Sunday"
            Planet.MOON -> "Monday"
            Planet.MARS -> "Tuesday"
            Planet.MERCURY -> "Wednesday"
            Planet.JUPITER -> "Thursday"
            Planet.VENUS -> "Friday"
            Planet.SATURN -> "Saturday"
            Planet.RAHU -> "Saturday"
            Planet.KETU -> "Tuesday"
            else -> "Sunday"
        }
    }

    /**
     * Get localized planetary weekday
     */
    fun getLocalizedPlanetaryWeekday(planet: Planet, language: Language): String {
        val key = when (planet) {
            Planet.SUN -> StringKeyDosha.PLANET_DAY_SUN
            Planet.MOON -> StringKeyDosha.PLANET_DAY_MOON
            Planet.MARS -> StringKeyDosha.PLANET_DAY_MARS
            Planet.MERCURY -> StringKeyDosha.PLANET_DAY_MERCURY
            Planet.JUPITER -> StringKeyDosha.PLANET_DAY_JUPITER
            Planet.VENUS -> StringKeyDosha.PLANET_DAY_VENUS
            Planet.SATURN -> StringKeyDosha.PLANET_DAY_SATURN
            Planet.RAHU -> StringKeyDosha.PLANET_DAY_RAHU
            Planet.KETU -> StringKeyDosha.PLANET_DAY_KETU
            else -> StringKeyDosha.PLANET_DAY_SUN
        }
        return StringResources.get(key, language)
    }

    private fun getPlanetLifeArea(planet: Planet): String {
        return when (planet) {
            Planet.SUN -> "authority, career, government favor, father's health, self-confidence"
            Planet.MOON -> "mind, emotions, mother, public image, mental peace"
            Planet.MARS -> "energy, courage, property, siblings, physical strength"
            Planet.MERCURY -> "communication, business, education, intelligence, writing"
            Planet.JUPITER -> "wisdom, wealth, children, fortune, higher education, spirituality"
            Planet.VENUS -> "love, marriage, beauty, arts, luxury, relationships"
            Planet.SATURN -> "discipline, longevity, career stability, hard work, patience"
            Planet.RAHU -> "foreign connections, technology, unconventional success, ambition"
            Planet.KETU -> "spirituality, liberation, intuition, past life karma, healing"
            else -> "general well-being"
        }
    }

    /**
     * Get localized planet life area description
     */
    fun getLocalizedPlanetLifeArea(planet: Planet, language: Language): String {
        val key = when (planet) {
            Planet.SUN -> StringKeyDosha.PLANET_LIFE_AREA_SUN
            Planet.MOON -> StringKeyDosha.PLANET_LIFE_AREA_MOON
            Planet.MARS -> StringKeyDosha.PLANET_LIFE_AREA_MARS
            Planet.MERCURY -> StringKeyDosha.PLANET_LIFE_AREA_MERCURY
            Planet.JUPITER -> StringKeyDosha.PLANET_LIFE_AREA_JUPITER
            Planet.VENUS -> StringKeyDosha.PLANET_LIFE_AREA_VENUS
            Planet.SATURN -> StringKeyDosha.PLANET_LIFE_AREA_SATURN
            Planet.RAHU -> StringKeyDosha.PLANET_LIFE_AREA_RAHU
            Planet.KETU -> StringKeyDosha.PLANET_LIFE_AREA_KETU
            else -> return StringResources.get(StringKeyDosha.LABEL_UNKNOWN, language)
        }
        return StringResources.get(key, language)
    }
}