package com.chronopath.locationtracker

import android.app.Application
import timber.log.Timber

class LocationTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.i("LocationTrackerApplication created")
    }
}
