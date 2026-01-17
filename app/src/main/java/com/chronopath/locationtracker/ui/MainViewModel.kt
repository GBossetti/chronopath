package com.chronopath.locationtracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.core.di.AppModule
import com.chronopath.locationtracker.core.services.LocationTrackingService
import com.chronopath.locationtracker.domain.usecase.GetLocationCountUseCase
import com.chronopath.locationtracker.domain.usecase.StartTrackingUseCase
import com.chronopath.locationtracker.domain.usecase.StopTrackingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the main tracking screen.
 * Manages tracking state, location count, and user actions.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val startTrackingUseCase: StartTrackingUseCase =
        AppModule.provideStartTrackingUseCase(application)
    private val stopTrackingUseCase: StopTrackingUseCase =
        AppModule.provideStopTrackingUseCase(application)
    private val getLocationCountUseCase: GetLocationCountUseCase =
        AppModule.provideGetLocationCountUseCase(application)

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _locationCount = MutableStateFlow(0)
    val locationCount: StateFlow<Int> = _locationCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        Timber.tag("ViewModel").d("MainViewModel initialized")
        checkTrackingStatus()
        observeLocationCount()
    }

    private fun checkTrackingStatus() {
        _isTracking.value = LocationTrackingService.isRunning(getApplication())
        Timber.tag("ViewModel").d("checkTrackingStatus - isTracking: ${_isTracking.value}")
    }

    private fun observeLocationCount() {
        viewModelScope.launch {
            getLocationCountUseCase(Unit).collect { count ->
                Timber.tag("ViewModel").d("Location count updated: $count")
                _locationCount.value = count
            }
        }
    }

    fun startTracking() {
        Timber.tag("ViewModel").i("startTracking - User initiated tracking start")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = startTrackingUseCase(Unit)) {
                is Result.Success -> {
                    _isTracking.value = true
                    Timber.tag("ViewModel").i("Tracking started successfully")
                }
                is Result.Error -> {
                    _error.value = result.message ?: "Failed to start tracking"
                    Timber.tag("ViewModel").e("Failed to start tracking: ${result.message}")
                }
                is Result.Loading -> { /* Handled by isLoading state */ }
            }

            _isLoading.value = false
        }
    }

    fun stopTracking() {
        Timber.tag("ViewModel").i("stopTracking - User initiated tracking stop")
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = stopTrackingUseCase(Unit)) {
                is Result.Success -> {
                    _isTracking.value = false
                    Timber.tag("ViewModel").i("Tracking stopped successfully")
                }
                is Result.Error -> {
                    _error.value = result.message ?: "Failed to stop tracking"
                    Timber.tag("ViewModel").e("Failed to stop tracking: ${result.message}")
                }
                is Result.Loading -> { /* Handled by isLoading state */ }
            }

            _isLoading.value = false
        }
    }

    fun refreshTrackingStatus() {
        checkTrackingStatus()
    }

    fun clearError() {
        _error.value = null
    }
}
