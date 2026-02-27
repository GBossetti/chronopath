package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.model.MovementEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours

class AnalyzeMovementUseCaseTest {

    // 1 degree lat ≈ 111 000 m; use absolute lat difference
    // NEAR step: 0.0001° ≈ 11 m  (< 50 m threshold)
    // FAR step:  0.001°  ≈ 111 m (≥ 50 m threshold)
    private val testCalcDistance: (Double, Double, Double, Double) -> Float =
        { lat1, _, lat2, _ -> (abs(lat2 - lat1) * 111_000.0).toFloat() }

    private val useCase = AnalyzeMovementUseCase(calcDistance = testCalcDistance)

    private fun makeLocation(lat: Double, lon: Double = 0.0, epochMs: Long = 0L) = Location(
        latitude = lat,
        longitude = lon,
        timestamp = Instant.fromEpochMilliseconds(epochMs),
        installationId = "test"
    )

    private val tz = TimeZone.currentSystemDefault()

    private fun instantForLocalDate(date: LocalDate, plusHours: Int = 12): Instant {
        return date.atStartOfDayIn(tz) + plusHours.hours
    }

    // ─── analyze() ───────────────────────────────────────────────────────────

    @Test
    fun `analyze empty list returns empty list`() {
        val result = useCase.analyze(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `analyze single point returns one Stay with pointCount 1 and durationMs 0`() {
        val result = useCase.analyze(listOf(makeLocation(0.0)))

        assertEquals(1, result.size)
        val stay = result.first() as MovementEvent.Stay
        assertEquals(1, stay.period.pointCount)
        assertEquals(0L, stay.period.durationMs)
    }

    @Test
    fun `analyze two near points returns one Stay with pointCount 2`() {
        val locations = listOf(
            makeLocation(lat = 0.0, epochMs = 0L),
            makeLocation(lat = 0.0001, epochMs = 60_000L) // ≈11 m apart
        )
        val result = useCase.analyze(locations)

        assertEquals(1, result.size)
        val stay = result.first() as MovementEvent.Stay
        assertEquals(2, stay.period.pointCount)
    }

    @Test
    fun `analyze two far points returns Stay then Trip`() {
        val locations = listOf(
            makeLocation(lat = 0.0, epochMs = 0L),
            makeLocation(lat = 0.001, epochMs = 60_000L) // ≈111 m apart
        )
        val result = useCase.analyze(locations)

        assertEquals(2, result.size)
        val stay = result[0] as MovementEvent.Stay
        assertEquals(1, stay.period.pointCount)

        val trip = result[1] as MovementEvent.Trip
        assertEquals(2, trip.trip.pointCount)
        assertEquals(111f, trip.trip.totalDistanceMeters, 1f)
    }

    @Test
    fun `analyze three consecutive far points returns Stay then Trip spanning all`() {
        val locations = listOf(
            makeLocation(lat = 0.0, epochMs = 0L),
            makeLocation(lat = 0.001, epochMs = 60_000L),  // ≈111 m
            makeLocation(lat = 0.002, epochMs = 120_000L)  // ≈111 m
        )
        val result = useCase.analyze(locations)

        assertEquals(2, result.size)
        val stay = result[0] as MovementEvent.Stay
        assertEquals(1, stay.period.pointCount)

        val trip = result[1] as MovementEvent.Trip
        assertEquals(3, trip.trip.pointCount)
        assertEquals(222f, trip.trip.totalDistanceMeters, 2f)
    }

    @Test
    fun `analyze near-near-far-far-near produces Stay Trip Stay`() {
        // p1→p2: near, p2→p3: far, p3→p4: far, p4→p5: near
        val locations = listOf(
            makeLocation(lat = 0.0, epochMs = 0L),
            makeLocation(lat = 0.0001, epochMs = 60_000L),   // near
            makeLocation(lat = 0.0011, epochMs = 120_000L),  // far from p2
            makeLocation(lat = 0.0021, epochMs = 180_000L),  // far from p3
            makeLocation(lat = 0.0022, epochMs = 240_000L)   // near from p4
        )
        val result = useCase.analyze(locations)

        assertEquals(3, result.size)

        val stay1 = result[0] as MovementEvent.Stay
        assertEquals(2, stay1.period.pointCount)

        val trip = result[1] as MovementEvent.Trip
        assertEquals(3, trip.trip.pointCount)
        assertEquals(222f, trip.trip.totalDistanceMeters, 2f)

        val stay2 = result[2] as MovementEvent.Stay
        assertEquals(2, stay2.period.pointCount)
    }

    @Test
    fun `analyze Stay centroid is average of all point lat and lon`() {
        val locations = listOf(
            makeLocation(lat = 0.0, lon = 0.0, epochMs = 0L),
            makeLocation(lat = 0.0001, lon = 0.0002, epochMs = 60_000L) // near
        )
        val result = useCase.analyze(locations)

        val stay = result.first() as MovementEvent.Stay
        assertEquals(0.00005, stay.period.centroidLat, 1e-9)
        assertEquals(0.0001, stay.period.centroidLon, 1e-9)
    }

    // ─── summarizeByDay() ────────────────────────────────────────────────────

    private fun stayEvent(date: LocalDate, durationMs: Long, offset: Int = 0): MovementEvent.Stay {
        val start = instantForLocalDate(date, plusHours = offset)
        val end = Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + durationMs)
        return MovementEvent.Stay(
            com.chronopath.locationtracker.domain.model.StayPeriod(
                centroidLat = 0.0,
                centroidLon = 0.0,
                startTime = start,
                endTime = end,
                pointCount = 1
            )
        )
    }

    private fun tripEvent(
        date: LocalDate,
        distanceMeters: Float,
        durationMs: Long,
        offset: Int = 0
    ): MovementEvent.Trip {
        val start = instantForLocalDate(date, plusHours = offset)
        val end = Instant.fromEpochMilliseconds(start.toEpochMilliseconds() + durationMs)
        return MovementEvent.Trip(
            com.chronopath.locationtracker.domain.model.Trip(
                startTime = start,
                endTime = end,
                totalDistanceMeters = distanceMeters,
                pointCount = 2
            )
        )
    }

    @Test
    fun `summarizeByDay empty list returns empty list`() {
        val result = useCase.summarizeByDay(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `summarizeByDay single Stay produces correct DaySummary`() {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val event = stayEvent(today, durationMs = 30_000L)

        val result = useCase.summarizeByDay(listOf(event))

        assertEquals(1, result.size)
        val summary = result[0]
        assertEquals(1, summary.stayCount)
        assertEquals(0, summary.tripCount)
        assertEquals(0f, summary.totalDistanceMeters, 0.01f)
        assertEquals(30_000L, summary.totalStayDurationMs)
    }

    @Test
    fun `summarizeByDay single Trip produces correct DaySummary`() {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val event = tripEvent(today, distanceMeters = 500f, durationMs = 600_000L)

        val result = useCase.summarizeByDay(listOf(event))

        assertEquals(1, result.size)
        val summary = result[0]
        assertEquals(0, summary.stayCount)
        assertEquals(1, summary.tripCount)
        assertTrue(summary.totalDistanceMeters > 0f)
        assertEquals(600_000L, summary.totalTripDurationMs)
    }

    @Test
    fun `summarizeByDay events on two different dates returns two summaries sorted by date`() {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val yesterday = today - DatePeriod(days = 1)

        val events = listOf(
            stayEvent(today, durationMs = 10_000L),
            tripEvent(yesterday, distanceMeters = 1000f, durationMs = 300_000L)
        )

        val result = useCase.summarizeByDay(events)

        assertEquals(2, result.size)
        assertEquals(yesterday, result[0].date)
        assertEquals(today, result[1].date)
    }

    @Test
    fun `summarizeByDay mixed same-day events sums totals correctly`() {
        val today = Clock.System.now().toLocalDateTime(tz).date

        val events = listOf(
            stayEvent(today, durationMs = 10_000L, offset = 8),
            tripEvent(today, distanceMeters = 1000f, durationMs = 300_000L, offset = 9),
            tripEvent(today, distanceMeters = 500f, durationMs = 150_000L, offset = 10),
            stayEvent(today, durationMs = 20_000L, offset = 11)
        )

        val result = useCase.summarizeByDay(events)

        assertEquals(1, result.size)
        val summary = result[0]
        assertEquals(2, summary.stayCount)
        assertEquals(2, summary.tripCount)
        assertEquals(1500f, summary.totalDistanceMeters, 0.01f)
        assertEquals(30_000L, summary.totalStayDurationMs)
        assertEquals(450_000L, summary.totalTripDurationMs)
    }
}
