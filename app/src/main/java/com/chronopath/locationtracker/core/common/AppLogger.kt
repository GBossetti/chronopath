package com.chronopath.locationtracker.core.common

import android.content.Context
import android.os.Build
import android.util.Log
import com.chronopath.locationtracker.core.common.Constants.LOG_DIR_NAME
import com.chronopath.locationtracker.core.common.Constants.LOG_FILE_EXTENSION
import com.chronopath.locationtracker.core.common.Constants.LOG_FILE_PREFIX
import com.chronopath.locationtracker.core.common.Constants.LOG_MAX_FILES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLogger {

    private var initialized = false
    private var isDebug = false
    private lateinit var logDir: File
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tsFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun init(context: Context, debug: Boolean) {
        if (initialized) return
        isDebug = debug
        logDir = File(context.filesDir, LOG_DIR_NAME).also { it.mkdirs() }
        rotate()
        writeSessionHeader(context)
        installCrashHandler()
        initialized = true
    }

    fun d(tag: String, message: String) = log(Log.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(Log.INFO, tag, message)
    fun w(tag: String, message: String) = log(Log.WARN, tag, message)
    fun e(tag: String, message: String, t: Throwable? = null) = log(Log.ERROR, tag, message, t)

    private fun log(priority: Int, tag: String, message: String, t: Throwable? = null) {
        if (!initialized) return
        if (isDebug) {
            when (priority) {
                Log.DEBUG -> Log.d(tag, message, t)
                Log.INFO  -> Log.i(tag, message, t)
                Log.WARN  -> Log.w(tag, message, t)
                Log.ERROR -> Log.e(tag, message, t)
            }
        }
        scope.launch { mutex.withLock { writeToFile(priority, tag, message, t) } }
    }

    private fun writeToFile(priority: Int, tag: String, message: String, t: Throwable?) {
        try {
            FileWriter(currentLogFile(), true).use { fw ->
                val pw = PrintWriter(fw)
                val ts = LocalDateTime.now().format(tsFmt)
                val level = when (priority) {
                    Log.DEBUG -> 'D'; Log.INFO -> 'I'; Log.WARN -> 'W'
                    Log.ERROR -> 'E'; Log.ASSERT -> 'A'; else -> '?'
                }
                pw.println("$ts $level/$tag: $message")
                t?.let {
                    pw.println("  ${it.javaClass.name}: ${it.message}")
                    it.stackTrace.take(8).forEach { f -> pw.println("    at $f") }
                }
                pw.flush()
            }
        } catch (_: Exception) {}
    }

    private fun writeSessionHeader(context: Context) {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) { "unknown" }
        val device = "${Build.MANUFACTURER} ${Build.MODEL} (API ${Build.VERSION.SDK_INT})"
        try {
            FileWriter(currentLogFile(), true).use { fw ->
                val pw = PrintWriter(fw)
                pw.println("=== SESSION START ${LocalDateTime.now().format(tsFmt)} ===")
                pw.println("=== App v$version | $device ===")
                pw.flush()
            }
        } catch (_: Exception) {}
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                FileWriter(currentLogFile(), true).use { fw ->
                    val pw = PrintWriter(fw)
                    pw.println("${LocalDateTime.now().format(tsFmt)} E/CRASH: Uncaught exception on thread '${thread.name}'")
                    pw.println("  ${throwable.javaClass.name}: ${throwable.message}")
                    throwable.stackTrace.forEach { pw.println("    at $it") }
                    pw.flush()
                }
            } catch (_: Exception) {}
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun rotate() {
        val files = logDir.listFiles { f -> f.name.endsWith(LOG_FILE_EXTENSION) }
            ?.sortedBy { it.name } ?: return
        if (files.size >= LOG_MAX_FILES)
            files.take(files.size - (LOG_MAX_FILES - 1)).forEach { it.delete() }
    }

    private fun currentLogFile() =
        File(logDir, "$LOG_FILE_PREFIX${LocalDate.now().format(dateFmt)}$LOG_FILE_EXTENSION")
}
