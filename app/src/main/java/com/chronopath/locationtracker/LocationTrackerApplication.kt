package com.chronopath.locationtracker

import android.app.Application
import com.chronopath.locationtracker.core.common.AppLogger
import timber.log.Timber

class LocationTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this, BuildConfig.DEBUG)   // standalone file logger, always active
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())      // logcat for other files that still use Timber
        }
        Timber.i("LocationTrackerApplication created")
    }
}
