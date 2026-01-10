package com.chronopath.locationtracker.data.local.converter

import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InstantConverterTest {

    private lateinit var converter: InstantConverter

    @Before
    fun setup() {
        converter = InstantConverter()
    }

    @Test
    fun `fromInstant converts instant to epoch milliseconds`() {
        val instant = Instant.parse("2024-01-15T10:30:00Z")

        val result = converter.fromInstant(instant)

        assertEquals(instant.toEpochMilliseconds(), result)
    }

    @Test
    fun `fromInstant returns null for null input`() {
        val result = converter.fromInstant(null)

        assertNull(result)
    }

    @Test
    fun `toInstant converts epoch milliseconds to instant`() {
        val millis = 1705315800000L // 2024-01-15T10:30:00Z

        val result = converter.toInstant(millis)

        assertEquals(Instant.fromEpochMilliseconds(millis), result)
    }

    @Test
    fun `toInstant returns null for null input`() {
        val result = converter.toInstant(null)

        assertNull(result)
    }

    @Test
    fun `round trip preserves instant value`() {
        val original = Instant.parse("2024-06-20T15:45:30.123Z")

        val millis = converter.fromInstant(original)
        val result = converter.toInstant(millis)

        assertEquals(original, result)
    }

    @Test
    fun `converts epoch zero correctly`() {
        val epochZero = Instant.fromEpochMilliseconds(0)

        val millis = converter.fromInstant(epochZero)
        val result = converter.toInstant(millis)

        assertEquals(0L, millis)
        assertEquals(epochZero, result)
    }

    @Test
    fun `converts negative epoch correctly`() {
        val beforeEpoch = Instant.fromEpochMilliseconds(-86400000L) // 1 day before epoch

        val millis = converter.fromInstant(beforeEpoch)
        val result = converter.toInstant(millis)

        assertEquals(-86400000L, millis)
        assertEquals(beforeEpoch, result)
    }
}
