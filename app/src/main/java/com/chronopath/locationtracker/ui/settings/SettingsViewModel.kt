package com.chronopath.locationtracker.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chronopath.locationtracker.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class TrackingIntervalOption(
    val label: String,
    val intervalMs: Long,
    val description: String
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val intervalOptions = listOf(
        TrackingIntervalOption("1 minute", 1 * 60 * 1000L, "High detail, high battery (~50-80%/day)"),
        TrackingIntervalOption("3 minutes", 3 * 60 * 1000L, "Detailed tracking (~16-32%/day)"),
        TrackingIntervalOption("5 minutes", 5 * 60 * 1000L, "Balanced (~8-24%/day)"),
        TrackingIntervalOption("10 minutes", 10 * 60 * 1000L, "Battery saver (~5-12%/day)"),
        TrackingIntervalOption("20 minutes", 20 * 60 * 1000L, "Minimal battery (~2-5%/day)")
    )

    private val _selectedIntervalMs = MutableStateFlow(5 * 60 * 1000L)
    val selectedIntervalMs: StateFlow<Long> = _selectedIntervalMs.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadCurrentSettings()
    }

    private fun loadCurrentSettings() {
        viewModelScope.launch {
            settingsRepository.trackingIntervalMs.collect { interval ->
                _selectedIntervalMs.value = interval
                Timber.tag("Settings").d("Loaded interval: ${interval}ms")
            }
        }
    }

    fun selectInterval(intervalMs: Long) {
        _selectedIntervalMs.value = intervalMs
    }

    fun saveSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                settingsRepository.setTrackingIntervalMs(_selectedIntervalMs.value)
                Timber.tag("Settings").i("Settings saved: interval=${_selectedIntervalMs.value}ms")
                _saveSuccess.value = true
            } catch (e: Exception) {
                Timber.tag("Settings").e(e, "Failed to save settings")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
}
