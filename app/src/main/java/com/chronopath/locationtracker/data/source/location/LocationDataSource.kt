package com.chronopath.locationtracker.data.source.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

/**
 * Interface for location data collection.
 * Abstracts the location provider implementation.
 */
interface LocationDataSource {
    /**
     * Stream of location updates.
     * Emits whenever a new location is available.
     */
    val locationUpdates: Flow<Location>

    /**
     * Starts location tracking with the specified configuration.
     * @param intervalMillis Time between updates in milliseconds
     * @param minDistanceMeters Minimum displacement between updates in meters
     */
    suspend fun startTracking(intervalMillis: Long, minDistanceMeters: Float)

    /**
     * Stops location tracking.
     */
    suspend fun stopTracking()

    /**
     * Checks if location tracking is currently active.
     */
    fun isTrackingActive(): Boolean
}