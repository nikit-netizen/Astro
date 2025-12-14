package com.astro.storm.ephemeris

import android.content.Context
import com.astro.storm.data.model.Nakshatra
import com.astro.storm.data.model.Planet
import swisseph.DblObj
import swisseph.SweConst
import swisseph.SweDate
import swisseph.SwissEph
import java.io.Closeable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor

class PanchangaCalculator(context: Context) : Closeable {

    private val swissEph: SwissEph = SwissEph()
    private val ephemerisPath: String
    @Volatile
    private var isClosed: Boolean = false

    companion object {
        private const val AYANAMSA_LAHIRI = SweConst.SE_SIDM_LAHIRI

        private const val TITHI_SPAN = 12.0
        private const val NAKSHATRA_SPAN = 360.0 / 27.0
        private const val YOGA_SPAN = 360.0 / 27.0
        private const val KARANA_SPAN = 6.0
        private const val PADA_SPAN = NAKSHATRA_SPAN / 4.0

        private const val TOTAL_TITHIS = 30
        private const val TOTAL_NAKSHATRAS = 27
        private const val TOTAL_YOGAS = 27
        private const val TOTAL_KARANAS = 60
        private const val MOVABLE_KARANAS = 7
        private const val MOVABLE_KARANA_CYCLES = 8
    }

    init {
        ephemerisPath = context.filesDir.absolutePath + "/ephe"
        swissEph.swe_set_ephe_path(ephemerisPath)
        swissEph.swe_set_sid_mode(AYANAMSA_LAHIRI, 0.0, 0.0)
    }

    fun calculatePanchanga(
        dateTime: LocalDateTime,
        latitude: Double,
        longitude: Double,
        timezone: String
    ): PanchangaData {
        check(!isClosed) { "PanchangaCalculator has been closed" }

        require(latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90 degrees, got $latitude"
        }
        require(longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180 degrees, got $longitude"
        }

        val zoneId = try {
            ZoneId.of(timezone)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid timezone: $timezone", e)
        }

        val zonedDateTime = ZonedDateTime.of(dateTime, zoneId)
        val utcDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"))

        val julianDay = calculateJulianDay(
            utcDateTime.year,
            utcDateTime.monthValue,
            utcDateTime.dayOfMonth,
            utcDateTime.hour,
            utcDateTime.minute,
            utcDateTime.second
        )

        val sunLongitudeSidereal = getPlanetLongitude(SweConst.SE_SUN, julianDay, sidereal = true)
        val moonLongitudeSidereal = getPlanetLongitude(SweConst.SE_MOON, julianDay, sidereal = true)

        val sunLongitudeTropical = getPlanetLongitude(SweConst.SE_SUN, julianDay, sidereal = false)
        val moonLongitudeTropical = getPlanetLongitude(SweConst.SE_MOON, julianDay, sidereal = false)

        val tithi = calculateTithi(sunLongitudeSidereal, moonLongitudeSidereal)
        val nakshatra = calculateNakshatra(moonLongitudeSidereal)
        val yoga = calculateYoga(sunLongitudeSidereal, moonLongitudeSidereal)
        val karana = calculateKarana(sunLongitudeSidereal, moonLongitudeSidereal)
        val vara = calculateVara(julianDay)
        val paksha = determinePaksha(tithi.number)

        val (sunrise, sunset) = calculateSunriseSunset(julianDay, latitude, longitude, zoneId)
        val moonPhase = calculateMoonPhase(sunLongitudeTropical, moonLongitudeTropical)

        val ayanamsa = swissEph.swe_get_ayanamsa_ut(julianDay)

        return PanchangaData(
            tithi = tithi,
            nakshatra = nakshatra,
            yoga = yoga,
            karana = karana,
            vara = vara,
            paksha = paksha,
            sunrise = sunrise,
            sunset = sunset,
            moonPhase = moonPhase,
            sunLongitude = sunLongitudeSidereal,
            moonLongitude = moonLongitudeSidereal,
            ayanamsa = ayanamsa
        )
    }

