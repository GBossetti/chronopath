package com.chronopath.locationtracker.data.local.converter

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

/**
 * Room TypeConverter for kotlinx.datetime.Instant ↔ Long (database).
 */
class InstantConverter {
    
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilliseconds()
    }
    
    @TypeConverter
    fun toInstant(millis: Long?): Instant? {
        return millis?.let { Instant.fromEpochMilliseconds(it) }
    }
}
