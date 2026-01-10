package com.chronopath.locationtracker.data.source.id

import android.content.Context
import java.util.UUID

/**
 * Manages device identification.
 * This is a framework-specific implementation (uses Android Context).
 */
class DeviceIdManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "device_id_preferences"
        private const val KEY_INSTALLATION_ID = "installation_id"
    }

    /**
     * Generates or retrieves a persistent installation UUID.
     * This ID persists across app restarts but not across reinstalls.
     */
    fun getInstallationId(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return prefs.getString(KEY_INSTALLATION_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit()
                .putString(KEY_INSTALLATION_ID, newId)
                .apply()
            newId
        }
    }

    /**
     * Retrieves the Advertising ID (AAID).
     * Returns null if Google Play Services unavailable or restricted.
     * Note: Requires Google Play Services dependency.
     */
    suspend fun getAdvertisingId(): String? {
        // TODO: Implement with Google Play Services AdvertisingIdClient
        // For MVP, return null
        return null
    }
}