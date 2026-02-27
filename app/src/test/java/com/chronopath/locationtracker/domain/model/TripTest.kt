package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTest {

    private val baseInstant = Instant.fromEpochMilliseconds(1_000_000L)

    @Test
    fun `durationMs is difference between end and start timestamps`() {
        val start = Instant.fromEpochMilliseconds(1_000_000L)
        val end = Instant.fromEpochMilliseconds(1_060_000L)
        val trip = Trip(
            startTime = start,
            endTime = end,
            totalDistanceMeters = 100f,
            pointCount = 2
        )

        assertEquals(60_000L, trip.durationMs)
    }

    @Test
    fun `durationMs is zero when start equals end`() {
        val trip = Trip(
            startTime = baseInstant,
            endTime = baseInstant,
            totalDistanceMeters = 0f,
            pointCount = 1
        )

        assertEquals(0L, trip.durationMs)
    }

    @Test
    fun `avgSpeedMs is totalDistanceMeters divided by duration in seconds`() {
        val start = Instant.fromEpochMilliseconds(0L)
        val end = Instant.fromEpochMilliseconds(10_000L) // 10 seconds
        val trip = Trip(
            startTime = start,
            endTime = end,
            totalDistanceMeters = 50f,
            pointCount = 2
        )

        // 50m / 10s = 5 m/s
        assertEquals(5f, trip.avgSpeedMs, 0.001f)
    }

    @Test
    fun `avgSpeedMs is zero when duration is zero`() {
        val trip = Trip(
            startTime = baseInstant,
            endTime = baseInstant,
            totalDistanceMeters = 100f,
            pointCount = 2
        )

        assertEquals(0f, trip.avgSpeedMs, 0.001f)
    }
}
