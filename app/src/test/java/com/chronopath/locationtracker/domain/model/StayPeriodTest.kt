package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class StayPeriodTest {

    private val baseInstant = Instant.fromEpochMilliseconds(1_000_000L)

    @Test
    fun `durationMs is difference between end and start timestamps`() {
        val start = Instant.fromEpochMilliseconds(1_000_000L)
        val end = Instant.fromEpochMilliseconds(1_060_000L)
        val stay = StayPeriod(
            centroidLat = 0.0,
            centroidLon = 0.0,
            startTime = start,
            endTime = end,
            pointCount = 1
        )

        assertEquals(60_000L, stay.durationMs)
    }

    @Test
    fun `durationMs is zero when start equals end`() {
        val stay = StayPeriod(
            centroidLat = 0.0,
            centroidLon = 0.0,
            startTime = baseInstant,
            endTime = baseInstant,
            pointCount = 1
        )

        assertEquals(0L, stay.durationMs)
    }
}
