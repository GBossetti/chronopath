package com.chronopath.locationtracker.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

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
}
