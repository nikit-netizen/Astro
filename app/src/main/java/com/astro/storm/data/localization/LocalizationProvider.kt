package com.astro.storm.data.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composition Local for accessing current language
 */
val LocalLanguage = compositionLocalOf { Language.DEFAULT }

/**
 * Composition Local for accessing current date system
 */
val LocalDateSystem = compositionLocalOf { DateSystem.DEFAULT }

/**
 * Composition Local for accessing localization manager
 */
val LocalLocalizationManager = compositionLocalOf<LocalizationManager?> { null }

/**
 * Provider composable that supplies localization context to the app
 *
 * Usage:
 * ```kotlin
 * LocalizationProvider {
 *     // Your app content
 *     Text(text = stringResource(StringKey.HOME_TAB))
 * }
 * ```
 */
@Composable
fun LocalizationProvider(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val localizationManager = remember { LocalizationManager.getInstance(context) }

    val language by localizationManager.language.collectAsState()
    val dateSystem by localizationManager.dateSystem.collectAsState()

    CompositionLocalProvider(
        LocalLanguage provides language,
        LocalDateSystem provides dateSystem,
        LocalLocalizationManager provides localizationManager
    ) {
        content()
    }
}

/**
 * Get localized string for a key using current language from Composition Local
 *
 * Usage:
 * ```kotlin
 * Text(text = stringResource(StringKey.HOME_TAB))
 * ```
 */
@Composable
@ReadOnlyComposable
fun stringResource(key: StringKeyInterface): String {
    val language = LocalLanguage.current
    return StringResources.get(key, language)
}

/**
 * Get localized string with format arguments
 *
 * Usage:
 * ```kotlin
 * Text(text = stringResource(StringKey.ERROR_CALCULATIONS_FAILED, errorCount))
 * ```
 */
@Composable
@ReadOnlyComposable
fun stringResource(key: StringKeyInterface, vararg args: Any): String {
    val language = LocalLanguage.current
    return StringResources.get(key, language, *args)
}

/**
 * Get current language
 */
@Composable
@ReadOnlyComposable
fun currentLanguage(): Language = LocalLanguage.current

/**
 * Get current date system
 */
@Composable
@ReadOnlyComposable
fun currentDateSystem(): DateSystem = LocalDateSystem.current

/**
 * Check if current language is Nepali
 */
@Composable
@ReadOnlyComposable
fun isNepali(): Boolean = LocalLanguage.current == Language.NEPALI

/**
 * Check if current date system is BS
 */
@Composable
@ReadOnlyComposable
fun isBSDateSystem(): Boolean = LocalDateSystem.current == DateSystem.BS

/**
 * Extension function for any StringKeyInterface to get localized value
 */
@Composable
@ReadOnlyComposable
fun StringKeyInterface.localized(): String = stringResource(this)

/**
 * Extension function for any StringKeyInterface to get localized value with args
 */
@Composable
@ReadOnlyComposable
fun StringKeyInterface.localized(vararg args: Any): String = stringResource(this, *args)

// ============================================
// DATE FORMATTING UTILITIES
// ============================================

/**
 * Format a LocalDate according to the user's selected date system (AD or BS)
 *
 * @param format The format style to use
 * @return Formatted date string in the current language and date system
 */
@Composable
@ReadOnlyComposable
fun java.time.LocalDate.formatLocalized(
    format: DateFormat = DateFormat.FULL
): String {
    val language = LocalLanguage.current
    val dateSystem = LocalDateSystem.current
    return formatDate(this, dateSystem, language, format)
}

/**
 * Format a LocalDateTime according to the user's selected date system
 */
@Composable
@ReadOnlyComposable
fun java.time.LocalDateTime.formatLocalized(
    format: DateFormat = DateFormat.FULL,
    includeTime: Boolean = false
): String {
    val language = LocalLanguage.current
    val dateSystem = LocalDateSystem.current
    val dateStr = formatDate(this.toLocalDate(), dateSystem, language, format)
    return if (includeTime) {
        val timeStr = java.time.format.DateTimeFormatter.ofPattern("HH:mm").format(this)
        "$dateStr $timeStr"
    } else {
        dateStr
    }
}

/**
 * Date format options for localized date display
 */
enum class DateFormat {
    /** Full format: "2081 Magh 15" or "2081 माघ १५" */
    FULL,
    /** Short format: "2081-10-15" or "२०८१-१०-१५" */
    SHORT,
    /** Month Year format: "Magh 2081" or "माघ २०८१" */
    MONTH_YEAR,
    /** Year only: "2081" or "२०८१" */
    YEAR_ONLY,
    /** Day Month: "15 Magh" or "१५ माघ" */
    DAY_MONTH,
    /** Full with weekday: "Sunday, 2081 Magh 15" */
    FULL_WITH_WEEKDAY
}

/**
 * Core date formatting function
 */
fun formatDate(
    date: java.time.LocalDate,
    dateSystem: DateSystem,
    language: Language,
    format: DateFormat
): String {
    return when (dateSystem) {
        DateSystem.BS -> formatBSDate(date, language, format)
        DateSystem.AD -> formatADDate(date, language, format)
    }
}

