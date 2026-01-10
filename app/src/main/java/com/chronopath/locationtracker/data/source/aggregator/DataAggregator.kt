package com.chronopath.locationtracker.data.source.aggregator

import com.chronopath.locationtracker.data.source.battery.BatteryDataSource
import com.chronopath.locationtracker.data.source.id.DeviceIdManager
import com.chronopath.locationtracker.data.source.location.LocationDataSource
import com.chronopath.locationtracker.data.source.network.NetworkDataSource
import com.chronopath.locationtracker.domain.model.Location
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant

class DataAggregator(
    private val locationDataSource: LocationDataSource,
    private val batteryDataSource: BatteryDataSource,
    private val networkDataSource: NetworkDataSource,
    private val deviceIdManager: DeviceIdManager
) {
    fun getAggregatedLocations(): Flow<Location> {
        return combine(
            locationDataSource.locationUpdates,
            batteryDataSource.batteryPercentage,
            batteryDataSource.isCharging,
            networkDataSource.networkType
        ) { androidLocation, batteryPercent, isCharging, networkType ->
            Location(
                latitude = androidLocation.latitude,
                longitude = androidLocation.longitude,
                timestamp = Instant.fromEpochMilliseconds(androidLocation.time),
                accuracy = androidLocation.accuracy,
                altitude = if (androidLocation.hasAltitude()) androidLocation.altitude else null,
                speed = if (androidLocation.hasSpeed()) androidLocation.speed else null,
                bearing = if (androidLocation.hasBearing()) androidLocation.bearing else null,
                provider = androidLocation.provider,
                batteryPercentage = batteryPercent,
                isCharging = isCharging,
                networkType = networkType,
                installationId = deviceIdManager.getInstallationId(),
                advertisingId = null
            )
        }
    }
}