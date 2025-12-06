package com.astro.storm.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Geocoding Service using OpenStreetMap Nominatim API
 *
 * This service provides free geocoding (location to coordinates) and
 * reverse geocoding (coordinates to location) using the Nominatim API.
 *
 * API Documentation: https://nominatim.org/release-docs/latest/api/Search/
 *
 * Usage Guidelines:
 * - Maximum of 1 request per second
 * - Requires a valid User-Agent header
 * - Results are cached on the server, repeat queries are fast
 */
object GeocodingService {
    private const val TAG = "GeocodingService"
    private const val BASE_URL = "https://nominatim.openstreetmap.org"
    private const val USER_AGENT = "AstroStorm/1.0 (Vedic Astrology App)"
    private const val TIMEOUT_MS = 10000

    /**
     * Search result from geocoding query
     */
    data class GeocodingResult(
        val displayName: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val importance: Double
    )

    /**
     * Search for locations by name using Nominatim API
     *
     * @param query The search query (city, address, or place name)
     * @param limit Maximum number of results to return (default 5)
     * @return List of matching locations with coordinates
     */
    suspend fun searchLocation(
        query: String,
        limit: Int = 5
    ): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("$BASE_URL/search?format=json&q=$encodedQuery&limit=$limit&addressdetails=1")

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Geocoding request failed with code: $responseCode")
                    return@withContext Result.failure(
                        Exception("Geocoding request failed with code: $responseCode")
                    )
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val results = parseSearchResults(response)
                Result.success(results)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding search failed", e)
            Result.failure(e)
        }
    }

    /**
     * Reverse geocode coordinates to get location name
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @return Location name or null if not found
     */
    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/reverse?format=json&lat=$latitude&lon=$longitude&zoom=10")

            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Reverse geocoding failed with code: $responseCode")
                    return@withContext Result.failure(
                        Exception("Reverse geocoding failed with code: $responseCode")
                    )
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val locationName = parseReverseResult(response)
                Result.success(locationName)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed", e)
            Result.failure(e)
        }
    }

    /**
     * Search for a location and return the best match
     *
     * @param query The search query
     * @return Best matching location or null if not found
     */
    suspend fun searchBestMatch(query: String): Result<GeocodingResult?> {
        return searchLocation(query, limit = 1).map { it.firstOrNull() }
    }

    private fun parseSearchResults(json: String): List<GeocodingResult> {
        return try {
            val jsonArray = JSONArray(json)
            val results = mutableListOf<GeocodingResult>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    GeocodingResult(
                        displayName = obj.getString("display_name"),
                        latitude = obj.getDouble("lat"),
                        longitude = obj.getDouble("lon"),
                        type = obj.optString("type", "unknown"),
                        importance = obj.optDouble("importance", 0.0)
                    )
                )
            }

            // Sort by importance (higher is better)
            results.sortedByDescending { it.importance }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse search results", e)
            emptyList()
        }
    }

    private fun parseReverseResult(json: String): String? {
        return try {
            val obj = org.json.JSONObject(json)
            obj.optString("display_name").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse reverse result", e)
            null
        }
    }

    /**
     * Format a location result for display
     * Returns a shorter version of the display name
     */
    fun formatDisplayName(displayName: String): String {
        val parts = displayName.split(",").map { it.trim() }
        return when {
            parts.size >= 3 -> "${parts[0]}, ${parts[1]}, ${parts.last()}"
            parts.size == 2 -> "${parts[0]}, ${parts[1]}"
            else -> parts.firstOrNull() ?: displayName
        }
    }
}