/**
 * Format date in Bikram Sambat
 */
private fun formatBSDate(
    date: java.time.LocalDate,
    language: Language,
    format: DateFormat
): String {
    val bsDate = BikramSambatConverter.toBS(date)
        ?: return formatADDate(date, language, format) // Fallback to AD if conversion fails

    val month = BikramSambatConverter.BSMonth.fromIndex(bsDate.month)

    return when (format) {
        DateFormat.FULL -> when (language) {
            Language.ENGLISH -> "${bsDate.year} ${month.englishName} ${bsDate.day}"
            Language.NEPALI -> "${BikramSambatConverter.toNepaliNumerals(bsDate.year)} ${month.nepaliName} ${BikramSambatConverter.toNepaliNumerals(bsDate.day)}"
        }
        DateFormat.SHORT -> when (language) {
            Language.ENGLISH -> "${bsDate.year}-${bsDate.month.toString().padStart(2, '0')}-${bsDate.day.toString().padStart(2, '0')}"
            Language.NEPALI -> "${BikramSambatConverter.toNepaliNumerals(bsDate.year)}-${BikramSambatConverter.toNepaliNumerals(bsDate.month).padStart(2, '०')}-${BikramSambatConverter.toNepaliNumerals(bsDate.day).padStart(2, '०')}"
        }
        DateFormat.MONTH_YEAR -> when (language) {
            Language.ENGLISH -> "${month.englishName} ${bsDate.year}"
            Language.NEPALI -> "${month.nepaliName} ${BikramSambatConverter.toNepaliNumerals(bsDate.year)}"
        }
        DateFormat.YEAR_ONLY -> when (language) {
            Language.ENGLISH -> bsDate.year.toString()
            Language.NEPALI -> BikramSambatConverter.toNepaliNumerals(bsDate.year)
        }
        DateFormat.DAY_MONTH -> when (language) {
            Language.ENGLISH -> "${bsDate.day} ${month.englishName}"
            Language.NEPALI -> "${BikramSambatConverter.toNepaliNumerals(bsDate.day)} ${month.nepaliName}"
        }
        DateFormat.FULL_WITH_WEEKDAY -> {
            val weekdayKey = getWeekdayKey(date.dayOfWeek)
            val weekday = StringResources.get(weekdayKey, language)
            when (language) {
                Language.ENGLISH -> "$weekday, ${bsDate.year} ${month.englishName} ${bsDate.day}"
                Language.NEPALI -> "$weekday, ${BikramSambatConverter.toNepaliNumerals(bsDate.year)} ${month.nepaliName} ${BikramSambatConverter.toNepaliNumerals(bsDate.day)}"
            }
        }
    }
}

/**
 * Format date in AD (Gregorian)
 */
private fun formatADDate(
    date: java.time.LocalDate,
    language: Language,
    format: DateFormat
): String {
    val formatter = when (format) {
        DateFormat.FULL -> java.time.format.DateTimeFormatter.ofPattern("yyyy MMM d")
        DateFormat.SHORT -> java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        DateFormat.MONTH_YEAR -> java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
        DateFormat.YEAR_ONLY -> java.time.format.DateTimeFormatter.ofPattern("yyyy")
        DateFormat.DAY_MONTH -> java.time.format.DateTimeFormatter.ofPattern("d MMM")
        DateFormat.FULL_WITH_WEEKDAY -> java.time.format.DateTimeFormatter.ofPattern("EEEE, yyyy MMM d")
    }

    val formatted = date.format(formatter)

    return when (language) {
        Language.ENGLISH -> formatted
        Language.NEPALI -> convertToNepaliNumerals(formatted)
    }
}

/**
 * Convert all digits in a string to Nepali numerals
 */
private fun convertToNepaliNumerals(text: String): String {
    val nepaliDigits = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
    return text.map { char ->
        if (char.isDigit()) nepaliDigits[char.digitToInt()] else char
    }.joinToString("")
}

/**
 * Get string key for weekday
 */
private fun getWeekdayKey(dayOfWeek: java.time.DayOfWeek): StringKeyInterface {
    return when (dayOfWeek) {
        java.time.DayOfWeek.SUNDAY -> StringKeyMatch.DAY_SUNDAY
        java.time.DayOfWeek.MONDAY -> StringKeyMatch.DAY_MONDAY
        java.time.DayOfWeek.TUESDAY -> StringKeyMatch.DAY_TUESDAY
        java.time.DayOfWeek.WEDNESDAY -> StringKeyMatch.DAY_WEDNESDAY
        java.time.DayOfWeek.THURSDAY -> StringKeyMatch.DAY_THURSDAY
        java.time.DayOfWeek.FRIDAY -> StringKeyMatch.DAY_FRIDAY
        java.time.DayOfWeek.SATURDAY -> StringKeyMatch.DAY_SATURDAY
    }
}

/**
 * Format a date range (start to end) according to user's selected date system
 */
