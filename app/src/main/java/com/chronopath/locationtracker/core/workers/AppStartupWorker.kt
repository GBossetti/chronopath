package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.data.settings.SettingsRepository
import timber.log.Timber


/**
 * Worker that runs when the app starts to restore tracking state if needed.
 */
class AppStartupWorker (
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORKER_TAG = "app_startup_worker"

        fun createWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<AppStartupWorker>()
                .addTag(WORKER_TAG)
                .build()
        }
    }

    override suspend fun doWork(): Result {
        Timber.tag("Worker").i("AppStartupWorker - Starting startup check")
        return try {
            // Check if tracking was active before app was closed
            val wasTrackingActive = wasTrackingActiveBeforeExit()
            Timber.tag("Worker").d("Was tracking active before exit: $wasTrackingActive")

            if (wasTrackingActive) {
                // Restart tracking service
                Timber.tag("Worker").i("Restoring tracking service on startup")
                restartTrackingService()

                Result.success(
                    Data.Builder()
                        .putString("action", "tracking_restored_on_startup")
                        .build()
                )
            } else {
                Timber.tag("Worker").d("No tracking to restore")
                Result.success(
                    Data.Builder()
                        .putString("action", "no_tracking_to_restore")
                        .build()
                )
            }
        } catch (e: Exception) {
            // Don't fail on startup - just log and continue
            Timber.tag("Worker").e(e, "Startup worker encountered error")
            Result.success(
                Data.Builder()
                    .putString("error", e.message ?: "startup_error")
                    .build()
            )
        }
    }

    private suspend fun wasTrackingActiveBeforeExit(): Boolean {
        val settingsRepository = SettingsRepository(applicationContext)
        return settingsRepository.getIsTrackingActive()
    }

    private fun restartTrackingService() {
        LocationTrackingService.start(applicationContext)
    }
}