    private fun getPlanetLongitude(planetId: Int, julianDay: Double, sidereal: Boolean): Double {
        val positions = DoubleArray(6)
        val errorBuffer = StringBuffer()

        val flags = if (sidereal) {
            SweConst.SEFLG_SIDEREAL or SweConst.SEFLG_SPEED or SweConst.SEFLG_SWIEPH
        } else {
            SweConst.SEFLG_SPEED or SweConst.SEFLG_SWIEPH
        }

        val result = swissEph.swe_calc_ut(julianDay, planetId, flags, positions, errorBuffer)

        if (result < 0) {
            throw PanchangaCalculationException(
                "Failed to calculate planet position for planet ID $planetId: $errorBuffer"
            )
        }

        return normalizeDegrees(positions[0])
    }

    /**
     * Normalize degrees using centralized utility.
     */
    private fun normalizeDegrees(degrees: Double): Double = VedicAstrologyUtils.normalizeDegree(degrees)

    private fun calculateLunarElongation(sunLongitude: Double, moonLongitude: Double): Double {
        var elongation = moonLongitude - sunLongitude
        if (elongation < 0.0) elongation += 360.0
        if (elongation >= 360.0) elongation = 0.0
        return elongation
    }

    private fun calculateTithi(sunLongitude: Double, moonLongitude: Double): TithiData {
        val elongation = calculateLunarElongation(sunLongitude, moonLongitude)

        val tithiIndex = (elongation / TITHI_SPAN).toInt()
        val tithiNumber = tithiIndex + 1
        val progressInTithi = elongation % TITHI_SPAN
        val tithiProgress = (progressInTithi / TITHI_SPAN) * 100.0

        val clampedIndex = tithiIndex.coerceIn(0, Tithi.entries.size - 1)
        val tithi = Tithi.entries[clampedIndex]
        val lord = getTithiLord(tithiNumber)

        val remainingDegrees = TITHI_SPAN - progressInTithi

        return TithiData(
            tithi = tithi,
            number = tithiNumber.coerceIn(1, TOTAL_TITHIS),
            progress = tithiProgress,
            lord = lord,
            elongation = elongation,
            remainingDegrees = remainingDegrees
        )
    }

    private fun getTithiLord(tithiNumber: Int): Planet {
        val tithiInPaksha = ((tithiNumber - 1) % 15) + 1
        return when (tithiInPaksha) {
            1 -> Planet.SUN
            2 -> Planet.MOON
            3 -> Planet.MARS
            4 -> Planet.MERCURY
            5 -> Planet.JUPITER
            6 -> Planet.VENUS
            7 -> Planet.SATURN
            8 -> Planet.RAHU
            9 -> Planet.SUN
            10 -> Planet.MOON
            11 -> Planet.MARS
            12 -> Planet.MERCURY
            13 -> Planet.JUPITER
            14 -> Planet.VENUS
            15 -> Planet.SATURN
            else -> Planet.SUN
        }
    }

    private fun calculateNakshatra(moonLongitude: Double): NakshatraData {
        val nakshatraIndex = (moonLongitude / NAKSHATRA_SPAN).toInt()
        val nakshatraNumber = nakshatraIndex + 1

        val positionInNakshatra = moonLongitude % NAKSHATRA_SPAN
        val nakshatraProgress = (positionInNakshatra / NAKSHATRA_SPAN) * 100.0

        val padaIndex = (positionInNakshatra / PADA_SPAN).toInt()
        val pada = (padaIndex % 4) + 1

        val clampedIndex = nakshatraIndex.coerceIn(0, Nakshatra.entries.size - 1)
        val nakshatra = Nakshatra.entries[clampedIndex]

        val remainingDegrees = NAKSHATRA_SPAN - positionInNakshatra

        return NakshatraData(
            nakshatra = nakshatra,
            number = nakshatraNumber.coerceIn(1, TOTAL_NAKSHATRAS),
            pada = pada,
            progress = nakshatraProgress,
            lord = nakshatra.ruler,
            degreeInNakshatra = positionInNakshatra,
            remainingDegrees = remainingDegrees
        )
    }

