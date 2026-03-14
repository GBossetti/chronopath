package com.chronopath.locationtracker.core.controller

import android.content.Context
import com.chronopath.locationtracker.core.common.AppLogger
import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.data.settings.SettingsRepository
import com.chronopath.locationtracker.domain.controller.TrackingController

/**
 * Implementation of TrackingController that delegates to LocationTrackingService.
 * Lives in the core layer as it depends on Android Context.
 */
class TrackingControllerImpl(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : TrackingController {

    override suspend fun startTracking(): Result<Boolean> {
        AppLogger.i("Controller", "startTracking()")
        return try {
            LocationTrackingService.start(context)
            AppLogger.d("Controller", "startTracking() — service intent sent")
            Result.Success(true)
        } catch (e: Exception) {
            AppLogger.e("Controller", "startTracking() failed", e)
            Result.Error(e, "Failed to start tracking service")
        }
    }

    override suspend fun stopTracking(): Result<Boolean> {
        AppLogger.i("Controller", "stopTracking()")
        return try {
            LocationTrackingService.stop(context)
            AppLogger.d("Controller", "stopTracking() — service intent sent")
            Result.Success(true)
        } catch (e: Exception) {
            AppLogger.e("Controller", "stopTracking() failed", e)
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
    suspend fun wasTrackingActiveBeforeExit(): Boolean {
        return settingsRepository.getIsTrackingActive()
    }
}
