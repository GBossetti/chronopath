package com.chronopath.locationtracker.core.controller

import android.content.Context
import com.chronopath.locationtracker.core.common.Constants
import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.domain.controller.TrackingController

/**
 * Implementation of TrackingController that delegates to LocationTrackingService.
 * Lives in the core layer as it depends on Android Context.
 */
class TrackingControllerImpl(
    private val context: Context
) : TrackingController {

    override suspend fun startTracking(): Result<Boolean> {
        return try {
            LocationTrackingService.start(context)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e, "Failed to start tracking service")
        }
    }

    override suspend fun stopTracking(): Result<Boolean> {
        return try {
            LocationTrackingService.stop(context)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e, "Failed to stop tracking service")
        }
    }

    override fun isTracking(): Boolean {
        return LocationTrackingService.isRunning(context)
    }

    /**
     * Checks if tracking was active before the app was closed.
     * Used by workers to restore tracking state.
     */
    fun wasTrackingActiveBeforeExit(): Boolean {
        val prefs = context.getSharedPreferences(
            Constants.PREFS_TRACKING_ACTIVE,
            Context.MODE_PRIVATE
        )
        return prefs.getBoolean("is_tracking_active", false)
    }
}
