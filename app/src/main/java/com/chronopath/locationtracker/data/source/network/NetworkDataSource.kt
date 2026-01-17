package com.chronopath.locationtracker.data.source.network

import kotlinx.coroutines.flow.Flow

interface NetworkDataSource {
    val networkType: Flow<String> // "WIFI", "MOBILE", "OFFLINE"

    // Direct read for point-in-time data (used when location arrives)
    fun getCurrentNetworkType(): String
}