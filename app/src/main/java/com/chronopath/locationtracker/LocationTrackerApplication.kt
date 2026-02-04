package com.chronopath.locationtracker

import android.app.Application
import com.chronopath.locationtracker.core.security.DatabaseMigrationHelper
import com.chronopath.locationtracker.core.security.SecureKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class LocationTrackerApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("LocationTrackerApplication created")

        // Run database migration in background
        migrateDatabase()
    }

    /**
     * Migrates existing unencrypted database to encrypted storage.
     * Runs asynchronously to avoid blocking app startup.
     */
    private fun migrateDatabase() {
        applicationScope.launch {
            try {
                val migrationHelper = DatabaseMigrationHelper(this@LocationTrackerApplication)

                if (migrationHelper.hasOldDatabase() && !migrationHelper.hasNewDatabase()) {
                    Timber.tag("App").i("Database migration required")
                    val secureKeyManager = SecureKeyManager(this@LocationTrackerApplication)
                    val migrated = migrationHelper.migrateToEncryptedDatabase(secureKeyManager)

                    when {
                        migrated > 0 -> Timber.tag("App").i("Successfully migrated $migrated location records")
                        migrated == 0 -> Timber.tag("App").d("No data to migrate")
                        else -> Timber.tag("App").e("Database migration failed")
                    }
                }
            } catch (e: Exception) {
                Timber.tag("App").e(e, "Error during database migration check")
            }
        }
    }
}
