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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    _locationFlow.value = location
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            context.mainLooper
        ).await()
    }

    override suspend fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    override fun isTrackingActive(): Boolean {
        return locationCallback != null
    }
}