package com.chronopath.locationtracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chronopath.locationtracker.data.local.converter.InstantConverter
import com.chronopath.locationtracker.data.local.dao.LocationDao
import com.chronopath.locationtracker.data.local.entity.LocationEntity

/**
 * Room database setup.
 */
@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(InstantConverter::class)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
