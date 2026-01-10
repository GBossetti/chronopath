package com.chronopath.locationtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
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
    
    companion object {
        @Volatile
        private var INSTANCE: LocationDatabase? = null
        
        fun getInstance(context: Context): LocationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDatabase::class.java,
                    "location_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
