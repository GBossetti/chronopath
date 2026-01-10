package com.chronopath.locationtracker.core.common

/**
 * Application constants for tracking configuration.
 */
object Constants {
    // Location tracking defaults
    const val DEFAULT_TRACKING_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    const val DEFAULT_MIN_DISTANCE_METERS = 100f // 100 meters

    // Request codes
    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002

    // Notification
    const val NOTIFICATION_CHANNEL_ID = "location_tracking_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Location Tracking"
    const val NOTIFICATION_ID = 1

    // Shared Preferences keys
    const val PREFS_TRACKING_ACTIVE = "tracking_active"
    const val PREFS_FIRST_LAUNCH = "first_launch"

    // Export
    const val EXPORT_FILE_NAME = "locations_export.json"
}