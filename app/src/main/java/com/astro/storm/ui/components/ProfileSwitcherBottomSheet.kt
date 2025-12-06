package com.astro.storm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.repository.SavedChart
import com.astro.storm.ui.theme.AppTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val SpringSpec = spring<Color>(stiffness = Spring.StiffnessMedium)
private val SpringSpecDp = spring<Dp>(stiffness = Spring.StiffnessMedium)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSwitcherBottomSheet(
    savedCharts: List<SavedChart>,
    selectedChartId: Long?,
    onChartSelected: (SavedChart) -> Unit,
    onAddNewChart: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val hapticFeedback = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.BottomSheetBackground,
        contentColor = AppTheme.TextPrimary,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { ProfileSwitcherDragHandle() },
        windowInsets = WindowInsets.navigationBars
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            ProfileSwitcherHeader()

            HorizontalDivider(
                color = AppTheme.DividerColor.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            if (savedCharts.isEmpty()) {
                ProfileSwitcherEmptyState()
            } else {
                ProfileChartsList(
                    charts = savedCharts,
                    selectedChartId = selectedChartId,
                    onChartSelected = { chart ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onChartSelected(chart)
                    }
                )
            }

            HorizontalDivider(
                color = AppTheme.DividerColor.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            AddNewChartButton(
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddNewChart()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileSwitcherDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 14.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(AppTheme.BottomSheetHandle.copy(alpha = 0.4f))
    )
}

@Composable
private fun ProfileSwitcherHeader() {
    Text(
        text = "Switch Profile",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.TextPrimary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    )
}

@Composable
private fun ProfileSwitcherEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(AppTheme.CardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = AppTheme.TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "No saved charts",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextMuted
            )

            Text(
                text = "Add your first chart to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.TextSubtle
            )
        }
    }
}

@Composable
private fun ProfileChartsList(
    charts: List<SavedChart>,
    selectedChartId: Long?,
    onChartSelected: (SavedChart) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = charts,
            key = { chart -> chart.id }
        ) { chart ->
            ProfileItem(
                chart = chart,
                isSelected = chart.id == selectedChartId,
                onClick = { onChartSelected(chart) }
            )
        }
    }
}

@Composable
private fun ProfileItem(
    chart: SavedChart,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.AccentPrimary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        animationSpec = SpringSpec,
        label = "profile_item_bg"
    )

    val formattedDetails = remember(chart.dateTime, chart.location) {
        formatChartDetails(chart)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppTheme.AccentPrimary.copy(alpha = 0.12f)),
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                contentDescription = buildString {
                    append(chart.name)
                    append(", ")
                    append(formattedDetails)
                    if (isSelected) append(", selected")
                }
                selected = isSelected
                role = Role.Button
            }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            name = chart.name,
            isSelected = isSelected,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = chart.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AppTheme.TextPrimary else AppTheme.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formattedDetails,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                    scaleIn(spring(stiffness = Spring.StiffnessMedium), initialScale = 0.8f),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    scaleOut(spring(stiffness = Spring.StiffnessMedium), targetScale = 0.8f)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppTheme.AccentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Stable
@Composable
fun ProfileAvatar(
    name: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val initials = remember(name) {
        extractInitials(name)
    }

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = SpringSpecDp,
        label = "avatar_border_width"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppTheme.AccentPrimary else AppTheme.BorderColor,
        animationSpec = SpringSpec,
        label = "avatar_border_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppTheme.AccentPrimary else AppTheme.TextSecondary,
        animationSpec = SpringSpec,
        label = "avatar_text_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.AccentPrimary.copy(alpha = 0.1f)
        } else {
            AppTheme.CardBackground
        },
        animationSpec = SpringSpec,
        label = "avatar_bg"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = (size.value / 2.4f).sp
        )
    }
}

@Composable
private fun AddNewChartButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AppTheme.AccentPrimary.copy(alpha = 0.12f)),
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                contentDescription = "Add new birth chart"
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppTheme.AccentPrimary.copy(alpha = 0.08f))
                .border(
                    width = 1.5.dp,
                    color = AppTheme.AccentPrimary.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = AppTheme.AccentPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Add new chart",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = AppTheme.AccentPrimary
        )
    }
}

@Composable
fun ProfileHeaderRow(
    currentChart: SavedChart?,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = true,
                    color = AppTheme.AccentPrimary.copy(alpha = 0.1f)
                ),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onProfileClick()
                },
                role = Role.DropdownList
            )
            .semantics {
                contentDescription = currentChart?.let {
                    "Current profile: ${it.name}. Tap to switch profiles"
                } ?: "No profile selected. Tap to select a profile"
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentChart != null) {
            ProfileAvatar(
                name = currentChart.name,
                isSelected = true,
                size = 32.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = currentChart.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppTheme.CardBackground)
                    .border(1.dp, AppTheme.BorderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = AppTheme.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Select Profile",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.TextMuted
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = AppTheme.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun extractInitials(name: String): String {
    return name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { word ->
            word.firstOrNull { it.isLetter() }?.uppercaseChar()
        }
        .joinToString("")
        .ifEmpty { "?" }
}

private fun formatChartDetails(chart: SavedChart): String {
    val formattedDate = try {
        val dateTime = LocalDateTime.parse(
            chart.dateTime,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
        )
        dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: DateTimeParseException) {
        null
    } catch (_: Exception) {
        null
    }

    val location = chart.location.trim().takeIf { it.isNotBlank() }

    return when {
        formattedDate != null && location != null -> "$formattedDate • $location"
        formattedDate != null -> formattedDate
        location != null -> location
        else -> "Birth chart"
    }
}