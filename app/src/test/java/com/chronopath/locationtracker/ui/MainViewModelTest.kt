package com.chronopath.locationtracker.ui

import android.app.Application
import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.core.di.AppModule
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.domain.usecase.GetLocationCountUseCase
import com.chronopath.locationtracker.domain.usecase.StartTrackingUseCase
import com.chronopath.locationtracker.domain.usecase.StopTrackingUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var startTrackingUseCase: StartTrackingUseCase
    private lateinit var stopTrackingUseCase: StopTrackingUseCase
    private lateinit var getLocationCountUseCase: GetLocationCountUseCase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        startTrackingUseCase = mockk()
        stopTrackingUseCase = mockk()
        getLocationCountUseCase = mockk()

        mockkObject(AppModule)
        mockkObject(LocationTrackingService)

        every { AppModule.provideStartTrackingUseCase(application) } returns startTrackingUseCase
        every { AppModule.provideStopTrackingUseCase(application) } returns stopTrackingUseCase
        every { AppModule.provideGetLocationCountUseCase(application) } returns getLocationCountUseCase
        every { LocationTrackingService.isRunning(application) } returns false
        coEvery { getLocationCountUseCase(Unit) } returns flowOf(0)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(AppModule)
        unmockkObject(LocationTrackingService)
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(application)
    }

    @Test
    fun `initial state has isTracking false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.isTracking.value)
    }

    @Test
    fun `initial state has locationCount zero`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(0, viewModel.locationCount.value)
    }

    @Test
    fun `initial state has isLoading false`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `initial state has error null`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
    }

    @Test
    fun `startTracking sets isTracking true on success`() = runTest {
        coEvery { startTrackingUseCase(Unit) } returns Result.Success(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTracking()
        advanceUntilIdle()

        assertTrue(viewModel.isTracking.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `startTracking sets error on failure`() = runTest {
        val errorMessage = "Failed to start"
        coEvery { startTrackingUseCase(Unit) } returns Result.Error(Exception(), errorMessage)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTracking()
        advanceUntilIdle()

        assertFalse(viewModel.isTracking.value)
        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `startTracking sets isLoading false after completion`() = runTest {
        coEvery { startTrackingUseCase(Unit) } returns Result.Success(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTracking()
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `stopTracking sets isTracking false on success`() = runTest {
        coEvery { startTrackingUseCase(Unit) } returns Result.Success(true)
        coEvery { stopTrackingUseCase(Unit) } returns Result.Success(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTracking()
        advanceUntilIdle()
        assertTrue(viewModel.isTracking.value)

        viewModel.stopTracking()
        advanceUntilIdle()

        assertFalse(viewModel.isTracking.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `stopTracking sets error on failure`() = runTest {
        val errorMessage = "Failed to stop"
        coEvery { stopTrackingUseCase(Unit) } returns Result.Error(Exception(), errorMessage)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.stopTracking()
        advanceUntilIdle()

        assertEquals(errorMessage, viewModel.error.value)
    }

    @Test
    fun `observeLocationCount updates locationCount from use case`() = runTest {
        coEvery { getLocationCountUseCase(Unit) } returns flowOf(42)
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(42, viewModel.locationCount.value)
    }

    @Test
    fun `clearError sets error to null`() = runTest {
        val errorMessage = "Some error"
        coEvery { startTrackingUseCase(Unit) } returns Result.Error(Exception(), errorMessage)
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startTracking()
        advanceUntilIdle()
        assertEquals(errorMessage, viewModel.error.value)

        viewModel.clearError()

        assertNull(viewModel.error.value)
    }

    @Test
    fun `refreshTrackingStatus updates isTracking from service`() = runTest {
        every { LocationTrackingService.isRunning(application) } returns false
        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.isTracking.value)

        every { LocationTrackingService.isRunning(application) } returns true
        viewModel.refreshTrackingStatus()

        assertTrue(viewModel.isTracking.value)
    }

    @Test
    fun `startTracking clears previous error`() = runTest {
        coEvery { startTrackingUseCase(Unit) } returns Result.Success(true)
        viewModel = createViewModel()
        advanceUntilIdle()

        // Simulate previous error by setting directly via stopTracking failure
        coEvery { stopTrackingUseCase(Unit) } returns Result.Error(Exception(), "Previous error")
        viewModel.stopTracking()
        advanceUntilIdle()
        assertEquals("Previous error", viewModel.error.value)

        // Start tracking should clear the error
        viewModel.startTracking()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
    }
}
