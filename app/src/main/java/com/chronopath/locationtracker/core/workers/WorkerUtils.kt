package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.work.Data
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.data.settings.SettingsRepository

internal object WorkerUtils {

    fun actionData(action: String): Data {
        return Data.Builder()
            .putString("action", action)
            .build()
    }

    fun errorData(error: String): Data {
        return Data.Builder()
            .putString("error", error)
            .build()
    }

    fun startTrackingService(context: Context) {
        LocationTrackingService.start(context)
    }

    fun isServiceRunning(context: Context): Boolean {
        return LocationTrackingService.isRunning(context)
    }

    suspend fun isTrackingActive(context: Context): Boolean {
        val settingsRepository = SettingsRepository(context)
        return settingsRepository.getIsTrackingActive()
    }
}
