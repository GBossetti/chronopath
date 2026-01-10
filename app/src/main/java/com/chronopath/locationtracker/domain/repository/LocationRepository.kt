package com.chronopath.locationtracker.domain.repository

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.domain.model.Location
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for location data operations.
 * Domain layer depends on this abstraction (Dependency Inversion Principle).
 */
interface LocationRepository {

    // Get all stored locations
    fun getAllLocations(): Flow<List<Location>>

    // Insert a single location
    suspend fun insertLocation(location: Location)

    // Get real-time tracked location stream
    fun getTrackedLocation(): Flow<Location>

    // Save location with Result wrapper
    suspend fun saveLocation(location: Location): Result<Boolean>

    // Get count of stored locations
    fun getLocationCount(): Flow<Int>
}