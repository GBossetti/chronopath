package com.chronopath.locationtracker.data.mapper

import com.chronopath.locationtracker.data.local.entity.LocationEntity
import com.chronopath.locationtracker.domain.model.Location
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationMapperTest {

    private val testInstant = Instant.parse("2024-01-15T10:30:00Z")
    private val testInstallationId = "test-installation-id"

    @Test
    fun `mapToEntity maps all fields correctly`() {
        val domain = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            accuracy = 10.5f,
            altitude = 100.0,
            speed = 5.5f,
            bearing = 180.0f,
            provider = "gps",
            batteryPercentage = 85,
            isCharging = true,
            networkType = "WIFI",
            installationId = testInstallationId,
            advertisingId = "test-ad-id"
        )

        val entity = LocationMapper.mapToEntity(domain)

        assertEquals(domain.latitude, entity.latitude, 0.0001)
        assertEquals(domain.longitude, entity.longitude, 0.0001)
        assertEquals(domain.timestamp, entity.timestamp)
        assertEquals(domain.accuracy, entity.accuracy)
        assertEquals(domain.altitude, entity.altitude)
        assertEquals(domain.speed, entity.speed)
        assertEquals(domain.bearing, entity.bearing)
        assertEquals(domain.provider, entity.provider)
        assertEquals(domain.batteryPercentage, entity.batteryPercentage)
        assertEquals(domain.isCharging, entity.isCharging)
        assertEquals(domain.networkType, entity.networkType)
        assertEquals(domain.installationId, entity.installationId)
        assertEquals(domain.advertisingId, entity.advertisingId)
    }

    @Test
    fun `mapToEntity handles nullable fields as null`() {
        val domain = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )

        val entity = LocationMapper.mapToEntity(domain)

        assertEquals(domain.latitude, entity.latitude, 0.0001)
        assertEquals(domain.longitude, entity.longitude, 0.0001)
        assertEquals(domain.timestamp, entity.timestamp)
        assertNull(entity.accuracy)
        assertNull(entity.altitude)
        assertNull(entity.speed)
        assertNull(entity.bearing)
        assertNull(entity.provider)
        assertNull(entity.batteryPercentage)
        assertNull(entity.isCharging)
        assertNull(entity.networkType)
        assertEquals(domain.installationId, entity.installationId)
        assertNull(entity.advertisingId)
    }

    @Test
    fun `mapToDomain maps all fields correctly`() {
        val entity = LocationEntity(
            id = 1,
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            accuracy = 10.5f,
            altitude = 100.0,
            speed = 5.5f,
            bearing = 180.0f,
            provider = "gps",
            batteryPercentage = 85,
            isCharging = true,
            networkType = "WIFI",
            installationId = testInstallationId,
            advertisingId = "test-ad-id"
        )

        val domain = LocationMapper.mapToDomain(entity)

        assertEquals(entity.latitude, domain.latitude, 0.0001)
        assertEquals(entity.longitude, domain.longitude, 0.0001)
        assertEquals(entity.timestamp, domain.timestamp)
        assertEquals(entity.accuracy, domain.accuracy)
        assertEquals(entity.altitude, domain.altitude)
        assertEquals(entity.speed, domain.speed)
        assertEquals(entity.bearing, domain.bearing)
        assertEquals(entity.provider, domain.provider)
        assertEquals(entity.batteryPercentage, domain.batteryPercentage)
        assertEquals(entity.isCharging, domain.isCharging)
        assertEquals(entity.networkType, domain.networkType)
        assertEquals(entity.installationId, domain.installationId)
        assertEquals(entity.advertisingId, domain.advertisingId)
    }

    @Test
    fun `mapToDomain handles nullable fields as null`() {
        val entity = LocationEntity(
            id = 1,
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )

        val domain = LocationMapper.mapToDomain(entity)

        assertEquals(entity.latitude, domain.latitude, 0.0001)
        assertEquals(entity.longitude, domain.longitude, 0.0001)
        assertEquals(entity.timestamp, domain.timestamp)
        assertNull(domain.accuracy)
        assertNull(domain.altitude)
        assertNull(domain.speed)
        assertNull(domain.bearing)
        assertNull(domain.provider)
        assertNull(domain.batteryPercentage)
        assertNull(domain.isCharging)
        assertNull(domain.networkType)
        assertEquals(entity.installationId, domain.installationId)
        assertNull(domain.advertisingId)
    }

    @Test
    fun `toDomainList returns empty list for empty input`() {
        val result = LocationMapper.toDomainList(emptyList())

        assertEquals(0, result.size)
    }

    @Test
    fun `toDomainList maps multiple entities correctly`() {
        val entities = listOf(
            LocationEntity(
                id = 1,
                latitude = 45.5,
                longitude = -122.6,
                timestamp = testInstant,
                installationId = testInstallationId
            ),
            LocationEntity(
                id = 2,
                latitude = 40.7,
                longitude = -74.0,
                timestamp = testInstant,
                installationId = testInstallationId
            )
        )

        val result = LocationMapper.toDomainList(entities)

        assertEquals(2, result.size)
        assertEquals(45.5, result[0].latitude, 0.0001)
        assertEquals(40.7, result[1].latitude, 0.0001)
    }

    @Test
    fun `round trip domain to entity to domain preserves data`() {
        val original = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            accuracy = 10.5f,
            altitude = 100.0,
            speed = 5.5f,
            bearing = 180.0f,
            provider = "gps",
            batteryPercentage = 85,
            isCharging = true,
            networkType = "WIFI",
            installationId = testInstallationId,
            advertisingId = "test-ad-id"
        )

        val entity = LocationMapper.mapToEntity(original)
        val result = LocationMapper.mapToDomain(entity)

        assertEquals(original.latitude, result.latitude, 0.0001)
        assertEquals(original.longitude, result.longitude, 0.0001)
        assertEquals(original.timestamp, result.timestamp)
        assertEquals(original.accuracy, result.accuracy)
        assertEquals(original.altitude, result.altitude)
        assertEquals(original.speed, result.speed)
        assertEquals(original.bearing, result.bearing)
        assertEquals(original.provider, result.provider)
        assertEquals(original.batteryPercentage, result.batteryPercentage)
        assertEquals(original.isCharging, result.isCharging)
        assertEquals(original.networkType, result.networkType)
        assertEquals(original.installationId, result.installationId)
        assertEquals(original.advertisingId, result.advertisingId)
    }
}
