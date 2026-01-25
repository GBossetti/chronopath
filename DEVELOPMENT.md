# Development Guide

Project architecture and development guide for ChronoPath.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug

# Clean build
./gradlew clean build

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.chronopath.locationtracker.ui.MainViewModelTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

## Tracking Configuration

### Interval vs Battery Trade-off

| Interval | GPS Wakeups/Hour | Estimated Daily Drain | Use Case |
|----------|------------------|----------------------|----------|
| 1 min | 60 | ~50-80% | Activity classification (walk/bike/car) |
| 3 min | 20 | ~16-32% | Detailed tracking, vacation diary |
| 5 min | 12 | ~8-24% | Balanced |
| 10 min | 6 | ~5-12% | Battery saver |
| 20 min | 3 | ~2-5% | Minimal, "where was I" only |

### Data Collected Per Location

Core (from GPS):
- `latitude`, `longitude`, `timestamp`
- `accuracy`, `altitude`, `speed`, `bearing`, `provider`

Context (minimal battery cost):
- `batteryPercentage`, `isCharging`
- `networkType` (WIFI/MOBILE/OFFLINE)
- `installationId`

### Configuration Files

- `core/common/Constants.kt`: Default values for interval and distance
- `data/settings/SettingsRepository.kt`: DataStore-based settings persistence
- `build.gradle.kts`: Build configuration and dependencies

## Architecture

This is a **Clean Architecture** Android app with Jetpack Compose UI.

### Layer Structure

```
domain/          Business logic layer (pure Kotlin, no Android dependencies)
├── model/       Domain entities (Location)
├── repository/  Repository interfaces (LocationRepository)
├── controller/  Tracking controller interface (TrackingController)
└── usecase/     Use case classes following UseCase<Input, Output> pattern

data/            Data layer implementation
├── local/       Room database (LocationDatabase, LocationDao, LocationEntity, InstantConverter)
├── repository/  Repository implementations (LocationRepositoryImpl)
├── mapper/      Domain <-> Entity mappers
├── settings/    User preferences (SettingsRepository with DataStore)
└── source/      Data sources (location, battery, network, device ID, aggregator)

ui/              Presentation layer
├── MainViewModel.kt   ViewModel managing tracking state and user actions
├── screen/      Screen composables (MainScreen)
├── settings/    Settings screen (SettingsScreen, SettingsViewModel)
├── components/  Reusable UI components (TrackingButton, PermissionHandler)
└── theme/       Compose Material3 theming

core/            Cross-cutting concerns
├── di/          AppModule (service locator pattern for DI)
├── common/      Constants, Result<T> sealed class
├── controller/  TrackingController implementation (TrackingControllerImpl)
├── services/    Foreground service (LocationTrackingService, NotificationHelper)
└── workers/     WorkManager workers for background tasks
```

### Key Architectural Patterns

- **Repository Pattern**: `LocationRepository` interface in domain, `LocationRepositoryImpl` in data
- **UseCase Pattern**: Each business operation is a `UseCase<Input, Output>` class
- **Result<T>**: Sealed class for unified error handling (`Result.Success`, `Result.Error`)
- **Flow-based Reactivity**: Kotlin Flows for async data streams throughout
- **Data Aggregation**: `DataAggregator` combines location + battery + network + device ID into unified `Location` objects

### Background Processing

- **WorkManager** handles background tasks that survive app/process death
- `TrackingHealthWorker`: Periodic (30 min) check to ensure tracking service is running
- `AppStartupWorker`: One-time worker on app start to restore tracking state
- `WorkerUtils`: Shared utilities for workers (action data, service control, settings access)
- `WorkManagerInitializer`: Uses App Startup Library for early initialization

### Data Sources

- `FusedLocationDataSource`: Google Play Services location provider with configurable accuracy/intervals
- `AndroidBatteryDataSource`: Battery state via BroadcastReceiver + callbackFlow
- `AndroidNetworkDataSource`: Network connectivity detection
- `DeviceIdManager`: Persistent installation UUID via SharedPreferences

## Key Files

- `MainActivity.kt`: Single Compose-based entry point
- `LocationTrackerApplication.kt`: Application class for app-level initialization
- `ui/MainViewModel.kt`: ViewModel managing tracking state and user actions
- `core/di/AppModule.kt`: Manual dependency injection setup
- `core/services/LocationTrackingService.kt`: Foreground service for continuous location tracking
- `domain/controller/TrackingController.kt`: Interface for tracking control (Dependency Inversion)
- `domain/repository/LocationRepository.kt`: Main data access interface
- `data/local/LocationDatabase.kt`: Room database configuration
- `data/source/aggregator/DataAggregator.kt`: Combines multi-source sensor data

## Tech Stack

- **Language**: Kotlin 1.9.22
- **UI**: Jetpack Compose with Material3
- **Database**: Room 2.6.1
- **Async**: Coroutines 1.7.3 + kotlinx-datetime
- **Location**: Google Play Services Location 21.0.1
- **Background**: WorkManager 2.9.0
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Java Target**: 17
