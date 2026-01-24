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
import com.chronopath.locationtracker.data.settings.SettingsRepository
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
import timber.log.Timber

/**
 * Foreground service for continuous location tracking.
 * Runs in the background and saves location data to the repository.
 */
class LocationTrackingService : Service() {

    companion object {
        const val ACTION_START = "com.chronopath.locationtracker.ACTION_START"
        const val ACTION_STOP = "com.chronopath.locationtracker.ACTION_STOP"

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
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        Timber.tag("Service").i("onCreate - LocationTrackingService created")
        NotificationHelper.createNotificationChannel(this)
        initializeDependencies()
        Timber.tag("Service").d("Dependencies initialized")
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
        settingsRepository = SettingsRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag("Service").d("onStartCommand - action: ${intent?.action}, flags: $flags, startId: $startId")
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        Timber.tag("Service").i("startTracking - Starting foreground service")
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
        Timber.tag("Service").d("Foreground notification displayed")

        serviceScope.launch {
            settingsRepository.setIsTrackingActive(true)
        }

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val intervalMs = settingsRepository.getTrackingIntervalMs()
            val minDistanceMeters = settingsRepository.getMinDistanceMeters()
            Timber.tag("Service").d("Starting location updates - interval: ${intervalMs}ms, minDistance: ${minDistanceMeters}m")
            // Start location updates
            locationDataSource.startTracking(
                intervalMillis = intervalMs,
                minDistanceMeters = minDistanceMeters
            )

            // Collect and save aggregated locations
            dataAggregator.getAggregatedLocations()
                .catch { e ->
                    Timber.tag("Service").e(e, "Error collecting location updates")
                }
                .collect { location ->
                    Timber.tag("Service").d("Location received - lat: %.6f, lon: %.6f, accuracy: %.1fm".format(
                        location.latitude, location.longitude, location.accuracy
                    ))
                    repository.saveLocation(location)
                }
        }
        Timber.tag("Service").i("Location tracking started successfully")
    }

    private fun stopTracking() {
        Timber.tag("Service").i("stopTracking - Stopping location tracking")
        trackingJob?.cancel()
        trackingJob = null

        serviceScope.launch {
            locationDataSource.stopTracking()
            Timber.tag("Service").d("Location updates stopped")
            settingsRepository.setIsTrackingActive(false)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.tag("Service").i("Service stopped")
    }

    override fun onDestroy() {
        Timber.tag("Service").i("onDestroy - Service being destroyed")
        super.onDestroy()
        trackingJob?.cancel()
        // Note: We don't set isTrackingActive to false here because the service being destroyed
        // doesn't mean the user stopped tracking - it could be a system kill that needs recovery
        serviceScope.cancel()
        Timber.tag("Service").d("Resources cleaned up")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
