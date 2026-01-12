package com.chronopath.locationtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackingButton(
    isTracking: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            if (isTracking) {
                onStopClick()
            } else {
                onStartClick()
            }
        },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = if (isTracking) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isTracking) "Stop Tracking" else "Start Tracking",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
