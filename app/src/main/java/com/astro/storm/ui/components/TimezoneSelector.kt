package com.astro.storm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.astro.storm.data.localization.Language
import com.astro.storm.data.localization.LocalLanguage
import com.astro.storm.data.localization.StringKey
import com.astro.storm.data.localization.stringResource
import com.astro.storm.ui.theme.AppTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * Production-grade Searchable Timezone Selector Dialog
 *
 * Features:
 * - Fast lazy loading with virtualized list
 * - Real-time search/filter as you type
 * - Common timezones at the top for quick access
 * - Shows current time in each timezone
 * - UTC offset display
 * - Keyboard support with proper focus management
 * - Smooth animations
 * - Memory efficient with derived state
 */

// Theme colors matching the app design
private object TimezoneSelectorTheme {
    val ScreenBackground = Color(0xFF1C1410)
    val CardBackground = Color(0xFF2A201A)
    val AccentColor = Color(0xFFB8A99A)
    val TextPrimary = Color(0xFFE8DFD6)
    val TextSecondary = Color(0xFFB8A99A)
    val BorderColor = Color(0xFF4A3F38)
    val ChipBackground = Color(0xFF3D322B)
    val SearchBackground = Color(0xFF3D322B)
    val SelectedBackground = Color(0xFF4A3F38)
}

/**
 * Data class representing a timezone with computed metadata
 */
private data class TimezoneInfo(
    val id: String,
    val displayName: String,
    val city: String,
    val region: String,
    val utcOffset: String,
    val currentTime: String,
    val offsetMinutes: Int,
    val isCommon: Boolean = false
)

/**
 * Precomputed common timezone IDs for quick access
 * These are prioritized and shown at the top when no search query
 */
private val COMMON_TIMEZONES = setOf(
    "Asia/Kathmandu",    // Nepal
    "Asia/Kolkata",       // India
    "Asia/Dubai",         // UAE
    "Asia/Singapore",     // Singapore
    "Asia/Tokyo",         // Japan
    "Asia/Shanghai",      // China
    "Asia/Hong_Kong",     // Hong Kong
    "Europe/London",      // UK
    "Europe/Paris",       // France
    "Europe/Berlin",      // Germany
    "Europe/Moscow",      // Russia
    "America/New_York",   // US East
    "America/Los_Angeles", // US West
    "America/Chicago",    // US Central
    "America/Denver",     // US Mountain
    "America/Toronto",    // Canada
    "Australia/Sydney",   // Australia
    "Pacific/Auckland",   // New Zealand
    "UTC"                 // UTC
)

