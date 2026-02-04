package com.chronopath.locationtracker.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import timber.log.Timber

/**
 * Manages encryption keys securely using Android Keystore.
 * Provides keys for SQLCipher database encryption.
 */
class SecureKeyManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DATABASE_KEY_ALIAS = "location_db_key"
        private const val ENCRYPTED_PREFS_NAME = "secure_key_prefs"
        private const val KEY_DATABASE_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Gets or creates a secure passphrase for SQLCipher database encryption.
     * The passphrase is stored encrypted using EncryptedSharedPreferences.
     */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val existingPassphrase = encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null)

        return if (existingPassphrase != null) {
            Timber.tag("Security").d("Using existing database passphrase")
            existingPassphrase.toByteArray(Charsets.UTF_8)
        } else {
            Timber.tag("Security").i("Generating new database passphrase")
            val newPassphrase = generateSecurePassphrase()
            encryptedPrefs.edit()
                .putString(KEY_DATABASE_PASSPHRASE, String(newPassphrase, Charsets.UTF_8))
                .apply()
            newPassphrase
        }
    }

    /**
     * Generates a cryptographically secure random passphrase.
     */
    private fun generateSecurePassphrase(): ByteArray {
        val secureRandom = java.security.SecureRandom()
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        secureRandom.nextBytes(passphrase)
        // Convert to printable characters for SQLCipher compatibility
        return passphrase.map { byte ->
            // Map to printable ASCII range (33-126)
            ((byte.toInt() and 0xFF) % 94 + 33).toChar()
        }.joinToString("").toByteArray(Charsets.UTF_8)
    }

    /**
     * Checks if a database passphrase already exists.
     * Useful for migration checks.
     */
    fun hasExistingPassphrase(): Boolean {
        return encryptedPrefs.getString(KEY_DATABASE_PASSPHRASE, null) != null
    }

    /**
     * Clears the stored passphrase. USE WITH CAUTION - this will make
     * any existing encrypted database unreadable.
     */
    fun clearPassphrase() {
        Timber.tag("Security").w("Clearing database passphrase")
        encryptedPrefs.edit().remove(KEY_DATABASE_PASSPHRASE).apply()
    }
}
