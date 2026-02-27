package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.Instant

data class StayPeriod(
    val centroidLat: Double,
    val centroidLon: Double,
    val startTime: Instant,
    val endTime: Instant,
    val pointCount: Int
) {
    val durationMs: Long get() = endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()
}
