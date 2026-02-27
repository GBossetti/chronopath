package com.chronopath.locationtracker.ui.analytics

import android.app.Application
import com.chronopath.locationtracker.core.di.AppModule
import com.chronopath.locationtracker.domain.model.DaySummary
import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.model.MovementEvent
import com.chronopath.locationtracker.domain.model.StayPeriod
import com.chronopath.locationtracker.domain.repository.LocationRepository
import com.chronopath.locationtracker.domain.usecase.AnalyzeMovementUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var locationRepository: LocationRepository

    private val tz = TimeZone.currentSystemDefault()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        application = mockk(relaxed = true)
        locationRepository = mockk()

        mockkObject(AppModule)
        mockkConstructor(AnalyzeMovementUseCase::class)

        every { AppModule.provideLocationRepository(application) } returns locationRepository

        // Default: empty location list
        every { locationRepository.getAllLocations() } returns flowOf(emptyList())

        // Default: analyze and summarize return empty
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createViewModel() = AnalyticsViewModel(application)

    private fun makeLocation(epochMs: Long = 0L) = Location(
        latitude = 0.0,
        longitude = 0.0,
        timestamp = Instant.fromEpochMilliseconds(epochMs),
        installationId = "test"
    )

    private fun todayStayEvent(): MovementEvent.Stay {
        val now = Clock.System.now()
        return MovementEvent.Stay(
            StayPeriod(
                centroidLat = 0.0,
                centroidLon = 0.0,
                startTime = now,
                endTime = now,
                pointCount = 1
            )
        )
    }

    private fun todaySummary(distanceMeters: Float, tripCount: Int = 1): DaySummary {
        val today = Clock.System.now().toLocalDateTime(tz).date
        return DaySummary(
            date = today,
            totalDistanceMeters = distanceMeters,
            totalStayDurationMs = 0L,
            totalTripDurationMs = 60_000L,
            tripCount = tripCount,
            stayCount = 0
        )
    }

    @Test
    fun `initial isLoading is true before first emission`() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun `empty location flow sets events empty and insightText to no data`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.events.value.isEmpty())
        assertEquals("No data recorded yet", viewModel.insightText.value)
    }

    @Test
    fun `non-empty locations causes analyze to be called and events reflects returned list`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        val stayEvent = todayStayEvent()
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns listOf(stayEvent)
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(stayEvent), viewModel.events.value)
    }

    @Test
    fun `todaySummary is set to DaySummary matching today`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        val summary = todaySummary(distanceMeters = 1000f)
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns listOf(summary)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.todaySummary.value)
        assertEquals(summary, viewModel.todaySummary.value)
    }

    @Test
    fun `insightText contains Barely moved when distance is less than 500m`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns
            listOf(todaySummary(distanceMeters = 200f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(
            "Expected 'Barely moved' in: ${viewModel.insightText.value}",
            viewModel.insightText.value.contains("Barely moved")
        )
    }

    @Test
    fun `insightText contains Active day when distance is greater than 5000m`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns
            listOf(todaySummary(distanceMeters = 6000f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(
            "Expected 'Active day' in: ${viewModel.insightText.value}",
            viewModel.insightText.value.contains("Active day")
        )
    }

    @Test
    fun `insightText contains Moderate activity when distance is between 500m and 5000m`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns
            listOf(todaySummary(distanceMeters = 2000f))

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(
            "Expected 'Moderate activity' in: ${viewModel.insightText.value}",
            viewModel.insightText.value.contains("Moderate activity")
        )
    }

    @Test
    fun `todaySummary is null when no matching date in day summaries`() = runTest {
        every { locationRepository.getAllLocations() } returns flowOf(listOf(makeLocation()))
        every { anyConstructed<AnalyzeMovementUseCase>().analyze(any()) } returns emptyList()
        every { anyConstructed<AnalyzeMovementUseCase>().summarizeByDay(any()) } returns emptyList()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.todaySummary.value)
    }
}