    private fun calculateYoga(sunLongitude: Double, moonLongitude: Double): YogaData {
        var sum = sunLongitude + moonLongitude

        while (sum >= 360.0) {
            sum -= 360.0
        }

        val yogaIndex = (sum / YOGA_SPAN).toInt()
        val yogaNumber = yogaIndex + 1
        val progressInYoga = sum % YOGA_SPAN
        val yogaProgress = (progressInYoga / YOGA_SPAN) * 100.0

        val clampedIndex = yogaIndex.coerceIn(0, Yoga.entries.size - 1)
        val yoga = Yoga.entries[clampedIndex]

        val remainingDegrees = YOGA_SPAN - progressInYoga

        return YogaData(
            yoga = yoga,
            number = yogaNumber.coerceIn(1, TOTAL_YOGAS),
            progress = yogaProgress,
            combinedLongitude = sum,
            remainingDegrees = remainingDegrees
        )
    }

    private fun calculateKarana(sunLongitude: Double, moonLongitude: Double): KaranaData {
        val elongation = calculateLunarElongation(sunLongitude, moonLongitude)

        val karanaIndex = (elongation / KARANA_SPAN).toInt()
        val karanaNumber = karanaIndex + 1
        val progressInKarana = elongation % KARANA_SPAN
        val karanaProgress = (progressInKarana / KARANA_SPAN) * 100.0

        val karana = getKaranaFromNumber(karanaNumber)

        val remainingDegrees = KARANA_SPAN - progressInKarana

        return KaranaData(
            karana = karana,
            number = karanaNumber.coerceIn(1, TOTAL_KARANAS),
            progress = karanaProgress,
            remainingDegrees = remainingDegrees
        )
    }

    private fun getKaranaFromNumber(karanaNumber: Int): Karana {
        return when (karanaNumber) {
            1 -> Karana.KIMSTUGHNA
            58 -> Karana.SHAKUNI
            59 -> Karana.CHATUSHPADA
            60 -> Karana.NAGA
            else -> {
                val adjustedNumber = karanaNumber - 2
                val movableIndex = adjustedNumber % MOVABLE_KARANAS
                when (movableIndex) {
                    0 -> Karana.BAVA
                    1 -> Karana.BALAVA
                    2 -> Karana.KAULAVA
                    3 -> Karana.TAITILA
                    4 -> Karana.GARA
                    5 -> Karana.VANIJA
                    6 -> Karana.VISHTI
                    else -> Karana.BAVA
                }
            }
        }
    }

    private fun calculateVara(julianDay: Double): Vara {
        val dayNumber = (floor(julianDay + 1.5).toLong() % 7).toInt()
        return Vara.entries[dayNumber.coerceIn(0, Vara.entries.size - 1)]
    }

    private fun determinePaksha(tithiNumber: Int): Paksha {
        return if (tithiNumber <= 15) Paksha.SHUKLA else Paksha.KRISHNA
    }

    private fun calculateSunriseSunset(
        julianDay: Double,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId
    ): Pair<String, String> {
        val geopos = doubleArrayOf(longitude, latitude, 0.0)
        val timeResult = DblObj()
        val errorBuffer = StringBuffer()

        val jdMidnight = floor(julianDay - 0.5) + 0.5

        val sunriseFlags = SweConst.SE_CALC_RISE or SweConst.SE_BIT_DISC_CENTER
        val riseResult = swissEph.swe_rise_trans(
            jdMidnight,
            SweConst.SE_SUN,
            null,
            SweConst.SEFLG_SWIEPH,
            sunriseFlags,
            geopos,
            1013.25,
            15.0,
            timeResult,
            errorBuffer
        )

        val sunriseJD: Double? = if (riseResult >= 0 && timeResult.`val` > jdMidnight) {
            timeResult.`val`
        } else {
            null
        }

        val sunsetFlags = SweConst.SE_CALC_SET or SweConst.SE_BIT_DISC_CENTER
        val setResult = swissEph.swe_rise_trans(
            jdMidnight,
            SweConst.SE_SUN,
            null,
            SweConst.SEFLG_SWIEPH,
            sunsetFlags,
            geopos,
            1013.25,
            15.0,
            timeResult,
            errorBuffer
        )

        val sunsetJD: Double? = if (setResult >= 0 && timeResult.`val` > jdMidnight) {
            timeResult.`val`
        } else {
            null
        }

        val sunrise = sunriseJD?.let { formatJulianDayToLocalTime(it, zoneId) } ?: "N/A"
        val sunset = sunsetJD?.let { formatJulianDayToLocalTime(it, zoneId) } ?: "N/A"

        return Pair(sunrise, sunset)
    }

