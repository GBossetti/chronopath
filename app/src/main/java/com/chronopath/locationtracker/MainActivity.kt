package com.chronopath.locationtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chronopath.locationtracker.ui.screen.MainScreen
import com.chronopath.locationtracker.ui.theme.LocationTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocationTrackerTheme {
                MainScreen()
            }
        }
    }
}
