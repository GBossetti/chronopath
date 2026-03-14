package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.domain.model.DaySummary
import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.model.MovementEvent
import com.chronopath.locationtracker.domain.model.StayPeriod
import com.chronopath.locationtracker.domain.model.Trip
import com.chronopath.locationtracker.core.common.AppLogger
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val STAY_THRESHOLD_METERS = 50f

private enum class TrackingState { STAY, TRIP }

class AnalyzeMovementUseCase(
    private val calcDistance: (Double, Double, Double, Double) -> Float = { lat1, lon1, lat2, lon2 ->
        val R = 6_371_000.0 // Earth radius in metres
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        (2 * R * asin(sqrt(a))).toFloat()
    }
) {

    /**
     * Analyzes a time-sorted list of locations and returns interleaved Stay and Trip events.
     */
    fun analyze(locations: List<Location>): List<MovementEvent> {
        AppLogger.d("Movement", "analyze() called — ${locations.size} locations")
        if (locations.isEmpty()) return emptyList()
        if (locations.size == 1) {
            val loc = locations.first()
            return listOf(
                MovementEvent.Stay(
                    StayPeriod(
                        centroidLat = loc.latitude,
                        centroidLon = loc.longitude,
                        startTime = loc.timestamp,
                        endTime = loc.timestamp,
                        pointCount = 1
                    )
                )
            )
        }

        val events = mutableListOf<MovementEvent>()

        var state = TrackingState.STAY
        var stayPoints = mutableListOf(locations.first())
        var tripPoints = mutableListOf<Location>()
        var tripDistance = 0f
        var previousPoint = locations.first()

        for (i in 1 until locations.size) {
            val point = locations[i]
            val dist = calcDistance(
                previousPoint.latitude, previousPoint.longitude,
                point.latitude, point.longitude
            )

            when (state) {
                TrackingState.STAY -> {
                    if (dist < STAY_THRESHOLD_METERS) {
                        stayPoints.add(point)
                    } else {
                        events.add(buildStay(stayPoints))
                        AppLogger.d("Movement", "STAY→TRIP at i=$i dist=%.1fm".format(dist))
                        tripPoints = mutableListOf(previousPoint, point)
                        tripDistance = dist
                        state = TrackingState.TRIP
                    }
                }
                TrackingState.TRIP -> {
                    if (dist < STAY_THRESHOLD_METERS) {
                        events.add(buildTrip(tripPoints, tripDistance))
                        AppLogger.d("Movement", "TRIP→STAY at i=$i dist=%.1fm".format(dist))
                        stayPoints = mutableListOf(previousPoint, point)
                        tripPoints = mutableListOf()
                        tripDistance = 0f
                        state = TrackingState.STAY
                    } else {
                        tripPoints.add(point)
                        tripDistance += dist
                    }
                }
            }

            previousPoint = point
        }

        // Emit the final open segment
        when (state) {
            TrackingState.STAY -> events.add(buildStay(stayPoints))
            TrackingState.TRIP -> events.add(buildTrip(tripPoints, tripDistance))
        }

        AppLogger.i("Movement", "analyze() done — ${events.size} events")
        return events
    }

    private fun buildStay(points: List<Location>): MovementEvent.Stay {
        val centroidLat = points.map { it.latitude }.average()
        val centroidLon = points.map { it.longitude }.average()
        return MovementEvent.Stay(
            StayPeriod(
                centroidLat = centroidLat,
                centroidLon = centroidLon,
                startTime = points.first().timestamp,
                endTime = points.last().timestamp,
                pointCount = points.size
            )
        )
    }

    private fun buildTrip(points: List<Location>, totalDistance: Float): MovementEvent.Trip {
        return MovementEvent.Trip(
            Trip(
                startTime = points.first().timestamp,
                endTime = points.last().timestamp,
                totalDistanceMeters = totalDistance,
                pointCount = points.size
            )
        )
    }

    /**
     * Groups a list of movement events by local date and returns per-day summaries.
     */
    fun summarizeByDay(events: List<MovementEvent>): List<DaySummary> {
        val tz = TimeZone.currentSystemDefault()

        data class DayAccumulator(
            var totalDistanceMeters: Float = 0f,
            var totalStayDurationMs: Long = 0L,
            var totalTripDurationMs: Long = 0L,
            var tripCount: Int = 0,
            var stayCount: Int = 0
        )

        val dayMap = mutableMapOf<kotlinx.datetime.LocalDate, DayAccumulator>()

        for (event in events) {
            when (event) {
                is MovementEvent.Stay -> {
                    val date = event.period.startTime.toLocalDateTime(tz).date
                    val acc = dayMap.getOrPut(date) { DayAccumulator() }
                    acc.totalStayDurationMs += event.period.durationMs
                    acc.stayCount++
                }
                is MovementEvent.Trip -> {
                    val date = event.trip.startTime.toLocalDateTime(tz).date
                    val acc = dayMap.getOrPut(date) { DayAccumulator() }
                    acc.totalDistanceMeters += event.trip.totalDistanceMeters
                    acc.totalTripDurationMs += event.trip.durationMs
                    acc.tripCount++
                }
            }
        }

        return dayMap.entries
            .sortedBy { it.key }
            .map { (date, acc) ->
                DaySummary(
                    date = date,
                    totalDistanceMeters = acc.totalDistanceMeters,
                    totalStayDurationMs = acc.totalStayDurationMs,
                    totalTripDurationMs = acc.totalTripDurationMs,
                    tripCount = acc.tripCount,
                    stayCount = acc.stayCount
                )
            }
    }
}