@Composable
@ReadOnlyComposable
fun formatDateRange(
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate,
    format: DateFormat = DateFormat.MONTH_YEAR
): String {
    val start = startDate.formatLocalized(format)
    val end = endDate.formatLocalized(format)
    return "$start – $end"
}

/**
 * Format remaining duration in localized format
 *
 * @param days Number of days remaining
 * @return Formatted duration string like "2y 3m" or "२ वर्ष ३ महिना"
 */
@Composable
@ReadOnlyComposable
fun formatRemainingDuration(days: Long): String {
    val language = LocalLanguage.current

    if (days <= 0) return ""

    val years = days / 365
    val remainingDaysAfterYears = days % 365
    val months = remainingDaysAfterYears / 30
    val remainingDays = remainingDaysAfterYears % 30

    return when (language) {
        Language.ENGLISH -> when {
            years > 0 && months > 0 -> "${years}y ${months}m remaining"
            years > 0 -> "${years}y remaining"
            months > 0 && remainingDays > 0 -> "${months}m ${remainingDays}d remaining"
            months > 0 -> "${months}m remaining"
            else -> "${remainingDays}d remaining"
        }
        Language.NEPALI -> when {
            years > 0 && months > 0 -> "${BikramSambatConverter.toNepaliNumerals(years.toInt())} वर्ष ${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना बाँकी"
            years > 0 -> "${BikramSambatConverter.toNepaliNumerals(years.toInt())} वर्ष बाँकी"
            months > 0 && remainingDays > 0 -> "${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना ${BikramSambatConverter.toNepaliNumerals(remainingDays.toInt())} दिन बाँकी"
            months > 0 -> "${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना बाँकी"
            else -> "${BikramSambatConverter.toNepaliNumerals(remainingDays.toInt())} दिन बाँकी"
        }
    }
}

/**
 * Format duration in years and months
 */
@Composable
@ReadOnlyComposable
fun formatDurationYearsMonths(years: Double): String {
    val language = LocalLanguage.current
    val wholeYears = years.toInt()
    val months = ((years - wholeYears) * 12).toInt()

    return when (language) {
        Language.ENGLISH -> when {
            months > 0 -> "${wholeYears}y ${months}m"
            else -> "${wholeYears} yrs"
        }
        Language.NEPALI -> when {
            months > 0 -> "${BikramSambatConverter.toNepaliNumerals(wholeYears)} वर्ष ${BikramSambatConverter.toNepaliNumerals(months)} महिना"
            else -> "${BikramSambatConverter.toNepaliNumerals(wholeYears)} वर्ष"
        }
    }
}

/**
 * Non-composable version for use outside composition
 */
object DateFormatter {
    fun format(
        date: java.time.LocalDate,
        dateSystem: DateSystem,
        language: Language,
        format: DateFormat = DateFormat.FULL
    ): String = formatDate(date, dateSystem, language, format)

    fun formatRange(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        dateSystem: DateSystem,
        language: Language,
        format: DateFormat = DateFormat.MONTH_YEAR
    ): String {
        val start = format(startDate, dateSystem, language, format)
        val end = format(endDate, dateSystem, language, format)
        return "$start – $end"
    }

    fun formatDuration(days: Long, language: Language): String {
        if (days <= 0) return ""

        val years = days / 365
        val remainingDaysAfterYears = days % 365
        val months = remainingDaysAfterYears / 30
        val remainingDays = remainingDaysAfterYears % 30

        return when (language) {
            Language.ENGLISH -> when {
                years > 0 && months > 0 -> "${years}y ${months}m"
                years > 0 -> "${years}y"
                months > 0 && remainingDays > 0 -> "${months}m ${remainingDays}d"
                months > 0 -> "${months}m"
                else -> "${remainingDays}d"
            }
            Language.NEPALI -> when {
                years > 0 && months > 0 -> "${BikramSambatConverter.toNepaliNumerals(years.toInt())} वर्ष ${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना"
                years > 0 -> "${BikramSambatConverter.toNepaliNumerals(years.toInt())} वर्ष"
                months > 0 && remainingDays > 0 -> "${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना ${BikramSambatConverter.toNepaliNumerals(remainingDays.toInt())} दिन"
                months > 0 -> "${BikramSambatConverter.toNepaliNumerals(months.toInt())} महिना"
                else -> "${BikramSambatConverter.toNepaliNumerals(remainingDays.toInt())} दिन"
            }
        }
    }

    fun formatYearsMonths(years: Double, language: Language): String {
        val wholeYears = years.toInt()
        val months = ((years - wholeYears) * 12).toInt()

        return when (language) {
            Language.ENGLISH -> when {
                months > 0 -> "${wholeYears}y ${months}m"
                else -> "${wholeYears} yrs"
            }
            Language.NEPALI -> when {
                months > 0 -> "${BikramSambatConverter.toNepaliNumerals(wholeYears)} वर्ष ${BikramSambatConverter.toNepaliNumerals(months)} महिना"
                else -> "${BikramSambatConverter.toNepaliNumerals(wholeYears)} वर्ष"
            }
        }
    }
}
