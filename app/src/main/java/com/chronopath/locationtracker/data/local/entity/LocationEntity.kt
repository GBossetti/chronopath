package com.chronopath.locationtracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.chronopath.locationtracker.data.local.converter.InstantConverter
import kotlinx.datetime.Instant

/**
 * Room entity representing the database table structure.
 * This is a framework-specific implementation detail.
 */
@Entity(tableName = "locations")
@TypeConverters(InstantConverter::class)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Instant,
    
    // Extended location data
    @ColumnInfo(name = "accuracy")
    val accuracy: Float? = null,
    
    @ColumnInfo(name = "altitude")
    val altitude: Double? = null,
    
    @ColumnInfo(name = "speed")
    val speed: Float? = null,
    
    @ColumnInfo(name = "bearing")
    val bearing: Float? = null,
    
    @ColumnInfo(name = "provider")
    val provider: String? = null,
    
    // Device context
    @ColumnInfo(name = "battery_percentage")
    val batteryPercentage: Int? = null,
    
    @ColumnInfo(name = "is_charging")
    val isCharging: Boolean? = null,
    
    @ColumnInfo(name = "network_type")
    val networkType: String? = null,
    
    // Device identification
    @ColumnInfo(name = "installation_id", index = true)
    val installationId: String,
    
    @ColumnInfo(name = "advertising_id")
    val advertisingId: String? = null
)