    private fun formatJulianDayToLocalTime(julianDay: Double, zoneId: ZoneId): String {
        val sweDate = SweDate(julianDay)

        val utcHour = sweDate.hour
        val hourInt = utcHour.toInt()
        val minuteFraction = (utcHour - hourInt) * 60.0
        val minuteInt = minuteFraction.toInt()
        val secondFraction = (minuteFraction - minuteInt) * 60.0
        val secondInt = secondFraction.toInt()

        val utcZonedDateTime = ZonedDateTime.of(
            sweDate.year,
            sweDate.month,
            sweDate.day,
            hourInt,
            minuteInt,
            secondInt,
            0,
            ZoneId.of("UTC")
        )

        val localDateTime = utcZonedDateTime.withZoneSameInstant(zoneId)

        val hour = localDateTime.hour
        val minute = localDateTime.minute
        val second = localDateTime.second

        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }

        return String.format(Locale.US, "%d:%02d:%02d %s", displayHour, minute, second, amPm)
    }

    private fun calculateMoonPhase(sunLongitudeTropical: Double, moonLongitudeTropical: Double): Double {
        var elongation = moonLongitudeTropical - sunLongitudeTropical
        if (elongation < 0.0) elongation += 360.0
        if (elongation >= 360.0) elongation = 0.0

        val elongationRadians = Math.toRadians(elongation)
        val illumination = (1.0 - cos(elongationRadians)) / 2.0 * 100.0

        return illumination.coerceIn(0.0, 100.0)
    }

    private fun calculateJulianDay(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Double {
        val decimalHours = hour + (minute / 60.0) + (second / 3600.0)
        val sweDate = SweDate(year, month, day, decimalHours, SweDate.SE_GREG_CAL)
        return sweDate.julDay
    }

    override fun close() {
        if (!isClosed) {
            synchronized(this) {
                if (!isClosed) {
                    swissEph.swe_close()
                    isClosed = true
                }
            }
        }
    }
}

class PanchangaCalculationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

enum class Tithi(val number: Int, val displayName: String, val sanskrit: String, val group: TithiGroup) {
    PRATIPADA(1, "Pratipada", "प्रतिपदा", TithiGroup.NANDA),
    DWITIYA(2, "Dwitiya", "द्वितीया", TithiGroup.BHADRA),
    TRITIYA(3, "Tritiya", "तृतीया", TithiGroup.JAYA),
    CHATURTHI(4, "Chaturthi", "चतुर्थी", TithiGroup.RIKTA),
    PANCHAMI(5, "Panchami", "पञ्चमी", TithiGroup.PURNA),
    SHASHTHI(6, "Shashthi", "षष्ठी", TithiGroup.NANDA),
    SAPTAMI(7, "Saptami", "सप्तमी", TithiGroup.BHADRA),
    ASHTAMI(8, "Ashtami", "अष्टमी", TithiGroup.JAYA),
    NAVAMI(9, "Navami", "नवमी", TithiGroup.RIKTA),
    DASHAMI(10, "Dashami", "दशमी", TithiGroup.PURNA),
    EKADASHI(11, "Ekadashi", "एकादशी", TithiGroup.NANDA),
    DWADASHI(12, "Dwadashi", "द्वादशी", TithiGroup.BHADRA),
    TRAYODASHI(13, "Trayodashi", "त्रयोदशी", TithiGroup.JAYA),
    CHATURDASHI(14, "Chaturdashi", "चतुर्दशी", TithiGroup.RIKTA),
    PURNIMA(15, "Purnima", "पूर्णिमा", TithiGroup.PURNA),
    PRATIPADA_K(16, "Pratipada", "प्रतिपदा", TithiGroup.NANDA),
    DWITIYA_K(17, "Dwitiya", "द्वितीया", TithiGroup.BHADRA),
    TRITIYA_K(18, "Tritiya", "तृतीया", TithiGroup.JAYA),
    CHATURTHI_K(19, "Chaturthi", "चतुर्थी", TithiGroup.RIKTA),
    PANCHAMI_K(20, "Panchami", "पञ्चमी", TithiGroup.PURNA),
    SHASHTHI_K(21, "Shashthi", "षष्ठी", TithiGroup.NANDA),
    SAPTAMI_K(22, "Saptami", "सप्तमी", TithiGroup.BHADRA),
    ASHTAMI_K(23, "Ashtami", "अष्टमी", TithiGroup.JAYA),
    NAVAMI_K(24, "Navami", "नवमी", TithiGroup.RIKTA),
    DASHAMI_K(25, "Dashami", "दशमी", TithiGroup.PURNA),
    EKADASHI_K(26, "Ekadashi", "एकादशी", TithiGroup.NANDA),
    DWADASHI_K(27, "Dwadashi", "द्वादशी", TithiGroup.BHADRA),
    TRAYODASHI_K(28, "Trayodashi", "त्रयोदशी", TithiGroup.JAYA),
    CHATURDASHI_K(29, "Chaturdashi", "चतुर्दशी", TithiGroup.RIKTA),
    AMAVASYA(30, "Amavasya", "अमावस्या", TithiGroup.PURNA)
}

