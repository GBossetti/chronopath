package com.chronopath.locationtracker.domain.controller

import com.chronopath.locationtracker.core.common.Result

/**
 * Abstraction for controlling location tracking.
 * Follows Dependency Inversion Principle - domain layer depends on this interface,
 * implementation details are in the core layer.
 */
interface TrackingController {
    /**
     * Starts location tracking.
     * @return Result.Success(true) if tracking started successfully
     */
    suspend fun startTracking(): Result<Boolean>

    /**
     * Stops location tracking.
     * @return Result.Success(true) if tracking stopped successfully
     */
    suspend fun stopTracking(): Result<Boolean>

    /**
     * Checks if tracking is currently active.
     */
    fun isTracking(): Boolean
}
