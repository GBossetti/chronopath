package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTest {

    private val testInstant = Instant.parse("2024-01-15T10:30:00Z")
    private val testInstallationId = "test-installation-id"

    @Test
    fun `valid location with minimum required fields`() {
        val location = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )

        assertTrue(location.isValid())
    }

    @Test
    fun `valid location at boundary values`() {
        val locationNorthPole = Location(
            latitude = 90.0,
            longitude = 0.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        assertTrue(locationNorthPole.isValid())

        val locationSouthPole = Location(
            latitude = -90.0,
            longitude = 0.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        assertTrue(locationSouthPole.isValid())

        val locationDateLine = Location(
            latitude = 0.0,
            longitude = 180.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        assertTrue(locationDateLine.isValid())

        val locationDateLineNegative = Location(
            latitude = 0.0,
            longitude = -180.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        assertTrue(locationDateLineNegative.isValid())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid latitude above 90 throws exception`() {
        Location(
            latitude = 90.1,
            longitude = 0.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid latitude below minus 90 throws exception`() {
        Location(
            latitude = -90.1,
            longitude = 0.0,
            timestamp = testInstant,
            installationId = testInstallationId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid longitude above 180 throws exception`() {
        Location(
            latitude = 0.0,
            longitude = 180.1,
            timestamp = testInstant,
            installationId = testInstallationId
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid longitude below minus 180 throws exception`() {
        Location(
            latitude = 0.0,
            longitude = -180.1,
            timestamp = testInstant,
            installationId = testInstallationId
        )
    }

    @Test
    fun `location with all optional fields`() {
        val location = Location(
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

        assertTrue(location.isValid())
    }
}
