package com.chronopath.locationtracker.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chronopath.locationtracker.ui.MainViewModel
import com.chronopath.locationtracker.ui.components.PermissionRequestCard
import com.chronopath.locationtracker.ui.components.PermissionState
import com.chronopath.locationtracker.ui.components.TrackingButton
import com.chronopath.locationtracker.ui.components.checkPermissionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: MainViewModel = viewModel()
) {
    val isTracking by viewModel.isTracking.collectAsState()
    val locationCount by viewModel.locationCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(checkPermissionState(context)) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Refresh tracking status when permissions change
    LaunchedEffect(permissionState.hasAllRequiredPermissions) {
        if (permissionState.hasAllRequiredPermissions) {
            viewModel.refreshTrackingStatus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ChronoPath",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission request card (if needed)
            if (!permissionState.hasAllRequiredPermissions) {
                PermissionRequestCard(
                    permissionState = permissionState,
                    onPermissionsGranted = {
                        permissionState = PermissionState(
                            hasCoarseLocation = true,
                            hasFineLocation = true,
                            hasBackgroundLocation = true,
                            hasNotificationPermission = true
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Card
            StatusCard(
                isTracking = isTracking,
                modifier = Modifier.fillMaxWidth()
            )

            // Location Count Card
            LocationCountCard(
                count = locationCount,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Tracking Button
            TrackingButton(
                isTracking = isTracking,
                isLoading = isLoading,
                enabled = permissionState.hasAllRequiredPermissions,
                onStartClick = { viewModel.startTracking() },
                onStopClick = { viewModel.stopTracking() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(
    isTracking: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isTracking) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelMedium,
                color = if (isTracking) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isTracking) "Tracking Active" else "Tracking Inactive",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isTracking) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun LocationCountCard(
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Locations Saved",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