@Composable
fun TimezonePickerDialog(
    selectedTimezone: String,
    onDismiss: () -> Unit,
    onTimezoneSelected: (String) -> Unit
) {
    val language = LocalLanguage.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf("") }

    // Pre-compute all timezones with metadata (only once)
    val allTimezones = remember {
        val now = ZonedDateTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        TimeZone.getAvailableIDs()
            .filter { id ->
                id.contains("/") &&
                        !id.startsWith("Etc/") &&
                        !id.startsWith("SystemV/") &&
                        !id.contains("GMT") &&
                        !id.startsWith("US/") &&
                        !id.startsWith("Canada/")
            }
            .mapNotNull { id ->
                try {
                    val zoneId = ZoneId.of(id)
                    val zonedTime = now.withZoneSameInstant(zoneId)
                    val offset = zonedTime.offset
                    val totalSeconds = offset.totalSeconds
                    val hours = totalSeconds / 3600
                    val minutes = kotlin.math.abs((totalSeconds % 3600) / 60)

                    val utcOffset = if (minutes == 0) {
                        "UTC${if (hours >= 0) "+" else ""}$hours"
                    } else {
                        "UTC${if (hours >= 0) "+" else ""}$hours:${minutes.toString().padStart(2, '0')}"
                    }

                    val parts = id.split("/")
                    val city = parts.lastOrNull()?.replace("_", " ") ?: id
                    val region = parts.dropLast(1).joinToString("/")

                    TimezoneInfo(
                        id = id,
                        displayName = "$city ($utcOffset)",
                        city = city,
                        region = region,
                        utcOffset = utcOffset,
                        currentTime = zonedTime.format(timeFormatter),
                        offsetMinutes = totalSeconds / 60,
                        isCommon = id in COMMON_TIMEZONES
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .sortedWith(compareBy({ !it.isCommon }, { it.offsetMinutes }, { it.city }))
    }

    // Filter timezones based on search query (computed lazily)
    val filteredTimezones by remember(searchQuery, allTimezones) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allTimezones
            } else {
                val query = searchQuery.lowercase().trim()
                allTimezones.filter { tz ->
                    tz.id.lowercase().contains(query) ||
                            tz.city.lowercase().contains(query) ||
                            tz.region.lowercase().contains(query) ||
                            tz.utcOffset.lowercase().contains(query)
                }
            }
        }
    }

    // Find selected timezone in the list for scroll position
    val selectedIndex = remember(selectedTimezone, filteredTimezones) {
        filteredTimezones.indexOfFirst { it.id == selectedTimezone }.coerceAtLeast(0)
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceAtMost((filteredTimezones.size - 1).coerceAtLeast(0))
    )

    // Auto-focus search field
    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 600.dp)
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = TimezoneSelectorTheme.CardBackground
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                TimezoneDialogHeader(
                    language = language,
                    onDismiss = onDismiss
                )

                HorizontalDivider(color = TimezoneSelectorTheme.BorderColor)

                // Search Field
                TimezoneSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    focusRequester = searchFocusRequester,
                    onClear = { searchQuery = "" },
                    language = language
                )

                HorizontalDivider(color = TimezoneSelectorTheme.BorderColor)

                // Results count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "${filteredTimezones.size} timezones"
                            Language.NEPALI -> "${filteredTimezones.size} समयक्षेत्रहरू"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TimezoneSelectorTheme.TextSecondary
                    )

                    if (searchQuery.isNotBlank()) {
                        TextButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Clear"
                                    Language.NEPALI -> "हटाउनुहोस्"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TimezoneSelectorTheme.AccentColor
                            )
                        }
                    }
                }

                // Timezone List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Common timezones section (when no search)
                    if (searchQuery.isBlank()) {
                        item {
                            Text(
                                text = when (language) {
                                    Language.ENGLISH -> "Common"
                                    Language.NEPALI -> "सामान्य"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TimezoneSelectorTheme.AccentColor,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(
                        items = filteredTimezones,
                        key = { it.id }
                    ) { timezone ->
                        TimezoneItem(
                            timezone = timezone,
                            isSelected = timezone.id == selectedTimezone,
                            onClick = {
                                onTimezoneSelected(timezone.id)
                                onDismiss()
                            }
                        )

                        // Add section header when transitioning from common to all
                        if (searchQuery.isBlank() && timezone.isCommon) {
                            val nextTimezone = filteredTimezones.getOrNull(filteredTimezones.indexOf(timezone) + 1)
                            if (nextTimezone != null && !nextTimezone.isCommon) {
                                Text(
                                    text = when (language) {
                                        Language.ENGLISH -> "All Timezones"
                                        Language.NEPALI -> "सबै समयक्षेत्रहरू"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TimezoneSelectorTheme.AccentColor,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }

                    // Empty state
                    if (filteredTimezones.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = TimezoneSelectorTheme.TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = when (language) {
                                            Language.ENGLISH -> "No timezones found"
                                            Language.NEPALI -> "समयक्षेत्र फेला परेन"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TimezoneSelectorTheme.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimezoneDialogHeader(
    language: Language,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = TimezoneSelectorTheme.AccentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = when (language) {
                    Language.ENGLISH -> "Select Timezone"
                    Language.NEPALI -> "समयक्षेत्र छान्नुहोस्"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TimezoneSelectorTheme.TextPrimary
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = when (language) {
                    Language.ENGLISH -> "Close"
                    Language.NEPALI -> "बन्द गर्नुहोस्"
                },
                tint = TimezoneSelectorTheme.TextSecondary
            )
        }
    }
}

@Composable
private fun TimezoneSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onClear: () -> Unit,
    language: Language
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .background(
                color = TimezoneSelectorTheme.SearchBackground,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TimezoneSelectorTheme.TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                color = TimezoneSelectorTheme.TextPrimary,
                fontSize = 16.sp
            ),
            singleLine = true,
            cursorBrush = SolidColor(TimezoneSelectorTheme.AccentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { /* Already filtering as you type */ }),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = when (language) {
                                Language.ENGLISH -> "Search by city, region, or UTC offset..."
                                Language.NEPALI -> "शहर, क्षेत्र, वा UTC खोज्नुहोस्..."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TimezoneSelectorTheme.TextSecondary
                        )
                    }
                    innerTextField()
                }
            }
        )

        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = when (language) {
                        Language.ENGLISH -> "Clear search"
                        Language.NEPALI -> "खोजी हटाउनुहोस्"
                    },
                    tint = TimezoneSelectorTheme.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TimezoneItem(
    timezone: TimezoneInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = if (isSelected) TimezoneSelectorTheme.SelectedBackground else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timezone.city,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) TimezoneSelectorTheme.AccentColor else TimezoneSelectorTheme.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timezone.region,
                        style = MaterialTheme.typography.bodySmall,
                        color = TimezoneSelectorTheme.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (timezone.region.isNotEmpty()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = TimezoneSelectorTheme.TextSecondary
                        )
                    }
                    Text(
                        text = timezone.utcOffset,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = TimezoneSelectorTheme.TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Current time in this timezone
                Text(
                    text = timezone.currentTime,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TimezoneSelectorTheme.AccentColor
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = TimezoneSelectorTheme.AccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact Timezone Selector Field (for use in forms)
 *
 * A tappable field that opens the timezone picker dialog
 */
@Composable
fun TimezoneSelector(
    selectedTimezone: String,
    onTimezoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val language = LocalLanguage.current
    var showDialog by remember { mutableStateOf(false) }

    // Get timezone display info
    val timezoneInfo = remember(selectedTimezone) {
        try {
            val now = ZonedDateTime.now()
            val zoneId = ZoneId.of(selectedTimezone)
            val zonedTime = now.withZoneSameInstant(zoneId)
            val offset = zonedTime.offset
            val totalSeconds = offset.totalSeconds
            val hours = totalSeconds / 3600
            val minutes = kotlin.math.abs((totalSeconds % 3600) / 60)

            val utcOffset = if (minutes == 0) {
                "UTC${if (hours >= 0) "+" else ""}$hours"
            } else {
                "UTC${if (hours >= 0) "+" else ""}$hours:${minutes.toString().padStart(2, '0')}"
            }

            val city = selectedTimezone.split("/").lastOrNull()?.replace("_", " ") ?: selectedTimezone
            "$city ($utcOffset)"
        } catch (e: Exception) {
            selectedTimezone
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = TimezoneSelectorTheme.ChipBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TimezoneSelectorTheme.AccentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = when (language) {
                            Language.ENGLISH -> "Timezone"
                            Language.NEPALI -> "समयक्षेत्र"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TimezoneSelectorTheme.TextSecondary
                    )
                    Text(
                        text = timezoneInfo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) TimezoneSelectorTheme.TextPrimary else TimezoneSelectorTheme.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = when (language) {
                    Language.ENGLISH -> "Change timezone"
                    Language.NEPALI -> "समयक्षेत्र परिवर्तन गर्नुहोस्"
                },
                tint = TimezoneSelectorTheme.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDialog) {
        TimezonePickerDialog(
            selectedTimezone = selectedTimezone,
            onDismiss = { showDialog = false },
            onTimezoneSelected = { timezone ->
                onTimezoneSelected(timezone)
                showDialog = false
            }
        )
    }
}
