package com.chronopath.locationtracker.data.source.network.impl

import android.content.Context
import com.chronopath.locationtracker.data.source.network.NetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AndroidNetworkDataSource(
    private val context: Context
) : NetworkDataSource {

    override fun getCurrentNetworkType(): String {
        return "Unknown" // Simplified for now
    }

    // Simplified implementation for brevity
    override val networkType: Flow<String> = flow {
        emit("Unknown")
    }
}