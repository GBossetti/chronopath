package com.chronopath.locationtracker.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.local.entity.LocationEntity
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for LocationDao using a real in-memory Room database.
 * These tests verify the reactive behavior of Room Flows.
 */
@RunWith(AndroidJUnit4::class)
class LocationDaoTest {

    private lateinit var database: LocationDatabase
    private lateinit var dao: LocationDao

    private val testInstant = Instant.parse("2024-01-15T10:30:00Z")
    private val testInstallationId = "test-installation-id"

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LocationDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.locationDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun createTestEntity(
        latitude: Double = 45.5,
        longitude: Double = -122.6
    ): LocationEntity {
        return LocationEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = testInstant,
            installationId = testInstallationId
        )
    }

    @Test
    fun insertLocation_and_getAllLocations_returnsInsertedLocation() = runTest {
        val entity = createTestEntity()

        dao.insertLocation(entity)

        dao.getAllLocations().test {
            val locations = awaitItem()
            assertEquals(1, locations.size)
            assertEquals(45.5, locations[0].latitude, 0.0001)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getAllLocations_emitsUpdates_whenNewLocationInserted() = runTest {
        dao.getAllLocations().test {
            // Initial state - empty
            assertEquals(0, awaitItem().size)

            // Insert first location
            dao.insertLocation(createTestEntity(latitude = 45.5))
            assertEquals(1, awaitItem().size)

            // Insert second location
            dao.insertLocation(createTestEntity(latitude = 40.7))
            assertEquals(2, awaitItem().size)

            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * THIS TEST SHOULD FAIL WITH CURRENT IMPLEMENTATION!
     *
     * Current DAO has: suspend fun getLocationCount(): Int
     * This returns a one-shot value, not a reactive Flow.
     *
     * The test expects: fun getLocationCount(): Flow<Int>
     * Which would emit updates when the database changes.
     *
     * This is the TDD "Red" phase - we write the failing test first,
     * then fix the implementation to make it pass.
     */
    @Test
    fun getLocationCount_emitsUpdates_whenNewLocationInserted() = runTest {
        dao.getLocationCount().test {
            // Initial count should be 0
            assertEquals(0, awaitItem())

            // Insert a location
            dao.insertLocation(createTestEntity())

            // Count should update to 1
            // THIS WILL FAIL because current implementation completes after first emit
            assertEquals(1, awaitItem())

            // Insert another location
            dao.insertLocation(createTestEntity(latitude = 40.7))

            // Count should update to 2
            assertEquals(2, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteAllLocations_clearsDatabase() = runTest {
        // Insert some locations
        dao.insertLocation(createTestEntity(latitude = 45.5))
        dao.insertLocation(createTestEntity(latitude = 40.7))

        dao.getAllLocations().test {
            assertEquals(2, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }

        // Delete all
        dao.deleteAllLocations()

        dao.getAllLocations().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
