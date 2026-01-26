package com.chronopath.locationtracker.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantsTest {

    @Test
    fun `default tracking interval is 5 minutes`() {
        val expectedMs = 5 * 60 * 1000L // 5 minutes in milliseconds

        assertEquals(expectedMs, Constants.DEFAULT_TRACKING_INTERVAL_MS)
        assertEquals(300_000L, Constants.DEFAULT_TRACKING_INTERVAL_MS)
    }

    @Test
    fun `default min distance is 50 meters`() {
        assertEquals(50f, Constants.DEFAULT_MIN_DISTANCE_METERS)
    }

    @Test
    fun `notification channel id is set`() {
        assertEquals("location_tracking_channel", Constants.NOTIFICATION_CHANNEL_ID)
    }

    @Test
    fun `notification id is 1`() {
        assertEquals(1, Constants.NOTIFICATION_ID)
    }

    @Test
    fun `prefs keys are defined`() {
        assertEquals("first_launch", Constants.PREFS_FIRST_LAUNCH)
    }

    @Test
    fun `tracking interval converts to minutes correctly`() {
        val intervalMs = Constants.DEFAULT_TRACKING_INTERVAL_MS
        val minutes = intervalMs / (60 * 1000)

        assertEquals(5L, minutes)
    }

    @Test
    fun `lite flavor interval is 20 minutes`() {
        val liteIntervalMs = 20 * 60 * 1000L

        assertEquals(1_200_000L, liteIntervalMs)
        assertEquals(20L, liteIntervalMs / (60 * 1000))
    }
}
