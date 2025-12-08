package com.astro.storm.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.localization.BikramSambatConverter
import com.astro.storm.data.localization.BikramSambatConverter.BSDate
import com.astro.storm.data.localization.BikramSambatConverter.BSMonth
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.LocalLanguage
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.stringResource
import com.astro.storm.ui.theme.AppTheme
import java.time.LocalDate

/**
 * BS (Bikram Sambat) Date Picker Dialog
 *
 * A modern, responsive date picker for selecting dates in Bikram Sambat calendar.
 * Features:
 * - Year, Month, Day selection with localized labels
 * - Dynamic month lengths based on actual BS calendar data
 * - Responsive grid layout for day selection
 * - Animated transitions with haptic feedback
 * - Quick navigation arrows for month/year
 * - Today button for quick selection
 * - Support for both English and Nepali display
 */
@Composable
fun BSDatePickerDialog(
    initialDate: BSDate = BikramSambatConverter.today(),
    onDismiss: () -> Unit,
    onConfirm: (BSDate) -> Unit,
    minYear: Int = BikramSambatConverter.minBSYear,
    maxYear: Int = BikramSambatConverter.maxBSYear
) {
    val language = LocalLanguage.current
    val haptic = LocalHapticFeedback.current
    val todayBS = remember { BikramSambatConverter.today() }

    var selectedYear by remember { mutableIntStateOf(initialDate.year.coerceIn(minYear, maxYear)) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.month) }
    var selectedDay by remember { mutableIntStateOf(initialDate.day) }

    // Ensure day is valid for current month
    val daysInMonth = remember(selectedYear, selectedMonth) {
        BikramSambatConverter.getDaysInMonth(selectedYear, selectedMonth) ?: 30
    }

    // Adjust day if it exceeds month's days
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    // Navigation functions
    fun goToPreviousMonth() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (selectedMonth > 1) {
            selectedMonth--
        } else if (selectedYear > minYear) {
            selectedYear--
            selectedMonth = 12
        }
    }

    fun goToNextMonth() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (selectedMonth < 12) {
            selectedMonth++
        } else if (selectedYear < maxYear) {
            selectedYear++
            selectedMonth = 1
        }
    }

    fun goToToday() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        selectedYear = todayBS.year
        selectedMonth = todayBS.month
        selectedDay = todayBS.day
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.CardBackground,
        titleContentColor = AppTheme.TextPrimary,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(StringKey.BS_DATE_PICKER_TITLE),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                // Today button
                Surface(
                    onClick = { goToToday() },
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.AccentPrimary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = AppTheme.AccentPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Today"
                                Language.NEPALI -> "आज"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.AccentPrimary
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // Current selection display with animation
                SelectedDateDisplay(
                    year = selectedYear,
                    month = selectedMonth,
                    day = selectedDay,
                    language = language
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.DividerColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Month/Year navigation with arrows
                MonthYearNavigator(
                    selectedYear = selectedYear,
                    selectedMonth = selectedMonth,
                    language = language,
                    onPreviousMonth = { goToPreviousMonth() },
                    onNextMonth = { goToNextMonth() },
                    canGoPrevious = selectedYear > minYear || selectedMonth > 1,
                    canGoNext = selectedYear < maxYear || selectedMonth < 12
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Year and Month selectors in a row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Year selector
                    YearSelector(
                        selectedYear = selectedYear,
                        onYearChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedYear = it
                        },
                        minYear = minYear,
                        maxYear = maxYear,
                        language = language,
                        modifier = Modifier.weight(1f)
                    )

                    // Month selector
                    MonthSelector(
                        selectedMonth = selectedMonth,
                        onMonthChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedMonth = it
                        },
                        language = language,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Day grid with animations
                DayGrid(
                    selectedDay = selectedDay,
                    daysInMonth = daysInMonth,
                    onDaySelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedDay = it
                    },
                    language = language,
                    todayDay = if (selectedYear == todayBS.year && selectedMonth == todayBS.month) todayBS.day else null
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(BSDate(selectedYear, selectedMonth, selectedDay))
                }
            ) {
                Text(
                    text = stringResource(StringKey.BTN_OK),
                    color = AppTheme.AccentPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(StringKey.BTN_CANCEL),
                    color = AppTheme.TextSecondary
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Month/Year Navigator with arrows for quick navigation
 */
@Composable
private fun MonthYearNavigator(
    selectedYear: Int,
    selectedMonth: Int,
    language: Language,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean
) {
    val bsMonth = BSMonth.fromIndex(selectedMonth)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPreviousMonth,
            enabled = canGoPrevious,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = when (language) {
                    Language.ENGLISH -> "Previous month"
                    Language.NEPALI -> "अघिल्लो महिना"
                },
                tint = if (canGoPrevious) AppTheme.TextPrimary else AppTheme.TextMuted
            )
        }

        AnimatedContent(
            targetState = Pair(selectedYear, selectedMonth),
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width } + fadeOut())
            },
            label = "month_year_animation"
        ) { (year, _) ->
            Text(
                text = when (language) {
                    Language.ENGLISH -> "${bsMonth.englishName} $year"
                    Language.NEPALI -> "${bsMonth.nepaliName} ${BikramSambatConverter.toNepaliNumerals(year)}"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.TextPrimary
            )
        }

        IconButton(
            onClick = onNextMonth,
            enabled = canGoNext,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = when (language) {
                    Language.ENGLISH -> "Next month"
                    Language.NEPALI -> "अर्को महिना"
                },
                tint = if (canGoNext) AppTheme.TextPrimary else AppTheme.TextMuted
            )
        }
    }
}

