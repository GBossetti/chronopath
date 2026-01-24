package com.chronopath.locationtracker.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chronopath.locationtracker.BuildConfig
import com.chronopath.locationtracker.core.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_TRACKING_INTERVAL_MS = longPreferencesKey("tracking_interval_ms")
        private val KEY_MIN_DISTANCE_METERS = floatPreferencesKey("min_distance_meters")
        private val KEY_IS_TRACKING_ACTIVE = booleanPreferencesKey("is_tracking_active")
    }

    val trackingIntervalMs: Flow<Long> = context.dataStore.data.map { preferences ->
        if (!BuildConfig.HAS_SETTINGS_UI && BuildConfig.FIXED_TRACKING_INTERVAL_MS > 0) {
            BuildConfig.FIXED_TRACKING_INTERVAL_MS
        } else {
            preferences[KEY_TRACKING_INTERVAL_MS] ?: Constants.DEFAULT_TRACKING_INTERVAL_MS
        }
    }

    val minDistanceMeters: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_MIN_DISTANCE_METERS] ?: Constants.DEFAULT_MIN_DISTANCE_METERS
    }

    suspend fun getTrackingIntervalMs(): Long {
        if (!BuildConfig.HAS_SETTINGS_UI && BuildConfig.FIXED_TRACKING_INTERVAL_MS > 0) {
            return BuildConfig.FIXED_TRACKING_INTERVAL_MS
        }
        return context.dataStore.data.first()[KEY_TRACKING_INTERVAL_MS]
            ?: Constants.DEFAULT_TRACKING_INTERVAL_MS
    }

    suspend fun getMinDistanceMeters(): Float {
        return context.dataStore.data.first()[KEY_MIN_DISTANCE_METERS]
            ?: Constants.DEFAULT_MIN_DISTANCE_METERS
    }

    suspend fun setTrackingIntervalMs(intervalMs: Long) {
        if (!BuildConfig.HAS_SETTINGS_UI) return
        context.dataStore.edit { preferences ->
            preferences[KEY_TRACKING_INTERVAL_MS] = intervalMs
        }
    }

    suspend fun setMinDistanceMeters(distanceMeters: Float) {
        if (!BuildConfig.HAS_SETTINGS_UI) return
        context.dataStore.edit { preferences ->
            preferences[KEY_MIN_DISTANCE_METERS] = distanceMeters
        }
    }

    val isTrackingActive: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_TRACKING_ACTIVE] ?: false
    }

    suspend fun getIsTrackingActive(): Boolean {
        return context.dataStore.data.first()[KEY_IS_TRACKING_ACTIVE] ?: false
    }

    suspend fun setIsTrackingActive(isActive: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_TRACKING_ACTIVE] = isActive
        }
    }
}
