package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.chronopath.locationtracker.core.common.Constants
import com.chronopath.locationtracker.core.services.LocationTrackingService
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit


/**
 * Worker that checks if tracking service is running and restarts it if needed.
 * Runs periodically (minimum 15 minutes on Android).
 */
class TrackingHealthWorker (
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORKER_TAG = "tracking_health_worker"

        fun createWorkRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<TrackingHealthWorker>(
                30, // Minimum interval is 15 minutes, we use 30 for balance
                TimeUnit.MINUTES
            )
                .addTag(WORKER_TAG)
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.MINUTES
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        Timber.tag("Worker").i("TrackingHealthWorker - Starting health check")
        return try {
            // Check if tracking should be active (stored in SharedPreferences)
            val shouldBeActive = isTrackingSupposedToBeActive()
            Timber.tag("Worker").d("Tracking should be active: $shouldBeActive")

            if (shouldBeActive) {
                // Check if service is actually running
                val isServiceRunning = isForegroundServiceRunning()
                Timber.tag("Worker").d("Service currently running: $isServiceRunning")

                if (!isServiceRunning) {
                    // Service died, need to restart it
                    Timber.tag("Worker").w("Service not running but should be - attempting restart")
                    restartTrackingService()
                    // Wait a bit to ensure service starts
                    delay(2000)

                    // Verify restart succeeded
                    if (isForegroundServiceRunning()) {
                        Timber.tag("Worker").i("Service restarted successfully")
                        Result.success(
                            Data.Builder()
                                .putString("action", "service_restarted")
                                .build()
                        )
                    } else {
                        Timber.tag("Worker").e("Failed to restart service")
                        Result.failure(
                            Data.Builder()
                                .putString("error", "failed_to_restart_service")
                                .build()
                        )
                    }
                } else {
                    // Service is running normally
                    Timber.tag("Worker").i("Health check passed - service running normally")
                    Result.success(
                        Data.Builder()
                            .putString("action", "health_check_passed")
                            .build()
                    )
                }
            } else {
                // Tracking should not be active, nothing to do
                Timber.tag("Worker").d("Tracking not active - no action needed")
                Result.success(
                    Data.Builder()
                        .putString("action", "no_action_needed")
                        .build()
                )
            }
        } catch (e: Exception) {
            Timber.tag("Worker").e(e, "Health check failed with exception")
            Result.failure(
                Data.Builder()
                    .putString("error", e.message ?: "unknown_error")
                    .build()
            )
        }
    }

    private suspend fun isTrackingSupposedToBeActive(): Boolean {
        // Read from SharedPreferences
        val prefs = applicationContext.getSharedPreferences(
            Constants.PREFS_TRACKING_ACTIVE,
            Context.MODE_PRIVATE
        )
        return prefs.getBoolean("is_tracking_active", false)
    }

    private fun isForegroundServiceRunning(): Boolean {
        return LocationTrackingService.isRunning(applicationContext)
    }

    private fun restartTrackingService() {
        LocationTrackingService.start(applicationContext)
    }
}