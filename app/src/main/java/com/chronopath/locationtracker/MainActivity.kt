package com.chronopath.locationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chronopath.locationtracker.ui.analytics.AnalyticsScreen
import com.chronopath.locationtracker.ui.screen.MainScreen
import com.chronopath.locationtracker.ui.settings.SettingsScreen
import com.chronopath.locationtracker.ui.theme.LocationTrackerTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag("Activity").d("onCreate")
        enableEdgeToEdge()
        setContent {
            LocationTrackerTheme {
                var showSettings  by remember { mutableStateOf(false) }
                var showAnalytics by remember { mutableStateOf(false) }

                when {
                    showSettings  -> SettingsScreen(
                        onNavigateBack = { showSettings = false }
                    )
                    showAnalytics -> AnalyticsScreen(
                        onNavigateBack = { showAnalytics = false }
                    )
                    else -> MainScreen(
                        onNavigateToSettings  = { showSettings  = true },
                        onNavigateToAnalytics = { showAnalytics = true }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("Activity").d("onResume - App in FOREGROUND")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("Activity").d("onPause - App in BACKGROUND")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("Activity").d("onDestroy")
    }
}
