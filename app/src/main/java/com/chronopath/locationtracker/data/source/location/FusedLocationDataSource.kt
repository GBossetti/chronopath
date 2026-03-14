package com.chronopath.locationtracker.data.source.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.tasks.await
import com.chronopath.locationtracker.core.common.AppLogger

class FusedLocationDataSource(
    private val context: Context
) : LocationDataSource {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback? = null
    private val _locationFlow = MutableStateFlow<Location?>(null)

    override val locationUpdates: Flow<Location> = _locationFlow.filterNotNull()

    @SuppressLint("MissingPermission")
    override suspend fun startTracking(intervalMillis: Long, minDistanceMeters: Float) {
        AppLogger.i("Location", "startTracking - interval: ${intervalMillis}ms, minDistance: ${minDistanceMeters}m")
        // Stop any existing tracking
        stopTracking()

        // Create location request using NEW Builder API
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).apply {
            setMinUpdateIntervalMillis(intervalMillis / 2)  // Fastest interval is half of normal interval
            setMinUpdateDistanceMeters(minDistanceMeters)
            setWaitForAccurateLocation(false)
        }.build()
        AppLogger.d("Location", "LocationRequest configured with HIGH_ACCURACY priority")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    AppLogger.d("Location", "Location update received, accuracy: %.1fm, provider: %s".format(
                        location.accuracy, location.provider
                    ))
                    _locationFlow.value = location
                }
            }
        }

        locationCallback?.let { callback ->
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                context.mainLooper
            ).await()
        }
        AppLogger.i("Location", "Location updates started successfully")
    }

    override suspend fun stopTracking() {
        AppLogger.i("Location", "stopTracking - Removing location updates")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            AppLogger.d("Location", "Location callback removed")
        }
        locationCallback = null
    }

    override fun isTrackingActive(): Boolean {
        return locationCallback != null
    }
}