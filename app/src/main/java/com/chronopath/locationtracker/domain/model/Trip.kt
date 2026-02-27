package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant

data class Trip(
    val startTime: Instant,
    val endTime: Instant,
    val totalDistanceMeters: Float,
    val pointCount: Int
) {
    val durationMs: Long get() = endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()
    val avgSpeedMs: Float get() = if (durationMs > 0) totalDistanceMeters / (durationMs / 1000f) else 0f
}