enum class TithiGroup(val displayName: String, val nature: String) {
    NANDA("Nanda", "Joyful"),
    BHADRA("Bhadra", "Auspicious"),
    JAYA("Jaya", "Victorious"),
    RIKTA("Rikta", "Empty"),
    PURNA("Purna", "Complete")
}

enum class Yoga(val number: Int, val displayName: String, val sanskrit: String, val nature: YogaNature) {
    VISHKUMBHA(1, "Vishkumbha", "विष्कुम्भ", YogaNature.INAUSPICIOUS),
    PRITI(2, "Priti", "प्रीति", YogaNature.AUSPICIOUS),
    AYUSHMAN(3, "Ayushman", "आयुष्मान्", YogaNature.AUSPICIOUS),
    SAUBHAGYA(4, "Saubhagya", "सौभाग्य", YogaNature.AUSPICIOUS),
    SHOBHANA(5, "Shobhana", "शोभन", YogaNature.AUSPICIOUS),
    ATIGANDA(6, "Atiganda", "अतिगण्ड", YogaNature.INAUSPICIOUS),
    SUKARMA(7, "Sukarma", "सुकर्म", YogaNature.AUSPICIOUS),
    DHRITI(8, "Dhriti", "धृति", YogaNature.AUSPICIOUS),
    SHULA(9, "Shula", "शूल", YogaNature.INAUSPICIOUS),
    GANDA(10, "Ganda", "गण्ड", YogaNature.INAUSPICIOUS),
    VRIDDHI(11, "Vriddhi", "वृद्धि", YogaNature.AUSPICIOUS),
    DHRUVA(12, "Dhruva", "ध्रुव", YogaNature.AUSPICIOUS),
    VYAGHATA(13, "Vyaghata", "व्याघात", YogaNature.INAUSPICIOUS),
    HARSHANA(14, "Harshana", "हर्षण", YogaNature.AUSPICIOUS),
    VAJRA(15, "Vajra", "वज्र", YogaNature.MIXED),
    SIDDHI(16, "Siddhi", "सिद्धि", YogaNature.AUSPICIOUS),
    VYATIPATA(17, "Vyatipata", "व्यतीपात", YogaNature.INAUSPICIOUS),
    VARIYAN(18, "Variyan", "वरीयान्", YogaNature.AUSPICIOUS),
    PARIGHA(19, "Parigha", "परिघ", YogaNature.INAUSPICIOUS),
    SHIVA(20, "Shiva", "शिव", YogaNature.AUSPICIOUS),
    SIDDHA(21, "Siddha", "सिद्ध", YogaNature.AUSPICIOUS),
    SADHYA(22, "Sadhya", "साध्य", YogaNature.AUSPICIOUS),
    SHUBHA(23, "Shubha", "शुभ", YogaNature.AUSPICIOUS),
    SHUKLA(24, "Shukla", "शुक्ल", YogaNature.AUSPICIOUS),
    BRAHMA(25, "Brahma", "ब्रह्म", YogaNature.AUSPICIOUS),
    INDRA(26, "Indra", "इन्द्र", YogaNature.AUSPICIOUS),
    VAIDHRITI(27, "Vaidhriti", "वैधृति", YogaNature.INAUSPICIOUS)
}

