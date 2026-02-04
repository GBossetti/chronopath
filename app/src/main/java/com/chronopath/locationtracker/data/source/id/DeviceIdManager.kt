package com.chronopath.locationtracker.data.source.id

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.util.UUID

/**
 * Manages device identification securely.
 * Uses EncryptedSharedPreferences for storing sensitive device IDs.
 */
class DeviceIdManager(private val context: Context) {
    companion object {
        private const val ENCRYPTED_PREFS_NAME = "encrypted_device_id_preferences"
        private const val KEY_INSTALLATION_ID = "installation_id"

        // Legacy prefs name for migration
        private const val LEGACY_PREFS_NAME = "device_id_preferences"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        // Migrate from legacy unencrypted prefs if needed
        migrateFromLegacyPrefs()
    }

    /**
     * Migrates installation ID from legacy unencrypted SharedPreferences
     * to encrypted storage, then deletes the legacy data.
     */
    private fun migrateFromLegacyPrefs() {
        val legacyPrefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val legacyId = legacyPrefs.getString(KEY_INSTALLATION_ID, null)

        if (legacyId != null && encryptedPrefs.getString(KEY_INSTALLATION_ID, null) == null) {
            Timber.tag("Security").i("Migrating installation ID to encrypted storage")
            encryptedPrefs.edit()
                .putString(KEY_INSTALLATION_ID, legacyId)
                .apply()

            // Clear legacy unencrypted data
            legacyPrefs.edit().clear().apply()
            Timber.tag("Security").d("Legacy preferences cleared")
        }
    }

    /**
     * Generates or retrieves a persistent installation UUID.
     * This ID persists across app restarts but not across reinstalls.
     * Stored in encrypted SharedPreferences for security.
     */
    fun getInstallationId(): String {
        return encryptedPrefs.getString(KEY_INSTALLATION_ID, null) ?: run {
            val newId = UUID.randomUUID().toString()
            encryptedPrefs.edit()
                .putString(KEY_INSTALLATION_ID, newId)
                .apply()
            Timber.tag("Security").d("Generated new installation ID")
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
