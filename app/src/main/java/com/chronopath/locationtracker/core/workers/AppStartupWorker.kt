package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.chronopath.locationtracker.core.common.Constants
import com.chronopath.locationtracker.core.services.LocationTrackingService


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
        return try {
            // Check if tracking was active before app was closed
            val wasTrackingActive = wasTrackingActiveBeforeExit()

            if (wasTrackingActive) {
                // Restart tracking service
                restartTrackingService()

                Result.success(
                    Data.Builder()
                        .putString("action", "tracking_restored_on_startup")
                        .build()
                )
            } else {
                Result.success(
                    Data.Builder()
                        .putString("action", "no_tracking_to_restore")
                        .build()
                )
            }
        } catch (e: Exception) {
            // Don't fail on startup - just log and continue
            Result.success(
                Data.Builder()
                    .putString("error", e.message ?: "startup_error")
                    .build()
            )
        }
    }

    private fun wasTrackingActiveBeforeExit(): Boolean {
        val prefs = applicationContext.getSharedPreferences(
            Constants.PREFS_TRACKING_ACTIVE,
            Context.MODE_PRIVATE
        )
        return prefs.getBoolean("is_tracking_active", false)
    }

    private fun restartTrackingService() {
        LocationTrackingService.start(applicationContext)
    }
}
