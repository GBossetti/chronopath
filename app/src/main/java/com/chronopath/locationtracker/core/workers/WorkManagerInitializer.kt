package com.chronopath.locationtracker.core.workers

import android.content.Context
import androidx.startup.Initializer
import androidx.work.Configuration
import androidx.work.WorkManager
import com.chronopath.locationtracker.core.di.AppModule
import java.util.concurrent.Executors

/**
 * Initializes WorkManager with custom configuration.
 * Runs automatically on app startup.
 */
class WorkManagerInitializer : Initializer<WorkManager> {

    override fun create(context: Context): WorkManager {
        // Initialize WorkManager with custom configuration
        val config = Configuration.Builder()
            .setExecutor(Executors.newFixedThreadPool(4))
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

        WorkManager.initialize(context, config)

        // Schedule initial workers
        scheduleInitialWorkers(context)

        return WorkManager.getInstance(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList() // No dependencies
    }

    private fun scheduleInitialWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Schedule health check worker (runs every 30 minutes)
        val healthRequest = TrackingHealthWorker.createWorkRequest()
        workManager.enqueueUniquePeriodicWork(
            TrackingHealthWorker.WORKER_TAG,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            healthRequest
        )

        // Schedule one-time startup worker
        val startupRequest = AppStartupWorker.createWorkRequest()
        workManager.enqueue(startupRequest)
    }
}