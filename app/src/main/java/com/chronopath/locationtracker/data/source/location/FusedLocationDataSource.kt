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
import timber.log.Timber

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
        Timber.tag("Location").i("startTracking - interval: ${intervalMillis}ms, minDistance: ${minDistanceMeters}m")
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
        Timber.tag("Location").d("LocationRequest configured with HIGH_ACCURACY priority")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Timber.tag("Location").d("Location update - lat: %.6f, lon: %.6f, accuracy: %.1fm, provider: %s".format(
                        location.latitude, location.longitude, location.accuracy, location.provider
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
        Timber.tag("Location").i("Location updates started successfully")
    }

    override suspend fun stopTracking() {
        Timber.tag("Location").i("stopTracking - Removing location updates")
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            Timber.tag("Location").d("Location callback removed")
        }
        locationCallback = null
    }

    override fun isTrackingActive(): Boolean {
        return locationCallback != null
    }
}