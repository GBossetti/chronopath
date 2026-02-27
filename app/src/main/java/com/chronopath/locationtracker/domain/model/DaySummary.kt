package com.chronopath.locationtracker.domain.model

import kotlinx.datetime.LocalDate

data class DaySummary(
    val date: LocalDate,
    val totalDistanceMeters: Float,
    val totalStayDurationMs: Long,
    val totalTripDurationMs: Long,
    val tripCount: Int,
    val stayCount: Int
)
