package com.chronopath.locationtracker.core.di

import android.content.Context
import androidx.room.Room
import com.chronopath.locationtracker.core.controller.TrackingControllerImpl
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.repository.LocationRepositoryImpl
import com.chronopath.locationtracker.data.source.aggregator.DataAggregator
import com.chronopath.locationtracker.data.source.battery.impl.AndroidBatteryDataSource
import com.chronopath.locationtracker.data.source.id.DeviceIdManager
import com.chronopath.locationtracker.data.source.location.FusedLocationDataSource
import com.chronopath.locationtracker.data.source.network.impl.AndroidNetworkDataSource
import com.chronopath.locationtracker.domain.controller.TrackingController
import com.chronopath.locationtracker.domain.repository.LocationRepository
import com.chronopath.locationtracker.domain.usecase.GetLocationCountUseCase
import com.chronopath.locationtracker.domain.usecase.GetLocationsUseCase
import com.chronopath.locationtracker.domain.usecase.StartTrackingUseCase
import com.chronopath.locationtracker.domain.usecase.StopTrackingUseCase

object AppModule {
    @Volatile
    private var database: LocationDatabase? = null

    @Volatile
    private var trackingController: TrackingController? = null

    private fun provideDatabase(context: Context): LocationDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                LocationDatabase::class.java,
                "location_tracker.db"
            ).build().also { database = it }
        }
    }

    fun provideLocationRepository(context: Context): LocationRepository {
        val appContext = context.applicationContext
        val db = provideDatabase(appContext)
        val deviceIdManager = DeviceIdManager(appContext)

        val aggregator = DataAggregator(
            locationDataSource = FusedLocationDataSource(appContext),
            batteryDataSource = AndroidBatteryDataSource(appContext),
            networkDataSource = AndroidNetworkDataSource(appContext),
            deviceIdManager = deviceIdManager
        )

        return LocationRepositoryImpl(
            database = db,
            aggregator = aggregator,
            deviceIdManager = deviceIdManager
        )
    }

    fun provideTrackingController(context: Context): TrackingController {
        return trackingController ?: synchronized(this) {
            trackingController ?: TrackingControllerImpl(
                context.applicationContext
            ).also { trackingController = it }
        }
    }

    fun provideStartTrackingUseCase(context: Context): StartTrackingUseCase {
        return StartTrackingUseCase(provideTrackingController(context))
    }

    fun provideStopTrackingUseCase(context: Context): StopTrackingUseCase {
        return StopTrackingUseCase(provideTrackingController(context))
    }

    fun provideGetLocationsUseCase(context: Context): GetLocationsUseCase {
        return GetLocationsUseCase(provideLocationRepository(context))
    }

    fun provideGetLocationCountUseCase(context: Context): GetLocationCountUseCase {
        return GetLocationCountUseCase(provideLocationRepository(context))
    }
}
