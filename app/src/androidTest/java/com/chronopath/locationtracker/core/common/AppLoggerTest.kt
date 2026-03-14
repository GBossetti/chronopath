package com.chronopath.locationtracker.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class AppLoggerTest {

    private lateinit var context: Context
    private lateinit var logDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        logDir = File(context.filesDir, Constants.LOG_DIR_NAME)
        resetAppLogger()
        logDir.deleteRecursively()
    }

    @After
    fun tearDown() {
        resetAppLogger()
        logDir.deleteRecursively()
    }

    private fun resetAppLogger() {
        AppLogger::class.java.getDeclaredField("initialized")
            .also { it.isAccessible = true }
            .set(AppLogger, false)
    }

    private fun todayLogFile(): File {
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        return File(logDir, "${Constants.LOG_FILE_PREFIX}$date${Constants.LOG_FILE_EXTENSION}")
    }

    @Test
    fun `init creates log directory`() {
        AppLogger.init(context, false)
        assertTrue(logDir.exists() && logDir.isDirectory)
    }

    @Test
    fun `init writes session header`() {
        AppLogger.init(context, false)
        Thread.sleep(300)
        val log = todayLogFile()
        assertTrue(log.exists())
        assertTrue(log.readText().contains("SESSION START"))
    }

    @Test
    fun `i() writes INFO line to file`() {
        AppLogger.init(context, false)
        AppLogger.i("Tag", "msg")
        Thread.sleep(300)
        val content = todayLogFile().readText()
        assertTrue(content.contains("I/Tag: msg"))
    }

    @Test
    fun `e() with throwable writes exception class and message`() {
        AppLogger.init(context, false)
        val ex = RuntimeException("boom")
        AppLogger.e("ErrTag", "error occurred", ex)
        Thread.sleep(300)
        val content = todayLogFile().readText()
        assertTrue(content.contains("RuntimeException"))
        assertTrue(content.contains("boom"))
    }

    @Test
    fun `init is idempotent`() {
        AppLogger.init(context, false)
        Thread.sleep(300)
        val countAfterFirst = todayLogFile().readText().split("SESSION START").size - 1
        AppLogger.init(context, false)
        Thread.sleep(300)
        val countAfterSecond = todayLogFile().readText().split("SESSION START").size - 1
        assertEquals(countAfterFirst, countAfterSecond)
    }

    @Test
    fun `rotation deletes oldest files when count reaches max`() {
        logDir.mkdirs()
        // Pre-create LOG_MAX_FILES dummy log files with ascending names
        repeat(Constants.LOG_MAX_FILES) { i ->
            File(logDir, "${Constants.LOG_FILE_PREFIX}2020-01-${String.format("%02d", i + 1)}${Constants.LOG_FILE_EXTENSION}")
                .writeText("dummy")
        }
        AppLogger.init(context, false)
        Thread.sleep(300)
        val remaining = logDir.listFiles { f -> f.name.endsWith(Constants.LOG_FILE_EXTENSION) }!!
        assertEquals(Constants.LOG_MAX_FILES, remaining.size)
    }

    @Test
    fun `log before init does nothing`() {
        AppLogger.i("Tag", "should not appear")
        Thread.sleep(300)
        assertFalse(todayLogFile().exists())
    }
}
