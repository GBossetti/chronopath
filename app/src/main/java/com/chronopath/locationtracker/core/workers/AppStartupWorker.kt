package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
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
            val wasTrackingActive = WorkerUtils.isTrackingActive(applicationContext)
            Timber.tag("Worker").d("Was tracking active before exit: $wasTrackingActive")

            if (wasTrackingActive) {
                // Restart tracking service
                Timber.tag("Worker").i("Restoring tracking service on startup")
                WorkerUtils.startTrackingService(applicationContext)
                Result.success(WorkerUtils.actionData("tracking_restored_on_startup"))
            } else {
                Timber.tag("Worker").d("No tracking to restore")
                Result.success(WorkerUtils.actionData("no_tracking_to_restore"))
            }
        } catch (e: Exception) {
            // Don't fail on startup - just log and continue
            Timber.tag("Worker").e(e, "Startup worker encountered error")
            Result.success(WorkerUtils.errorData(e.message ?: "startup_error"))
        }
    }
}
