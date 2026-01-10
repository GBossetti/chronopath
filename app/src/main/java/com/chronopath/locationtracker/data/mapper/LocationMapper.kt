package com.chronopath.locationtracker.data.mapper

import com.chronopath.locationtracker.data.local.entity.LocationEntity
import com.chronopath.locationtracker.domain.model.Location

/**
 * Mapper for converting between domain models and data entities.
 * Follows the Data Mapper pattern.
 */
object LocationMapper {
    
    // Maps domain Location to database LocationEntity
    fun mapToEntity(domain: Location): LocationEntity {
        return LocationEntity(
            latitude = domain.latitude,
            longitude = domain.longitude,
            timestamp = domain.timestamp,
            accuracy = domain.accuracy,
            altitude = domain.altitude,
            speed = domain.speed,
            bearing = domain.bearing,
            provider = domain.provider,
            batteryPercentage = domain.batteryPercentage,
            isCharging = domain.isCharging,
            networkType = domain.networkType,
            installationId = domain.installationId,
            advertisingId = domain.advertisingId
        )
    }

    // Maps database LocationEntity to domain Location
    fun mapToDomain(entity: LocationEntity): Location {
        return Location(
            latitude = entity.latitude,
            longitude = entity.longitude,
            timestamp = entity.timestamp,
            accuracy = entity.accuracy,
            altitude = entity.altitude,
            speed = entity.speed,
            bearing = entity.bearing,
            provider = entity.provider,
            batteryPercentage = entity.batteryPercentage,
            isCharging = entity.isCharging,
            networkType = entity.networkType,
            installationId = entity.installationId,
            advertisingId = entity.advertisingId
        )
    }

    fun toDomainList(entities: List<LocationEntity>): List<Location> {
        return entities.map { mapToDomain(it) }
    }
}