@Composable
private fun SelectedDateDisplay(
    year: Int,
    month: Int,
    day: Int,
    language: Language
) {
    val bsMonth = BSMonth.fromIndex(month)
    val displayDate = when (language) {
        Language.ENGLISH -> "$day ${bsMonth.englishName}, $year BS"
        Language.NEPALI -> "${BikramSambatConverter.toNepaliNumerals(day)} ${bsMonth.nepaliName}, ${BikramSambatConverter.toNepaliNumerals(year)} वि.सं."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.AccentPrimary.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.AccentPrimary
            )

            // Show AD equivalent
            BikramSambatConverter.toAD(year, month, day)?.let { adDate ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "(${adDate.year}-${adDate.monthValue.toString().padStart(2, '0')}-${adDate.dayOfMonth.toString().padStart(2, '0')} AD)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.TextMuted
                )
            }
        }
    }
}

@Composable
private fun YearSelector(
    selectedYear: Int,
    onYearChange: (Int) -> Unit,
    minYear: Int,
    maxYear: Int,
    language: Language,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(StringKey.BS_YEAR),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        DropdownSelector(
            text = when (language) {
                Language.ENGLISH -> selectedYear.toString()
                Language.NEPALI -> BikramSambatConverter.toNepaliNumerals(selectedYear)
            },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            val years = (maxYear downTo minYear).toList()
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = years.indexOf(selectedYear).coerceAtLeast(0)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(years) { year ->
                    DropdownItem(
                        text = when (language) {
                            Language.ENGLISH -> year.toString()
                            Language.NEPALI -> BikramSambatConverter.toNepaliNumerals(year)
                        },
                        isSelected = year == selectedYear,
                        onClick = {
                            onYearChange(year)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: Int,
    onMonthChange: (Int) -> Unit,
    language: Language,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = stringResource(StringKey.BS_MONTH),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.TextMuted,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        DropdownSelector(
            text = BSMonth.fromIndex(selectedMonth).getName(language),
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 250.dp)
            ) {
                items(BSMonth.entries) { month ->
                    DropdownItem(
                        text = month.getName(language),
                        isSelected = month.index == selectedMonth,
                        onClick = {
                            onMonthChange(month.index)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownSelector(
    text: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "dropdown_rotation"
    )

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(10.dp),
            color = AppTheme.ChipBackground,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) AppTheme.AccentPrimary else AppTheme.DividerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = AppTheme.TextMuted,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(300)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.CardBackground),
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun DropdownItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) AppTheme.AccentPrimary.copy(alpha = 0.15f) else AppTheme.CardBackground
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) AppTheme.AccentPrimary else AppTheme.TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun DayGrid(
    selectedDay: Int,
    daysInMonth: Int,
    onDaySelected: (Int) -> Unit,
    language: Language,
    todayDay: Int? = null
) {
    Column {
        Text(
            text = stringResource(StringKey.BS_DAY),
            style = MaterialTheme.typography.labelMedium,
            color = AppTheme.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Weekday headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val weekdays = when (language) {
                Language.ENGLISH -> listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                Language.NEPALI -> listOf("आ", "सो", "मं", "बु", "बि", "शु", "श")
            }
            weekdays.forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 240.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items((1..daysInMonth).toList()) { day ->
                DayCell(
                    day = day,
                    isSelected = day == selectedDay,
                    isToday = day == todayDay,
                    onClick = { onDaySelected(day) },
                    language = language
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean = false,
    onClick: () -> Unit,
    language: Language
) {
    val displayDay = when (language) {
        Language.ENGLISH -> day.toString()
        Language.NEPALI -> BikramSambatConverter.toNepaliNumerals(day)
    }

    val backgroundColor = when {
        isSelected -> AppTheme.AccentPrimary
        else -> AppTheme.ChipBackground.copy(alpha = 0.5f)
    }

    val borderColor = when {
        isSelected -> AppTheme.AccentPrimary
        isToday -> AppTheme.AccentPrimary
        else -> AppTheme.DividerColor
    }

    val borderWidth = when {
        isSelected -> 0.dp
        isToday -> 2.dp
        else -> 1.dp
    }

    val textColor = when {
        isSelected -> AppTheme.ButtonText
        isToday -> AppTheme.AccentPrimary
        else -> AppTheme.TextPrimary
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, radius = 18.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayDay,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
    }
}

/**
 * Compact BS Date Selector (for inline use)
 *
 * Shows a clickable field that opens the BS Date Picker dialog
 */
@Composable
fun BSDateSelector(
    selectedDate: BSDate,
    onDateSelected: (BSDate) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val language = LocalLanguage.current
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        onClick = { if (enabled) showPicker = true },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.ChipBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppTheme.DividerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedDate.format(language),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) AppTheme.TextPrimary else AppTheme.TextMuted
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTheme.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showPicker) {
        BSDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showPicker = false },
            onConfirm = { date ->
                onDateSelected(date)
                showPicker = false
            }
        )
    }
}

/**
 * Helper function to convert LocalDate to BSDate
 */
fun LocalDate.toBSDate(): BSDate? = BikramSambatConverter.toBS(this)

/**
 * Helper function to convert BSDate to LocalDate
 */
fun BSDate.toLocalDate(): LocalDate? = BikramSambatConverter.toAD(this)
