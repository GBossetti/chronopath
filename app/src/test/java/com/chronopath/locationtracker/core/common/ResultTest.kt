package com.chronopath.locationtracker.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `Success contains data`() {
        val result = Result.Success("test data")

        assertEquals("test data", result.data)
    }

    @Test
    fun `Success isSuccess returns true`() {
        val result: Result<String> = Result.Success("test")

        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Error with exception`() {
        val exception = RuntimeException("Test error")
        val result = Result.Error(exception = exception)

        assertEquals(exception, result.exception)
        assertNull(result.message)
    }

    @Test
    fun `Error with message`() {
        val result = Result.Error(message = "Something went wrong")

        assertNull(result.exception)
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `Error with both exception and message`() {
        val exception = RuntimeException("Test error")
        val result = Result.Error(exception = exception, message = "Something went wrong")

        assertEquals(exception, result.exception)
        assertEquals("Something went wrong", result.message)
    }

    @Test
    fun `Error isError returns true`() {
        val result: Result<String> = Result.Error(message = "error")

        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Loading isLoading returns true`() {
        val result: Result<String> = Result.Loading

        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `Loading is singleton`() {
        val loading1: Result<String> = Result.Loading
        val loading2: Result<Int> = Result.Loading

        assertTrue(loading1 === loading2)
    }

    @Test
    fun `Success with different types`() {
        val stringResult = Result.Success("text")
        val intResult = Result.Success(42)
        val listResult = Result.Success(listOf(1, 2, 3))

        assertEquals("text", stringResult.data)
        assertEquals(42, intResult.data)
        assertEquals(listOf(1, 2, 3), listResult.data)
    }
}
