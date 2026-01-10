package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant

/**
 * Core business entity representing a location point.
 * Contains all data that can be collected from device sensors.
 * All fields except lat/long/timestamp can be null if unavailable.
 */
data class Location(
    // Core required fields
    val latitude: Double,
    val longitude: Double,
    val timestamp: Instant,
    
    // Extended location data (nullable)
    val accuracy: Float? = null,      // Horizontal accuracy in meters
    val altitude: Double? = null,     // Meters above sea level (GPS only)
    val speed: Float? = null,         // m/s (calculated by Android)
    val bearing: Float? = null,       // Degrees from true north
    val provider: String? = null,     // "gps", "network", "fused"
    
    // Device context (nullable)
    val batteryPercentage: Int? = null,   // 0-100%
    val isCharging: Boolean? = null,      // Charging state
    val networkType: String? = null,      // "WIFI", "MOBILE", "OFFLINE"
    
    // Device identification
    val installationId: String,           // Persistent UUID
    val advertisingId: String? = null     // AAID (optional)
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
    
    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}
