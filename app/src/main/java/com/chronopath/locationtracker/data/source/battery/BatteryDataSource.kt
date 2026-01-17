package com.chronopath.locationtracker.data.source.battery

import kotlinx.coroutines.flow.Flow

interface BatteryDataSource {
    val batteryPercentage: Flow<Int>
    val isCharging: Flow<Boolean>

    // Direct reads for point-in-time data (used when location arrives)
    fun getCurrentBatteryPercentage(): Int
    fun getCurrentChargingState(): Boolean
}