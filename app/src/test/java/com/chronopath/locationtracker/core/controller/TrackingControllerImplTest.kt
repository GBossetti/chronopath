package com.chronopath.locationtracker.core.controller

import android.content.Context
import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.data.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrackingControllerImplTest {

    private lateinit var controller: TrackingControllerImpl
    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        settingsRepository = mockk()
        controller = TrackingControllerImpl(context, settingsRepository)
        mockkObject(LocationTrackingService)
    }

    @After
    fun tearDown() {
        unmockkObject(LocationTrackingService)
    }

    @Test
    fun `startTracking returns Success when service starts successfully`() = runTest {
        every { LocationTrackingService.start(context) } returns Unit

        val result = controller.startTracking()

        assertTrue(result is Result.Success)
        assertEquals(true, (result as Result.Success).data)
    }

    @Test
    fun `startTracking returns Error when service throws exception`() = runTest {
        val exception = RuntimeException("Service failed to start")
        every { LocationTrackingService.start(context) } throws exception

        val result = controller.startTracking()

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
        assertEquals("Failed to start tracking service", result.message)
    }

    @Test
    fun `stopTracking returns Success when service stops successfully`() = runTest {
        every { LocationTrackingService.stop(context) } returns Unit

        val result = controller.stopTracking()

        assertTrue(result is Result.Success)
        assertEquals(true, (result as Result.Success).data)
    }

    @Test
    fun `stopTracking returns Error when service throws exception`() = runTest {
        val exception = RuntimeException("Service failed to stop")
        every { LocationTrackingService.stop(context) } throws exception

        val result = controller.stopTracking()

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
        assertEquals("Failed to stop tracking service", result.message)
    }

    @Test
    fun `isTracking returns true when service is running`() {
        every { LocationTrackingService.isRunning(context) } returns true

        val result = controller.isTracking()

        assertTrue(result)
    }

    @Test
    fun `isTracking returns false when service is not running`() {
        every { LocationTrackingService.isRunning(context) } returns false

        val result = controller.isTracking()

        assertFalse(result)
    }

    @Test
    fun `wasTrackingActiveBeforeExit returns value from settings repository`() = runTest {
        coEvery { settingsRepository.getIsTrackingActive() } returns true

        val result = controller.wasTrackingActiveBeforeExit()

        assertTrue(result)
        coVerify { settingsRepository.getIsTrackingActive() }
    }

    @Test
    fun `wasTrackingActiveBeforeExit returns false when tracking was not active`() = runTest {
        coEvery { settingsRepository.getIsTrackingActive() } returns false

        val result = controller.wasTrackingActiveBeforeExit()

        assertFalse(result)
    }
}
