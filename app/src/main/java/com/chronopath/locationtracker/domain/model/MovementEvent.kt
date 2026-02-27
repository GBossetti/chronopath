package com.chronopath.locationtracker.domain.model

sealed class MovementEvent {
    data class Stay(val period: StayPeriod) : MovementEvent()
    data class Trip(val trip: com.chronopath.locationtracker.domain.model.Trip) : MovementEvent()
}
