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
        checkTrackingStatus()
        observeLocationCount()
    }

    private fun checkTrackingStatus() {
        _isTracking.value = LocationTrackingService.isRunning(getApplication())
    }

    private fun observeLocationCount() {
        viewModelScope.launch {
            getLocationCountUseCase(Unit).collect { count ->
                _locationCount.value = count
            }
        }
    }

    fun startTracking() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = startTrackingUseCase(Unit)) {
                is Result.Success -> {
                    _isTracking.value = true
                }
                is Result.Error -> {
                    _error.value = result.message ?: "Failed to start tracking"
                }
                is Result.Loading -> { /* Handled by isLoading state */ }
            }

            _isLoading.value = false
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = stopTrackingUseCase(Unit)) {
                is Result.Success -> {
                    _isTracking.value = false
                }
                is Result.Error -> {
                    _error.value = result.message ?: "Failed to stop tracking"
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
