package com.astro.storm.data.localization

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object BikramSambatConverter {

    enum class BSMonth(val index: Int, val englishName: String, val nepaliName: String) {
        BAISHAKH(1, "Baishakh", "बैशाख"),
        JESTHA(2, "Jestha", "जेठ"),
        ASHADH(3, "Ashadh", "असार"),
        SHRAWAN(4, "Shrawan", "साउन"),
        BHADRA(5, "Bhadra", "भदौ"),
        ASHWIN(6, "Ashwin", "असोज"),
        KARTIK(7, "Kartik", "कार्तिक"),
        MANGSIR(8, "Mangsir", "मंसिर"),
        POUSH(9, "Poush", "पुष"),
        MAGH(10, "Magh", "माघ"),
        FALGUN(11, "Falgun", "फाल्गुन"),
        CHAITRA(12, "Chaitra", "चैत्र");

        fun getName(language: Language): String = when (language) {
            Language.ENGLISH -> englishName
            Language.NEPALI -> nepaliName
        }

        companion object {
            fun fromIndex(index: Int): BSMonth = entries.find { it.index == index }
                ?: throw IllegalArgumentException("Invalid month index: $index")
        }
    }

    enum class BSWeekday(
        val englishName: String,
        val nepaliName: String,
        val shortEnglish: String,
        val shortNepali: String,
        val dayIndex: Int
    ) {
        SUNDAY("Sunday", "आइतबार", "Sun", "आइत", 0),
        MONDAY("Monday", "सोमबार", "Mon", "सोम", 1),
        TUESDAY("Tuesday", "मङ्गलबार", "Tue", "मङ्गल", 2),
        WEDNESDAY("Wednesday", "बुधबार", "Wed", "बुध", 3),
        THURSDAY("Thursday", "बिहीबार", "Thu", "बिही", 4),
        FRIDAY("Friday", "शुक्रबार", "Fri", "शुक्र", 5),
        SATURDAY("Saturday", "शनिबार", "Sat", "शनि", 6);

        fun getName(language: Language, short: Boolean = false): String = when (language) {
            Language.ENGLISH -> if (short) shortEnglish else englishName
            Language.NEPALI -> if (short) shortNepali else nepaliName
        }

        companion object {
            fun fromJavaDayOfWeek(dayOfWeek: DayOfWeek): BSWeekday = when (dayOfWeek) {
                DayOfWeek.SUNDAY -> SUNDAY
                DayOfWeek.MONDAY -> MONDAY
                DayOfWeek.TUESDAY -> TUESDAY
                DayOfWeek.WEDNESDAY -> WEDNESDAY
                DayOfWeek.THURSDAY -> THURSDAY
                DayOfWeek.FRIDAY -> FRIDAY
                DayOfWeek.SATURDAY -> SATURDAY
            }

            fun fromIndex(index: Int): BSWeekday = entries.find { it.dayIndex == index }
                ?: throw IllegalArgumentException("Invalid weekday index: $index")
        }
    }

    data class BSDate(
        val year: Int,
        val month: Int,
        val day: Int
    ) : Comparable<BSDate> {
        
        val bsMonth: BSMonth get() = BSMonth.fromIndex(month)

        val weekday: BSWeekday? get() = toAD(this)?.let { BSWeekday.fromJavaDayOfWeek(it.dayOfWeek) }

        val isValid: Boolean get() = isValidBSDate(year, month, day)

        fun format(language: Language): String = when (language) {
            Language.ENGLISH -> "$day ${bsMonth.englishName}, $year"
            Language.NEPALI -> "${toNepaliNumerals(day)} ${bsMonth.nepaliName}, ${toNepaliNumerals(year)}"
        }

        fun formatWithWeekday(language: Language): String {
            val weekdayName = weekday?.getName(language) ?: return format(language)
            return when (language) {
                Language.ENGLISH -> "$weekdayName, $day ${bsMonth.englishName}, $year"
                Language.NEPALI -> "$weekdayName, ${toNepaliNumerals(day)} ${bsMonth.nepaliName}, ${toNepaliNumerals(year)}"
            }
        }

        fun formatShort(language: Language): String {
            val monthStr = month.toString().padStart(2, '0')
            val dayStr = day.toString().padStart(2, '0')
            return when (language) {
                Language.ENGLISH -> "$year-$monthStr-$dayStr"
                Language.NEPALI -> "${toNepaliNumerals(year)}-${toNepaliNumerals(month).padStart(2, '०')}-${toNepaliNumerals(day).padStart(2, '०')}"
            }
        }

        fun formatMonthYear(language: Language): String = when (language) {
            Language.ENGLISH -> "${bsMonth.englishName} $year"
            Language.NEPALI -> "${bsMonth.nepaliName} ${toNepaliNumerals(year)}"
        }

        fun formatISO(): String = "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

        override fun toString(): String = "${formatISO()} BS"

        override fun compareTo(other: BSDate): Int {
            return when {
                year != other.year -> year.compareTo(other.year)
                month != other.month -> month.compareTo(other.month)
                else -> day.compareTo(other.day)
            }
        }

        fun isSameDay(other: BSDate): Boolean = year == other.year && month == other.month && day == other.day

        fun toAD(): LocalDate? = BikramSambatConverter.toAD(this)

        companion object {
            fun parse(dateString: String): BSDate? {
                return try {
                    val parts = dateString.trim().replace(" BS", "").split("-")
                    if (parts.size == 3) {
                        BSDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    } else null
                } catch (e: Exception) {
                    null
                }
            }

            fun today(): BSDate = BikramSambatConverter.today()
        }
    }

    private const val REFERENCE_BS_YEAR = 2000
    private const val REFERENCE_BS_MONTH = 1
    private const val REFERENCE_BS_DAY = 1
    private val REFERENCE_AD_DATE: LocalDate = LocalDate.of(1943, 4, 14)

    private val BS_MONTH_DATA: Map<Int, IntArray> = mapOf(
        1970 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1971 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        1972 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1973 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1974 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1975 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1976 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1977 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        1978 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1979 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1980 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1981 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        1982 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1983 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1984 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1985 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        1986 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1987 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1988 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        1989 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        1990 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1991 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        1992 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1993 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        1994 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1995 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        1996 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        1997 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1998 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        1999 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2000 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2001 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2002 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2003 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2004 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2005 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2006 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2007 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2008 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2009 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2010 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2011 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2012 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2013 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2014 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2015 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2016 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2017 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2018 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2019 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2020 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2021 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2022 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2023 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2024 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2025 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2026 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2027 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2028 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2029 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2030 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2031 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2032 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2033 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2034 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2035 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2036 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2037 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2038 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2039 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2040 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2041 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2042 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2043 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2044 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2045 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2046 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2047 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2048 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2049 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2050 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2051 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2052 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2053 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2054 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2055 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2056 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2057 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2058 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2059 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2060 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2061 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2062 to intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31),
        2063 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2064 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2065 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2066 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2067 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2068 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2069 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2070 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2071 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2072 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2073 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2074 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2075 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2081 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2082 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2084 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2085 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2086 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2087 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2088 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2089 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2090 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2091 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2092 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2093 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2094 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2095 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2096 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2097 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2098 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2099 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2100 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30)
    )

    private data class YearCacheEntry(
        val daysFromReference: Long,
        val monthStartDays: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is YearCacheEntry) return false
            return daysFromReference == other.daysFromReference && monthStartDays.contentEquals(other.monthStartDays)
        }

        override fun hashCode(): Int = 31 * daysFromReference.hashCode() + monthStartDays.contentHashCode()
    }

    private val yearCache: Map<Int, YearCacheEntry> by lazy { buildYearCache() }

    private val adToYearIndex: List<Pair<LocalDate, Int>> by lazy { buildADToYearIndex() }

    private fun buildYearCache(): Map<Int, YearCacheEntry> {
        val result = mutableMapOf<Int, YearCacheEntry>()
        var daysFromRef = 0L

        for (year in REFERENCE_BS_YEAR..maxBSYear) {
            val monthData = BS_MONTH_DATA[year] ?: continue
            val monthStarts = IntArray(13)
            monthStarts[0] = 0
            for (m in 0 until 12) {
                monthStarts[m + 1] = monthStarts[m] + monthData[m]
            }
            result[year] = YearCacheEntry(daysFromRef, monthStarts)
            daysFromRef += monthData.sum()
        }

        daysFromRef = 0L
        for (year in (REFERENCE_BS_YEAR - 1) downTo minBSYear) {
            val monthData = BS_MONTH_DATA[year] ?: continue
            daysFromRef -= monthData.sum()
            val monthStarts = IntArray(13)
            monthStarts[0] = 0
            for (m in 0 until 12) {
                monthStarts[m + 1] = monthStarts[m] + monthData[m]
            }
            result[year] = YearCacheEntry(daysFromRef, monthStarts)
        }

        return result
    }

    private fun buildADToYearIndex(): List<Pair<LocalDate, Int>> {
        val result = mutableListOf<Pair<LocalDate, Int>>()
        for (year in minBSYear..maxBSYear) {
            val adDate = toADInternal(year, 1, 1) ?: continue
            result.add(adDate to year)
        }
        return result.sortedBy { it.first }
    }

    val minBSYear: Int get() = BS_MONTH_DATA.keys.minOrNull() ?: 1970
    val maxBSYear: Int get() = BS_MONTH_DATA.keys.maxOrNull() ?: 2100

    val minADDate: LocalDate by lazy { toAD(minBSYear, 1, 1) ?: LocalDate.of(1913, 4, 13) }
    val maxADDate: LocalDate by lazy {
        val lastYear = maxBSYear
        val lastMonth = 12
        val lastDay = getDaysInMonth(lastYear, lastMonth) ?: 30
        toAD(lastYear, lastMonth, lastDay) ?: LocalDate.of(2044, 4, 12)
    }

    fun getDaysInMonth(year: Int, month: Int): Int? {
        if (month !in 1..12) return null
        return BS_MONTH_DATA[year]?.getOrNull(month - 1)
    }

    fun getDaysInYear(year: Int): Int? = BS_MONTH_DATA[year]?.sum()

    fun getMonthStartDay(year: Int, month: Int): Int? {
        if (month !in 1..12) return null
        return yearCache[year]?.monthStartDays?.getOrNull(month - 1)
    }

    fun toAD(bsDate: BSDate): LocalDate? = toAD(bsDate.year, bsDate.month, bsDate.day)

    fun toAD(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate? {
        if (!isValidBSDate(bsYear, bsMonth, bsDay)) return null
        return toADInternal(bsYear, bsMonth, bsDay)
    }

    private fun toADInternal(bsYear: Int, bsMonth: Int, bsDay: Int): LocalDate? {
        val cache = yearCache[bsYear] ?: return null
        val monthStartDays = cache.monthStartDays
        val daysFromYearStart = monthStartDays[bsMonth - 1] + (bsDay - 1)
        val totalDaysFromReference = cache.daysFromReference + daysFromYearStart
        return REFERENCE_AD_DATE.plusDays(totalDaysFromReference)
    }

    fun toBS(adDate: LocalDate): BSDate? {
        if (adDate.isBefore(minADDate) || adDate.isAfter(maxADDate)) return null

        val daysDiff = ChronoUnit.DAYS.between(REFERENCE_AD_DATE, adDate)
        val bsYear = findBSYear(daysDiff) ?: return null
        val cache = yearCache[bsYear] ?: return null

        val daysIntoYear = (daysDiff - cache.daysFromReference).toInt()

        if (daysIntoYear < 0) return null

        val monthStarts = cache.monthStartDays
        var bsMonth = 1
        for (m in 1..12) {
            if (daysIntoYear < monthStarts[m]) {
                bsMonth = m
                break
            }
            if (m == 12) bsMonth = 12
        }

        val bsDay = daysIntoYear - monthStarts[bsMonth - 1] + 1
        val maxDay = getDaysInMonth(bsYear, bsMonth) ?: 30

        if (bsDay < 1 || bsDay > maxDay) return null

        return BSDate(bsYear, bsMonth, bsDay)
    }

    private fun findBSYear(daysDiff: Long): Int? {
        if (daysDiff >= 0) {
            for (year in REFERENCE_BS_YEAR..maxBSYear) {
                val cache = yearCache[year] ?: continue
                val nextYear = year + 1
                val nextCache = yearCache[nextYear]

                if (nextCache == null) {
                    val yearDays = getDaysInYear(year) ?: continue
                    if (daysDiff < cache.daysFromReference + yearDays) return year
                } else if (daysDiff < nextCache.daysFromReference) {
                    return year
                }
            }
            return maxBSYear
        } else {
            for (year in (REFERENCE_BS_YEAR - 1) downTo minBSYear) {
                val cache = yearCache[year] ?: continue
                if (daysDiff >= cache.daysFromReference) return year
            }
            return minBSYear
        }
    }

    fun today(): BSDate = toBS(LocalDate.now()) ?: BSDate(2081, 1, 1)

    fun isValidBSDate(year: Int, month: Int, day: Int): Boolean {
        if (year !in minBSYear..maxBSYear) return false
        if (month !in 1..12) return false
        val daysInMonth = getDaysInMonth(year, month) ?: return false
        return day in 1..daysInMonth
    }

    fun getWeekday(year: Int, month: Int, day: Int): BSWeekday? {
        return toAD(year, month, day)?.let { BSWeekday.fromJavaDayOfWeek(it.dayOfWeek) }
    }

    fun getFirstDayOfMonthWeekdayIndex(year: Int, month: Int): Int? {
        return toAD(year, month, 1)?.let { adDate ->
            when (adDate.dayOfWeek) {
                DayOfWeek.SUNDAY -> 0
                DayOfWeek.MONDAY -> 1
                DayOfWeek.TUESDAY -> 2
                DayOfWeek.WEDNESDAY -> 3
                DayOfWeek.THURSDAY -> 4
                DayOfWeek.FRIDAY -> 5
                DayOfWeek.SATURDAY -> 6
            }
        }
    }

    private val NEPALI_DIGITS = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
    
    private val ENGLISH_TO_NEPALI_DIGIT = mapOf(
        '0' to '०', '1' to '१', '2' to '२', '3' to '३', '4' to '४',
        '5' to '५', '6' to '६', '7' to '७', '8' to '८', '9' to '९'
    )
    
    private val NEPALI_TO_ENGLISH_DIGIT = mapOf(
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9'
    )

    fun toNepaliNumerals(number: Int): String {
        if (number < 0) return "-${toNepaliNumerals(-number)}"
        if (number < 10) return NEPALI_DIGITS[number].toString()
        
        val sb = StringBuilder()
        var n = number
        while (n > 0) {
            sb.insert(0, NEPALI_DIGITS[n % 10])
            n /= 10
        }
        return sb.toString()
    }

    fun toNepaliNumerals(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(ENGLISH_TO_NEPALI_DIGIT[c] ?: c)
        }
        return sb.toString()
    }

    fun fromNepaliNumerals(nepaliNumber: String): Int? {
        if (nepaliNumber.isEmpty()) return null
        return try {
            val converted = StringBuilder(nepaliNumber.length)
            for (c in nepaliNumber) {
                converted.append(NEPALI_TO_ENGLISH_DIGIT[c] ?: c)
            }
            converted.toString().toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getYearRange(): IntRange = minBSYear..maxBSYear

    fun getMonthNames(language: Language): List<String> = BSMonth.entries.map { it.getName(language) }

    fun getWeekdayNames(language: Language, short: Boolean = false): List<String> =
        BSWeekday.entries.map { it.getName(language, short) }

    fun getDaysForMonth(year: Int, month: Int): IntRange {
        val days = getDaysInMonth(year, month) ?: 30
        return 1..days
    }

    fun calculateAge(birthDate: BSDate): Int {
        val today = today()
        var age = today.year - birthDate.year

        if (today.month < birthDate.month ||
            (today.month == birthDate.month && today.day < birthDate.day)
        ) {
            age--
        }

        return age.coerceAtLeast(0)
    }

    fun calculateAge(birthDate: BSDate, referenceDate: BSDate): Int {
        var age = referenceDate.year - birthDate.year

        if (referenceDate.month < birthDate.month ||
            (referenceDate.month == birthDate.month && referenceDate.day < birthDate.day)
        ) {
            age--
        }

        return age.coerceAtLeast(0)
    }

    fun daysBetween(from: BSDate, to: BSDate): Long? {
        val fromAD = toAD(from) ?: return null
        val toAD = toAD(to) ?: return null
        return ChronoUnit.DAYS.between(fromAD, toAD)
    }

    fun addDays(bsDate: BSDate, days: Long): BSDate? {
        val adDate = toAD(bsDate) ?: return null
        val newADDate = adDate.plusDays(days)
        return toBS(newADDate)
    }

    fun addMonths(bsDate: BSDate, months: Int): BSDate? {
        if (months == 0) return bsDate
        
        var year = bsDate.year
        var month = bsDate.month
        var day = bsDate.day

        val totalMonths = (year * 12 + month - 1) + months
        year = totalMonths / 12
        month = (totalMonths % 12) + 1

        if (month < 1) {
            month += 12
            year--
        } else if (month > 12) {
            month -= 12
            year++
        }

        if (year !in minBSYear..maxBSYear) return null

        val maxDay = getDaysInMonth(year, month) ?: return null
        day = day.coerceAtMost(maxDay)

        return BSDate(year, month, day)
    }

    fun addYears(bsDate: BSDate, years: Int): BSDate? {
        val newYear = bsDate.year + years
        if (newYear !in minBSYear..maxBSYear) return null

        val maxDay = getDaysInMonth(newYear, bsDate.month) ?: return null
        val newDay = bsDate.day.coerceAtMost(maxDay)

        return BSDate(newYear, bsDate.month, newDay)
    }

    fun getNextMonth(year: Int, month: Int): Pair<Int, Int>? {
        return when {
            month < 12 -> year to (month + 1)
            year < maxBSYear -> (year + 1) to 1
            else -> null
        }
    }

    fun getPreviousMonth(year: Int, month: Int): Pair<Int, Int>? {
        return when {
            month > 1 -> year to (month - 1)
            year > minBSYear -> (year - 1) to 12
            else -> null
        }
    }

    fun getStartOfMonth(year: Int, month: Int): BSDate? {
        if (!isValidBSDate(year, month, 1)) return null
        return BSDate(year, month, 1)
    }

    fun getEndOfMonth(year: Int, month: Int): BSDate? {
        val days = getDaysInMonth(year, month) ?: return null
        return BSDate(year, month, days)
    }

    fun getStartOfYear(year: Int): BSDate? {
        if (year !in minBSYear..maxBSYear) return null
        return BSDate(year, 1, 1)
    }

    fun getEndOfYear(year: Int): BSDate? {
        if (year !in minBSYear..maxBSYear) return null
        val days = getDaysInMonth(year, 12) ?: return null
        return BSDate(year, 12, days)
    }

    fun isLeapYear(year: Int): Boolean {
        val days = getDaysInYear(year) ?: return false
        return days > 365
    }

    fun getDayOfYear(bsDate: BSDate): Int? {
        if (!bsDate.isValid) return null
        val cache = yearCache[bsDate.year] ?: return null
        return cache.monthStartDays[bsDate.month - 1] + bsDate.day
    }

    fun fromDayOfYear(year: Int, dayOfYear: Int): BSDate? {
        if (year !in minBSYear..maxBSYear) return null
        val cache = yearCache[year] ?: return null
        val totalDays = getDaysInYear(year) ?: return null
        
        if (dayOfYear < 1 || dayOfYear > totalDays) return null

        val monthStarts = cache.monthStartDays
        for (m in 1..12) {
            if (dayOfYear <= monthStarts[m]) {
                val day = dayOfYear - monthStarts[m - 1]
                return BSDate(year, m, day)
            }
        }
        return null
    }
}