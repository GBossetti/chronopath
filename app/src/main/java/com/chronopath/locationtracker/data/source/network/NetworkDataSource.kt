package com.chronopath.locationtracker.data.source.network

import kotlinx.coroutines.flow.Flow

interface NetworkDataSource {
    val networkType: Flow<String> // "WIFI", "MOBILE", "OFFLINE"
}