package com.astro.storm.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object GeocodingService {
    private const val TAG = "GeocodingService"
    private const val BASE_URL = "https://nominatim.openstreetmap.org"
    private const val USER_AGENT = "AstroStorm/1.0 (Vedic Astrology App; contact@astrostorm.app)"
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val READ_TIMEOUT_MS = 15000
    private const val RATE_LIMIT_DELAY_MS = 1100L
    private const val MAX_RETRIES = 2
    private const val CACHE_MAX_SIZE = 100

    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    private val searchCache = object : LinkedHashMap<String, List<GeocodingResult>>(
        CACHE_MAX_SIZE, 0.75f, true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<GeocodingResult>>): Boolean {
            return size > CACHE_MAX_SIZE
        }
    }
    private val cacheLock = Any()

    private val reverseCache = ConcurrentHashMap<String, String>(CACHE_MAX_SIZE)

    sealed class GeocodingError : Exception() {
        data class NetworkError(override val message: String, override val cause: Throwable? = null) : GeocodingError()
        data class ApiError(val code: Int, override val message: String) : GeocodingError()
        data class ParseError(override val message: String, override val cause: Throwable? = null) : GeocodingError()
        data class ValidationError(override val message: String) : GeocodingError()
        data object RateLimitExceeded : GeocodingError() {
            private fun readResolve(): Any = RateLimitExceeded
            override val message: String = "Rate limit exceeded. Please try again later."
        }
    }

    data class GeocodingResult(
        val displayName: String,
        val latitude: Double,
        val longitude: Double,
        val type: String,
        val importance: Double,
        val city: String?,
        val state: String?,
        val country: String?
    ) {
        val formattedShortName: String
            get() = buildString {
                append(city ?: displayName.split(",").firstOrNull()?.trim() ?: "Unknown")
                state?.let { append(", $it") }
                country?.let { append(", $it") }
            }

        val coordinatesFormatted: String
            get() = String.format(Locale.US, "%.4f°, %.4f°", latitude, longitude)
    }

    suspend fun searchLocation(
        query: String,
        limit: Int = 5
    ): Result<List<GeocodingResult>> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        
        if (trimmedQuery.isBlank()) {
            return@withContext Result.success(emptyList())
        }

        if (trimmedQuery.length < 2) {
            return@withContext Result.failure(
                GeocodingError.ValidationError("Query must be at least 2 characters")
            )
        }

        val cacheKey = "${trimmedQuery.lowercase(Locale.US)}_$limit"
        synchronized(cacheLock) {
            searchCache[cacheKey]?.let { cached ->
                Log.d(TAG, "Cache hit for query: $trimmedQuery")
                return@withContext Result.success(cached)
            }
        }

        executeWithRateLimit {
            executeWithRetry {
                performSearch(trimmedQuery, limit)
            }
        }.also { result ->
            result.onSuccess { results ->
                synchronized(cacheLock) {
                    searchCache[cacheKey] = results
                }
            }
        }
    }

    suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<String?> = withContext(Dispatchers.IO) {
        if (latitude < -90 || latitude > 90) {
            return@withContext Result.failure(
                GeocodingError.ValidationError("Latitude must be between -90 and 90")
            )
        }
        if (longitude < -180 || longitude > 180) {
            return@withContext Result.failure(
                GeocodingError.ValidationError("Longitude must be between -180 and 180")
            )
        }

        val cacheKey = String.format(Locale.US, "%.4f_%.4f", latitude, longitude)
        reverseCache[cacheKey]?.let { cached ->
            Log.d(TAG, "Reverse cache hit for: $cacheKey")
            return@withContext Result.success(cached)
        }

        executeWithRateLimit {
            executeWithRetry {
                performReverseGeocode(latitude, longitude)
            }
        }.also { result ->
            result.onSuccess { locationName ->
                locationName?.let { reverseCache[cacheKey] = it }
            }
        }
    }

    suspend fun searchBestMatch(query: String): Result<GeocodingResult?> {
        return searchLocation(query, limit = 1).map { it.firstOrNull() }
    }

    fun formatDisplayName(displayName: String): String {
        val parts = displayName.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return when {
            parts.size >= 3 -> "${parts[0]}, ${parts[1]}, ${parts.last()}"
            parts.size == 2 -> "${parts[0]}, ${parts[1]}"
            else -> parts.firstOrNull() ?: displayName
        }
    }

    fun clearCache() {
        synchronized(cacheLock) {
            searchCache.clear()
        }
        reverseCache.clear()
        Log.d(TAG, "Cache cleared")
    }

    private suspend fun <T> executeWithRateLimit(block: suspend () -> Result<T>): Result<T> {
        rateLimitMutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestTime
            if (elapsed < RATE_LIMIT_DELAY_MS) {
                delay(RATE_LIMIT_DELAY_MS - elapsed)
            }
            return block().also {
                lastRequestTime = System.currentTimeMillis()
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        maxRetries: Int = MAX_RETRIES,
        block: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Throwable? = null
        repeat(maxRetries + 1) { attempt ->
            val result = block()
            if (result.isSuccess) {
                return result
            }
            lastException = result.exceptionOrNull()
            if (lastException is GeocodingError.ValidationError ||
                lastException is GeocodingError.ApiError) {
                return result
            }
            if (attempt < maxRetries) {
                delay((attempt + 1) * 500L)
                Log.d(TAG, "Retry attempt ${attempt + 1} after failure")
            }
        }
        return Result.failure(lastException ?: GeocodingError.NetworkError("Unknown error"))
    }

    private fun performSearch(query: String, limit: Int): Result<List<GeocodingResult>> {
        var connection: HttpURLConnection? = null
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL(
                "$BASE_URL/search?format=json&q=$encodedQuery&limit=$limit&addressdetails=1"
            )

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = true
            }

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                return Result.failure(GeocodingError.RateLimitExceeded)
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "Search failed: $responseCode - $errorBody")
                return Result.failure(
                    GeocodingError.ApiError(responseCode, "Search request failed: $responseCode")
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val results = parseSearchResults(response)
            Result.success(results)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Search timeout", e)
            Result.failure(GeocodingError.NetworkError("Connection timeout. Please check your internet.", e))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "No internet", e)
            Result.failure(GeocodingError.NetworkError("No internet connection.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(GeocodingError.NetworkError("Search failed: ${e.message}", e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun performReverseGeocode(latitude: Double, longitude: Double): Result<String?> {
        var connection: HttpURLConnection? = null
        return try {
            val latStr = String.format(Locale.US, "%.6f", latitude)
            val lonStr = String.format(Locale.US, "%.6f", longitude)
            val url = URL("$BASE_URL/reverse?format=json&lat=$latStr&lon=$lonStr&zoom=10&addressdetails=1")

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Language", "en")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode == 429) {
                return Result.failure(GeocodingError.RateLimitExceeded)
            }
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Reverse geocoding failed: $responseCode")
                return Result.failure(
                    GeocodingError.ApiError(responseCode, "Reverse geocoding failed: $responseCode")
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val locationName = parseReverseResult(response)
            Result.success(locationName)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Reverse geocode timeout", e)
            Result.failure(GeocodingError.NetworkError("Connection timeout.", e))
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "No internet", e)
            Result.failure(GeocodingError.NetworkError("No internet connection.", e))
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocoding failed", e)
            Result.failure(GeocodingError.NetworkError("Reverse geocoding failed: ${e.message}", e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseSearchResults(json: String): List<GeocodingResult> {
        return try {
            val jsonArray = JSONArray(json)
            val results = mutableListOf<GeocodingResult>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val address = obj.optJSONObject("address")

                results.add(
                    GeocodingResult(
                        displayName = obj.getString("display_name"),
                        latitude = obj.getString("lat").toDouble(),
                        longitude = obj.getString("lon").toDouble(),
                        type = obj.optString("type", "unknown"),
                        importance = obj.optDouble("importance", 0.0),
                        city = address?.let {
                            it.optString("city").takeIf { s -> s.isNotBlank() }
                                ?: it.optString("town").takeIf { s -> s.isNotBlank() }
                                ?: it.optString("village").takeIf { s -> s.isNotBlank() }
                                ?: it.optString("municipality").takeIf { s -> s.isNotBlank() }
                        },
                        state = address?.optString("state")?.takeIf { it.isNotBlank() },
                        country = address?.optString("country")?.takeIf { it.isNotBlank() }
                    )
                )
            }

            results.sortedByDescending { it.importance }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse search results", e)
            throw GeocodingError.ParseError("Failed to parse results", e)
        }
    }

    private fun parseReverseResult(json: String): String? {
        return try {
            val obj = JSONObject(json)
            if (obj.has("error")) {
                Log.w(TAG, "Reverse geocode error: ${obj.optString("error")}")
                return null
            }
            obj.optString("display_name").takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse reverse result", e)
            null
        }
    }
}