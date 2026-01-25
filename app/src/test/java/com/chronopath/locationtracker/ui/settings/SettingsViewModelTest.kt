package com.chronopath.locationtracker.ui.settings

import android.app.Application
import com.chronopath.locationtracker.data.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        mockkConstructor(SettingsRepository::class)
        every { anyConstructed<SettingsRepository>().trackingIntervalMs } returns flowOf(5 * 60 * 1000L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkConstructor(SettingsRepository::class)
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(application)
    }

    // TrackingIntervalOption tests

    @Test
    fun `TrackingIntervalOption has correct 1 minute interval`() {
        val option = TrackingIntervalOption(
            label = "1 minute",
            intervalMs = 1 * 60 * 1000L,
            description = "High detail"
        )

        assertEquals(60_000L, option.intervalMs)
        assertEquals("1 minute", option.label)
    }

    @Test
    fun `TrackingIntervalOption has correct 3 minute interval`() {
        val option = TrackingIntervalOption(
            label = "3 minutes",
            intervalMs = 3 * 60 * 1000L,
            description = "Detailed tracking"
        )

        assertEquals(180_000L, option.intervalMs)
    }

    @Test
    fun `TrackingIntervalOption has correct 5 minute interval`() {
        val option = TrackingIntervalOption(
            label = "5 minutes",
            intervalMs = 5 * 60 * 1000L,
            description = "Balanced"
        )

        assertEquals(300_000L, option.intervalMs)
    }

    @Test
    fun `TrackingIntervalOption has correct 20 minute interval`() {
        val option = TrackingIntervalOption(
            label = "20 minutes",
            intervalMs = 20 * 60 * 1000L,
            description = "Minimal battery"
        )

        assertEquals(1_200_000L, option.intervalMs)
    }

    @Test
    fun `interval options cover expected range`() {
        val expectedIntervals = listOf(
            1 * 60 * 1000L,   // 1 min
            3 * 60 * 1000L,   // 3 min
            5 * 60 * 1000L,   // 5 min
            10 * 60 * 1000L,  // 10 min
            20 * 60 * 1000L   // 20 min
        )

        val options = listOf(
            TrackingIntervalOption("1 minute", 1 * 60 * 1000L, ""),
            TrackingIntervalOption("3 minutes", 3 * 60 * 1000L, ""),
            TrackingIntervalOption("5 minutes", 5 * 60 * 1000L, ""),
            TrackingIntervalOption("10 minutes", 10 * 60 * 1000L, ""),
            TrackingIntervalOption("20 minutes", 20 * 60 * 1000L, "")
        )

        assertEquals(expectedIntervals, options.map { it.intervalMs })
    }

    @Test
    fun `interval conversion from minutes to milliseconds is correct`() {
        val minutes = 3
        val expectedMs = 3 * 60 * 1000L

        assertEquals(180_000L, expectedMs)
        assertEquals(minutes.toLong(), expectedMs / 60_000)
    }

    // SettingsViewModel tests

    @Test
    fun `initial selectedIntervalMs is default 5 minutes`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(5 * 60 * 1000L, viewModel.selectedIntervalMs.value)
    }

    @Test
    fun `loadCurrentSettings updates selectedIntervalMs from repository`() = runTest {
        val storedInterval = 10 * 60 * 1000L
        every { anyConstructed<SettingsRepository>().trackingIntervalMs } returns flowOf(storedInterval)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(storedInterval, viewModel.selectedIntervalMs.value)
    }

    @Test
    fun `selectInterval updates selectedIntervalMs`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val newInterval = 20 * 60 * 1000L
        viewModel.selectInterval(newInterval)

        assertEquals(newInterval, viewModel.selectedIntervalMs.value)
    }

    @Test
    fun `saveSettings calls repository with selected interval`() = runTest {
        coEvery { anyConstructed<SettingsRepository>().setTrackingIntervalMs(any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        val newInterval = 3 * 60 * 1000L
        viewModel.selectInterval(newInterval)
        viewModel.saveSettings()
        advanceUntilIdle()

        coVerify { anyConstructed<SettingsRepository>().setTrackingIntervalMs(newInterval) }
    }

    @Test
    fun `saveSettings sets saveSuccess true on success`() = runTest {
        coEvery { anyConstructed<SettingsRepository>().setTrackingIntervalMs(any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveSettings()
        advanceUntilIdle()

        assertTrue(viewModel.saveSuccess.value)
    }

    @Test
    fun `saveSettings sets isSaving false after completion`() = runTest {
        coEvery { anyConstructed<SettingsRepository>().setTrackingIntervalMs(any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveSettings()
        advanceUntilIdle()

        assertFalse(viewModel.isSaving.value)
    }

    @Test
    fun `clearSaveSuccess sets saveSuccess to false`() = runTest {
        coEvery { anyConstructed<SettingsRepository>().setTrackingIntervalMs(any()) } returns Unit
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveSettings()
        advanceUntilIdle()
        assertTrue(viewModel.saveSuccess.value)

        viewModel.clearSaveSuccess()

        assertFalse(viewModel.saveSuccess.value)
    }

    @Test
    fun `intervalOptions contains 5 options`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(5, viewModel.intervalOptions.size)
    }

    @Test
    fun `intervalOptions has 1 minute as first option`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(60_000L, viewModel.intervalOptions.first().intervalMs)
    }

    @Test
    fun `intervalOptions has 20 minutes as last option`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(1_200_000L, viewModel.intervalOptions.last().intervalMs)
    }
}
