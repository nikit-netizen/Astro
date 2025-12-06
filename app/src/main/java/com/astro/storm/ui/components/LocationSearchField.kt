package com.astro.storm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astro.storm.data.api.GeocodingService
import com.astro.storm.ui.theme.AppTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Location Search Field with Nominatim geocoding integration
 *
 * Features:
 * - Real-time search suggestions as user types
 * - Debounced API calls to prevent rate limiting
 * - Loading indicator during search
 * - Auto-fill coordinates when location is selected
 * - Clear button to reset search
 */
@Composable
fun LocationSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (location: String, latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Search location",
    placeholder: String = "Enter city or place name"
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchResults by remember { mutableStateOf<List<GeocodingService.GeocodingResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Debounced search function
    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 3) {
            searchResults = emptyList()
            showResults = false
            return
        }

        searchJob = scope.launch {
            delay(500) // Debounce delay
            isSearching = true
            errorMessage = null

            val result = GeocodingService.searchLocation(query, limit = 5)
            result.onSuccess { results ->
                searchResults = results
                showResults = results.isNotEmpty()
            }.onFailure { error ->
                errorMessage = "Search failed: ${error.message}"
                searchResults = emptyList()
            }

            isSearching = false
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                onValueChange(newValue)
                performSearch(newValue)
            },
            label = {
                Text(label, color = AppTheme.TextMuted, fontSize = 14.sp)
            },
            placeholder = {
                Text(placeholder, color = AppTheme.TextSubtle, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = AppTheme.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = AppTheme.AccentGold,
                            strokeWidth = 2.dp
                        )
                    }
                    if (value.isNotEmpty() && !isSearching) {
                        IconButton(
                            onClick = {
                                onValueChange("")
                                searchResults = emptyList()
                                showResults = false
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear",
                                tint = AppTheme.TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && searchResults.isNotEmpty()) {
                        showResults = true
                    }
                },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppTheme.TextPrimary,
                unfocusedTextColor = AppTheme.TextPrimary,
                focusedBorderColor = AppTheme.AccentGold,
                unfocusedBorderColor = AppTheme.BorderColor,
                focusedLabelColor = AppTheme.AccentGold,
                unfocusedLabelColor = AppTheme.TextMuted,
                cursorColor = AppTheme.AccentGold
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    performSearch(value)
                    focusManager.clearFocus()
                }
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = AppTheme.ErrorColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Search Results Dropdown
        AnimatedVisibility(
            visible = showResults && searchResults.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.CardBackground,
                shadowElevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(searchResults) { result ->
                        LocationResultItem(
                            result = result,
                            onClick = {
                                val formattedName = GeocodingService.formatDisplayName(result.displayName)
                                onValueChange(formattedName)
                                onLocationSelected(
                                    formattedName,
                                    result.latitude,
                                    result.longitude
                                )
                                showResults = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationResultItem(
    result: GeocodingService.GeocodingResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppTheme.AccentGold.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = AppTheme.AccentGold,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Extract main location name (first part)
            val parts = result.displayName.split(",").map { it.trim() }
            val mainName = parts.firstOrNull() ?: result.displayName
            val details = if (parts.size > 1) {
                parts.drop(1).take(2).joinToString(", ")
            } else ""

            Text(
                text = mainName,
                color = AppTheme.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    color = AppTheme.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Show coordinates
            Text(
                text = "${String.format("%.4f", result.latitude)}°, ${String.format("%.4f", result.longitude)}°",
                color = AppTheme.TextSubtle,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Simple location search that returns coordinates for a query
 */
@Composable
fun rememberLocationSearchState(): LocationSearchState {
    return remember { LocationSearchState() }
}

class LocationSearchState {
    var isSearching by mutableStateOf(false)
        private set
    var results by mutableStateOf<List<GeocodingService.GeocodingResult>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    suspend fun search(query: String, limit: Int = 5) {
        if (query.length < 3) {
            results = emptyList()
            return
        }

        isSearching = true
        error = null

        val result = GeocodingService.searchLocation(query, limit)
        result.onSuccess { searchResults ->
            results = searchResults
        }.onFailure { e ->
            error = e.message
            results = emptyList()
        }

        isSearching = false
    }

    fun clear() {
        results = emptyList()
        error = null
    }
}