enum class YogaNature(val displayName: String) {
    AUSPICIOUS("Auspicious"),
    INAUSPICIOUS("Inauspicious"),
    MIXED("Mixed")
}

enum class Karana(val displayName: String, val sanskrit: String, val type: KaranaType) {
    KIMSTUGHNA("Kimstughna", "किंस्तुघ्न", KaranaType.FIXED),
    BAVA("Bava", "बव", KaranaType.MOVABLE),
    BALAVA("Balava", "बालव", KaranaType.MOVABLE),
    KAULAVA("Kaulava", "कौलव", KaranaType.MOVABLE),
    TAITILA("Taitila", "तैतिल", KaranaType.MOVABLE),
    GARA("Gara", "गर", KaranaType.MOVABLE),
    VANIJA("Vanija", "वणिज", KaranaType.MOVABLE),
    VISHTI("Vishti", "विष्टि", KaranaType.MOVABLE),
    SHAKUNI("Shakuni", "शकुनि", KaranaType.FIXED),
    CHATUSHPADA("Chatushpada", "चतुष्पाद", KaranaType.FIXED),
    NAGA("Naga", "नाग", KaranaType.FIXED);

    val nature: String
        get() = type.displayName
}

enum class KaranaType(val displayName: String) {
    FIXED("Fixed"),
    MOVABLE("Movable")
}

enum class Vara(val number: Int, val displayName: String, val sanskrit: String, val lord: Planet) {
    SUNDAY(0, "Sunday", "रविवार", Planet.SUN),
    MONDAY(1, "Monday", "सोमवार", Planet.MOON),
    TUESDAY(2, "Tuesday", "मंगलवार", Planet.MARS),
    WEDNESDAY(3, "Wednesday", "बुधवार", Planet.MERCURY),
    THURSDAY(4, "Thursday", "गुरुवार", Planet.JUPITER),
    FRIDAY(5, "Friday", "शुक्रवार", Planet.VENUS),
    SATURDAY(6, "Saturday", "शनिवार", Planet.SATURN)
}

enum class Paksha(val displayName: String, val sanskrit: String) {
    SHUKLA("Shukla Paksha", "शुक्ल पक्ष"),
    KRISHNA("Krishna Paksha", "कृष्ण पक्ष")
}

data class TithiData(
    val tithi: Tithi,
    val number: Int,
    val progress: Double,
    val lord: Planet,
    val elongation: Double,
    val remainingDegrees: Double
) {
    val group: TithiGroup
        get() = tithi.group

    val isKrishnaPaksha: Boolean
        get() = number > 15
}

data class NakshatraData(
    val nakshatra: Nakshatra,
    val number: Int,
    val pada: Int,
    val progress: Double,
    val lord: Planet,
    val degreeInNakshatra: Double,
    val remainingDegrees: Double
)

data class YogaData(
    val yoga: Yoga,
    val number: Int,
    val progress: Double,
    val combinedLongitude: Double,
    val remainingDegrees: Double
) {
    val isAuspicious: Boolean
        get() = yoga.nature == YogaNature.AUSPICIOUS
}

data class KaranaData(
    val karana: Karana,
    val number: Int,
    val progress: Double,
    val remainingDegrees: Double
) {
    val isVishti: Boolean
        get() = karana == Karana.VISHTI
}

