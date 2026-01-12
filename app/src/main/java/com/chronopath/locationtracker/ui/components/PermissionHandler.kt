package com.chronopath.locationtracker.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class PermissionState(
    val hasCoarseLocation: Boolean = false,
    val hasFineLocation: Boolean = false,
    val hasBackgroundLocation: Boolean = false,
    val hasNotificationPermission: Boolean = false
) {
    val hasBasicLocationPermission: Boolean
        get() = hasCoarseLocation || hasFineLocation

    val hasAllRequiredPermissions: Boolean
        get() = hasFineLocation && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || hasBackgroundLocation)
}

fun checkPermissionState(context: android.content.Context): PermissionState {
    return PermissionState(
        hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED,
        hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED,
        hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true,
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    )
}

@Composable
fun PermissionRequestCard(
    permissionState: PermissionState,
    onPermissionsGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPermissionState by remember { mutableStateOf(permissionState) }

    // Launcher for basic location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        currentPermissionState = currentPermissionState.copy(
            hasCoarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false,
            hasFineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        )
        if (currentPermissionState.hasBasicLocationPermission) {
            onPermissionsGranted()
        }
    }

    // Launcher for background location permission (Android 10+)
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        currentPermissionState = currentPermissionState.copy(
            hasBackgroundLocation = granted
        )
        if (granted) {
            onPermissionsGranted()
        }
    }

    // Launcher for notification permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        currentPermissionState = currentPermissionState.copy(
            hasNotificationPermission = granted
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Location Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "This app needs location permission to track your position in the background.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                !currentPermissionState.hasBasicLocationPermission -> {
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text("Grant Location Permission")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !currentPermissionState.hasBackgroundLocation -> {
                    Text(
                        text = "Background location is needed for tracking when the app is closed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            backgroundPermissionLauncher.launch(
                                Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            )
                        }
                    ) {
                        Text("Grant Background Location")
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !currentPermissionState.hasNotificationPermission -> {
                    Button(
                        onClick = {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    ) {
                        Text("Enable Notifications")
                    }
                }
            }
        }
    }
}
