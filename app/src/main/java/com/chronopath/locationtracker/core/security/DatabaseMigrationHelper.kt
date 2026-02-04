package com.chronopath.locationtracker.core.security

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.local.entity.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.io.File

/**
 * Handles migration from unencrypted database to encrypted database.
 * Preserves all existing location data during the migration.
 */
class DatabaseMigrationHelper(private val context: Context) {

    companion object {
        private const val OLD_DB_NAME = "location_tracker.db"
        private const val NEW_DB_NAME = "location_tracker_encrypted.db"
    }

    /**
     * Checks if the old unencrypted database exists.
     */
    fun hasOldDatabase(): Boolean {
        val dbFile = context.getDatabasePath(OLD_DB_NAME)
        return dbFile.exists()
    }

    /**
     * Checks if the new encrypted database exists.
     */
    fun hasNewDatabase(): Boolean {
        val dbFile = context.getDatabasePath(NEW_DB_NAME)
        return dbFile.exists()
    }

    /**
     * Migrates data from old unencrypted database to new encrypted database.
     * Returns the number of records migrated, or -1 if migration failed.
     */
    suspend fun migrateToEncryptedDatabase(secureKeyManager: SecureKeyManager): Int = withContext(Dispatchers.IO) {
        if (!hasOldDatabase()) {
            Timber.tag("Migration").d("No old database to migrate")
            return@withContext 0
        }

        if (hasNewDatabase()) {
            Timber.tag("Migration").d("Encrypted database already exists, skipping migration")
            return@withContext 0
        }

        Timber.tag("Migration").i("Starting database migration to encrypted storage")

        try {
            // Read from old unencrypted database
            val oldDbPath = context.getDatabasePath(OLD_DB_NAME).absolutePath
            val locations = readFromOldDatabase(oldDbPath)
            Timber.tag("Migration").d("Read ${locations.size} locations from old database")

            if (locations.isEmpty()) {
                Timber.tag("Migration").d("No data to migrate, cleaning up old database")
                deleteOldDatabase()
                return@withContext 0
            }

            // Write to new encrypted database
            val passphrase = secureKeyManager.getOrCreateDatabasePassphrase()
            val factory = SupportFactory(passphrase)

            val newDb = Room.databaseBuilder(
                context.applicationContext,
                LocationDatabase::class.java,
                NEW_DB_NAME
            )
                .openHelperFactory(factory)
                .build()

            // Insert all locations
            locations.forEach { entity ->
                newDb.locationDao().insertLocation(entity)
            }

            // Verify migration
            val newCount = newDb.locationDao().getLocationCountSync()
            Timber.tag("Migration").d("Verified $newCount locations in encrypted database")

            newDb.close()

            if (newCount == locations.size) {
                Timber.tag("Migration").i("Migration successful, deleting old database")
                deleteOldDatabase()
                return@withContext newCount
            } else {
                Timber.tag("Migration").e("Migration verification failed: expected ${locations.size}, got $newCount")
                return@withContext -1
            }

        } catch (e: Exception) {
            Timber.tag("Migration").e(e, "Database migration failed")
            return@withContext -1
        }
    }

    /**
     * Reads all location entities from the old unencrypted database.
     */
    private fun readFromOldDatabase(dbPath: String): List<LocationEntity> {
        val locations = mutableListOf<LocationEntity>()

        try {
            val db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)

            val cursor = db.rawQuery("SELECT * FROM locations", null)

            while (cursor.moveToNext()) {
                try {
                    val idIndex = cursor.getColumnIndex("id")
                    val latIndex = cursor.getColumnIndex("latitude")
                    val lonIndex = cursor.getColumnIndex("longitude")
                    val timestampIndex = cursor.getColumnIndex("timestamp")
                    val accIndex = cursor.getColumnIndex("accuracy")
                    val altIndex = cursor.getColumnIndex("altitude")
                    val speedIndex = cursor.getColumnIndex("speed")
                    val bearingIndex = cursor.getColumnIndex("bearing")
                    val providerIndex = cursor.getColumnIndex("provider")
                    val batteryIndex = cursor.getColumnIndex("battery_percentage")
                    val chargingIndex = cursor.getColumnIndex("is_charging")
                    val networkIndex = cursor.getColumnIndex("network_type")
                    val installationIdIndex = cursor.getColumnIndex("installation_id")
                    val advertisingIdIndex = cursor.getColumnIndex("advertising_id")

                    if (latIndex >= 0 && lonIndex >= 0 && timestampIndex >= 0 && installationIdIndex >= 0) {
                        locations.add(
                            LocationEntity(
                                id = 0, // Let Room generate new IDs
                                latitude = cursor.getDouble(latIndex),
                                longitude = cursor.getDouble(lonIndex),
                                timestamp = Instant.parse(cursor.getString(timestampIndex)),
                                accuracy = if (accIndex >= 0 && !cursor.isNull(accIndex)) cursor.getFloat(accIndex) else null,
                                altitude = if (altIndex >= 0 && !cursor.isNull(altIndex)) cursor.getDouble(altIndex) else null,
                                speed = if (speedIndex >= 0 && !cursor.isNull(speedIndex)) cursor.getFloat(speedIndex) else null,
                                bearing = if (bearingIndex >= 0 && !cursor.isNull(bearingIndex)) cursor.getFloat(bearingIndex) else null,
                                provider = if (providerIndex >= 0 && !cursor.isNull(providerIndex)) cursor.getString(providerIndex) else null,
                                batteryPercentage = if (batteryIndex >= 0 && !cursor.isNull(batteryIndex)) cursor.getInt(batteryIndex) else null,
                                isCharging = if (chargingIndex >= 0 && !cursor.isNull(chargingIndex)) cursor.getInt(chargingIndex) == 1 else null,
                                networkType = if (networkIndex >= 0 && !cursor.isNull(networkIndex)) cursor.getString(networkIndex) else null,
                                installationId = cursor.getString(installationIdIndex),
                                advertisingId = if (advertisingIdIndex >= 0 && !cursor.isNull(advertisingIdIndex)) cursor.getString(advertisingIdIndex) else null
                            )
                        )
                    }
                } catch (e: Exception) {
                    Timber.tag("Migration").w(e, "Error reading row, skipping")
                }
            }

            cursor.close()
            db.close()

        } catch (e: Exception) {
            Timber.tag("Migration").e(e, "Error reading old database")
        }

        return locations
    }

    /**
     * Deletes the old unencrypted database files.
     */
    private fun deleteOldDatabase() {
        try {
            val dbFile = context.getDatabasePath(OLD_DB_NAME)
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            val journalFile = File(dbFile.path + "-journal")

            dbFile.delete()
            walFile.delete()
            shmFile.delete()
            journalFile.delete()

            Timber.tag("Migration").d("Old database files deleted")
        } catch (e: Exception) {
            Timber.tag("Migration").e(e, "Error deleting old database")
        }
    }
}