data class PanchangaData(
    val tithi: TithiData,
    val nakshatra: NakshatraData,
    val yoga: YogaData,
    val karana: KaranaData,
    val vara: Vara,
    val paksha: Paksha,
    val sunrise: String,
    val sunset: String,
    val moonPhase: Double,
    val sunLongitude: Double,
    val moonLongitude: Double,
    val ayanamsa: Double = 0.0
) {
    val isShuklaPaksha: Boolean
        get() = paksha == Paksha.SHUKLA

    val tithiInPaksha: Int
        get() = if (tithi.number <= 15) tithi.number else tithi.number - 15

    fun toFormattedString(): String {
        return buildString {
            appendLine("════════════════════════════════════════════════════")
            appendLine("                    पञ्चाङ्ग (PANCHANGA)")
            appendLine("════════════════════════════════════════════════════")
            appendLine()
            appendLine("तिथि (TITHI)")
            appendLine("  ${tithi.tithi.displayName} (${tithi.tithi.sanskrit})")
            appendLine("  ${paksha.displayName} - ${tithiInPaksha}/15")
            appendLine("  Group: ${tithi.group.displayName} (${tithi.group.nature})")
            appendLine("  Progress: ${formatProgress(tithi.progress)}")
            appendLine("  Lord: ${tithi.lord.displayName}")
            appendLine()
            appendLine("नक्षत्र (NAKSHATRA)")
            appendLine("  ${nakshatra.nakshatra.displayName} - Pada ${nakshatra.pada}")
            appendLine("  Number: ${nakshatra.number}/27")
            appendLine("  Progress: ${formatProgress(nakshatra.progress)}")
            appendLine("  Lord: ${nakshatra.lord.displayName}")
            appendLine()
            appendLine("योग (YOGA)")
            appendLine("  ${yoga.yoga.displayName} (${yoga.yoga.sanskrit})")
            appendLine("  Nature: ${yoga.yoga.nature.displayName}")
            appendLine("  Number: ${yoga.number}/27")
            appendLine("  Progress: ${formatProgress(yoga.progress)}")
            appendLine()
            appendLine("करण (KARANA)")
            appendLine("  ${karana.karana.displayName} (${karana.karana.sanskrit})")
            appendLine("  Type: ${karana.karana.nature}")
            appendLine("  Number: ${karana.number}/60")
            appendLine("  Progress: ${formatProgress(karana.progress)}")
            if (karana.isVishti) {
                appendLine("  ⚠ Vishti (Bhadra) Karana - Inauspicious")
            }
            appendLine()
            appendLine("वार (VARA): ${vara.displayName} (${vara.sanskrit})")
            appendLine("  Lord: ${vara.lord.displayName}")
            appendLine()
            appendLine("────────────────────────────────────────────────────")
            appendLine("सूर्योदय (SUNRISE): $sunrise")
            appendLine("सूर्यास्त (SUNSET): $sunset")
            appendLine("चन्द्र प्रकाश (MOON ILLUMINATION): ${formatProgress(moonPhase)}")
            appendLine()
            appendLine("────────────────────────────────────────────────────")
            appendLine("SIDEREAL POSITIONS")
            appendLine("  Sun: ${formatDegrees(sunLongitude)}")
            appendLine("  Moon: ${formatDegrees(moonLongitude)}")
            appendLine("  Ayanamsa (Lahiri): ${formatDegrees(ayanamsa)}")
            appendLine()
        }
    }

    private fun formatProgress(value: Double): String {
        return String.format(Locale.US, "%.2f%%", value)
    }

    private fun formatDegrees(value: Double): String {
        val degrees = value.toInt()
        val minutesTotal = (value - degrees) * 60.0
        val minutes = minutesTotal.toInt()
        val seconds = ((minutesTotal - minutes) * 60.0).toInt()
        return String.format(Locale.US, "%d° %d' %d\"", degrees, minutes, seconds)
    }
}

private val Yoga.yogaNature: YogaNature
    get() = when (this) {
        Yoga.VISHKUMBHA, Yoga.ATIGANDA, Yoga.SHULA, Yoga.GANDA,
        Yoga.VYAGHATA, Yoga.VYATIPATA, Yoga.PARIGHA, Yoga.VAIDHRITI -> YogaNature.INAUSPICIOUS
        Yoga.VAJRA -> YogaNature.MIXED
        else -> YogaNature.AUSPICIOUS
    }