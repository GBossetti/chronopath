package com.chronopath.locationtracker.data.source.battery

import kotlinx.coroutines.flow.Flow

interface BatteryDataSource {
    val batteryPercentage: Flow<Int>
    val isCharging: Flow<Boolean>
}