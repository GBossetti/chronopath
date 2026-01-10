package com.chronopath.locationtracker.core.services

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.chronopath.locationtracker.core.common.Constants
import com.chronopath.locationtracker.core.di.AppModule
import com.chronopath.locationtracker.data.source.aggregator.DataAggregator
import com.chronopath.locationtracker.data.source.battery.impl.AndroidBatteryDataSource
import com.chronopath.locationtracker.data.source.id.DeviceIdManager
import com.chronopath.locationtracker.data.source.location.FusedLocationDataSource
import com.chronopath.locationtracker.data.source.network.impl.AndroidNetworkDataSource
import com.chronopath.locationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Foreground service for continuous location tracking.
 * Runs in the background and saves location data to the repository.
 */
class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.chronopath.locationtracker.ACTION_START"
        const val ACTION_STOP = "com.chronopath.locationtracker.ACTION_STOP"

        private const val PREFS_KEY_IS_TRACKING = "is_tracking_active"

        /**
         * Starts the location tracking service.
         */
        fun start(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stops the location tracking service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * Checks if the service is currently running.
         */
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            @Suppress("DEPRECATION")
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (LocationTrackingService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trackingJob: Job? = null

    private lateinit var locationDataSource: FusedLocationDataSource
    private lateinit var dataAggregator: DataAggregator
    private lateinit var repository: LocationRepository

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        initializeDependencies()
    }

    private fun initializeDependencies() {
        val deviceIdManager = DeviceIdManager(applicationContext)
        locationDataSource = FusedLocationDataSource(applicationContext)

        dataAggregator = DataAggregator(
            locationDataSource = locationDataSource,
            batteryDataSource = AndroidBatteryDataSource(applicationContext),
            networkDataSource = AndroidNetworkDataSource(applicationContext),
            deviceIdManager = deviceIdManager
        )

        repository = AppModule.provideLocationRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        val notification = NotificationHelper.buildTrackingNotification(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Constants.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID, notification)
        }

        updateTrackingState(true)

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            // Start location updates
            locationDataSource.startTracking(
                intervalMillis = Constants.DEFAULT_TRACKING_INTERVAL_MS,
                minDistanceMeters = Constants.DEFAULT_MIN_DISTANCE_METERS
            )

            // Collect and save aggregated locations
            dataAggregator.getAggregatedLocations()
                .catch { e ->
                    // Log error but keep service running
                    e.printStackTrace()
                }
                .collect { location ->
                    repository.saveLocation(location)
                }
        }
    }

    private fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null

        serviceScope.launch {
            locationDataSource.stopTracking()
        }

        updateTrackingState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateTrackingState(isActive: Boolean) {
        getSharedPreferences(Constants.PREFS_TRACKING_ACTIVE, MODE_PRIVATE)
            .edit()
            .putBoolean(PREFS_KEY_IS_TRACKING, isActive)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
        updateTrackingState(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
