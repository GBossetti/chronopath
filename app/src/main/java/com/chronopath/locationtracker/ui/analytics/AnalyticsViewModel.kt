package com.chronopath.locationtracker.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronopath.locationtracker.core.common.Constants
import com.chronopath.locationtracker.core.di.AppModule
import com.chronopath.locationtracker.domain.model.DaySummary
import com.chronopath.locationtracker.domain.model.MovementEvent
import com.chronopath.locationtracker.domain.usecase.AnalyzeMovementUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = AppModule.provideLocationRepository(application)
    private val analyzeMovementUseCase = AnalyzeMovementUseCase()

    private val _events = MutableStateFlow<List<MovementEvent>>(emptyList())
    val events: StateFlow<List<MovementEvent>> = _events.asStateFlow()

    private val _todaySummary = MutableStateFlow<DaySummary?>(null)
    val todaySummary: StateFlow<DaySummary?> = _todaySummary.asStateFlow()

    private val _insightText = MutableStateFlow("Loading...")
    val insightText: StateFlow<String> = _insightText.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        Timber.tag("AnalyticsVM").d("AnalyticsViewModel initialized")
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            locationRepository.getAllLocations().collect { locations ->
                Timber.tag("AnalyticsVM").d("Processing ${locations.size} locations")

                if (locations.isEmpty()) {
                    _events.value = emptyList()
                    _todaySummary.value = null
                    _insightText.value = "No data recorded yet"
                    _isLoading.value = false
                    return@collect
                }

                val sorted = locations.sortedBy { it.timestamp }
                val allEvents = analyzeMovementUseCase.analyze(sorted)

                val tz = TimeZone.currentSystemDefault()
                val today = Clock.System.now().toLocalDateTime(tz).date

                val todayEvents = allEvents.filter { event ->
                    when (event) {
                        is MovementEvent.Stay -> event.period.startTime.toLocalDateTime(tz).date == today
                        is MovementEvent.Trip -> event.trip.startTime.toLocalDateTime(tz).date == today
                    }
                }

                _events.value = todayEvents

                val daySummaries = analyzeMovementUseCase.summarizeByDay(allEvents)
                val summary = daySummaries.find { it.date == today }
                _todaySummary.value = summary

                _insightText.value = buildInsightText(summary)
                _isLoading.value = false
            }
        }
    }

    private fun buildInsightText(summary: DaySummary?): String {
        if (summary == null) return "No data recorded yet"

        val distanceM = summary.totalDistanceMeters
        return when {
            distanceM < Constants.INSIGHT_MOSTLY_STILL_M -> {
                val meters = distanceM.toInt()
                "Barely moved today — $meters m total"
            }
            distanceM > Constants.INSIGHT_ACTIVE_M -> {
                val km = String.format("%.1f", distanceM / 1000f)
                "Active day — ${summary.tripCount} trip${if (summary.tripCount != 1) "s" else ""}, $km km"
            }
            else -> {
                val km = String.format("%.1f", distanceM / 1000f)
                "Moderate activity — $km km across ${summary.tripCount} trip${if (summary.tripCount != 1) "s" else ""}"
            }
        }
    }
}
