package com.astro.storm.data.model

import java.time.LocalDateTime

/**
 * Gender options for astrological chart analysis.
 * Used for gender-specific interpretations in certain astrological traditions.
 */
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    PREFER_NOT_TO_SAY("Prefer not to say");

    companion object {
        fun fromString(value: String?): Gender {
            return entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: PREFER_NOT_TO_SAY
        }
    }
}

/**
 * Birth data for chart calculation
 */
data class BirthData(
    val name: String,
    val dateTime: LocalDateTime,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val location: String,
    val gender: Gender = Gender.PREFER_NOT_TO_SAY
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees" }
    }
}
