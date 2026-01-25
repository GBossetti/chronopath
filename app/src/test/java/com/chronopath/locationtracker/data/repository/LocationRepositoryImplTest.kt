package com.chronopath.locationtracker.data.repository

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.local.dao.LocationDao
import com.chronopath.locationtracker.data.local.entity.LocationEntity
import com.chronopath.locationtracker.data.source.aggregator.DataAggregator
import com.chronopath.locationtracker.domain.model.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocationRepositoryImplTest {

    private lateinit var repository: LocationRepositoryImpl
    private lateinit var database: LocationDatabase
    private lateinit var locationDao: LocationDao
    private lateinit var aggregator: DataAggregator

    private val testInstant = Instant.parse("2024-01-15T10:30:00Z")
    private val testInstallationId = "test-installation-id"

    @Before
    fun setup() {
        locationDao = mockk()
        database = mockk {
            every { locationDao() } returns locationDao
        }
        aggregator = mockk()

        repository = LocationRepositoryImpl(
            database = database,
            aggregator = aggregator
        )
    }

    @Test
    fun `getAllLocations returns mapped domain objects`() = runTest {
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
        every { locationDao.getAllLocations() } returns flowOf(entities)

        val result = repository.getAllLocations().first()

        assertEquals(2, result.size)
        assertEquals(45.5, result[0].latitude, 0.0001)
        assertEquals(40.7, result[1].latitude, 0.0001)
    }

    @Test
    fun `getAllLocations returns empty list when no locations`() = runTest {
        every { locationDao.getAllLocations() } returns flowOf(emptyList())

        val result = repository.getAllLocations().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `insertLocation maps domain to entity and inserts`() = runTest {
        val location = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        coEvery { locationDao.insertLocation(any()) } returns Unit

        repository.insertLocation(location)

        coVerify {
            locationDao.insertLocation(match {
                it.latitude == 45.5 &&
                it.longitude == -122.6 &&
                it.installationId == testInstallationId
            })
        }
    }

    @Test
    fun `getTrackedLocation delegates to aggregator`() = runTest {
        val trackedLocation = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        every { aggregator.getAggregatedLocations() } returns flowOf(trackedLocation)

        val result = repository.getTrackedLocation().first()

        assertEquals(45.5, result.latitude, 0.0001)
        assertEquals(-122.6, result.longitude, 0.0001)
    }

    @Test
    fun `saveLocation returns Success on successful insert`() = runTest {
        val location = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        coEvery { locationDao.insertLocation(any()) } returns Unit

        val result = repository.saveLocation(location)

        assertTrue(result is Result.Success)
        assertEquals(true, (result as Result.Success).data)
    }

    @Test
    fun `saveLocation returns Error on exception`() = runTest {
        val location = Location(
            latitude = 45.5,
            longitude = -122.6,
            timestamp = testInstant,
            installationId = testInstallationId
        )
        val exception = RuntimeException("Database error")
        coEvery { locationDao.insertLocation(any()) } throws exception

        val result = repository.saveLocation(location)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
    }

    @Test
    fun `getLocationCount returns count from dao`() = runTest {
        coEvery { locationDao.getLocationCount() } returns 42

        val result = repository.getLocationCount().first()

        assertEquals(42, result)
    }

    @Test
    fun `getLocationCount returns zero when empty`() = runTest {
        coEvery { locationDao.getLocationCount() } returns 0

        val result = repository.getLocationCount().first()

        assertEquals(0, result)
    }
}
