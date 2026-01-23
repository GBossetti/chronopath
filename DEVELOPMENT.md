# Development Guide

Project architecture and development guide for ChronoPath.

## Build Commands

```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Install debug APK to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.chronopath.locationtracker.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Generate APK without installing
./gradlew assembleDebug
```

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
└── source/      Data sources (location, battery, network, device ID, aggregator)

ui/              Presentation layer
├── MainViewModel.kt   ViewModel managing tracking state and user actions
├── screen/      Screen composables (MainScreen)
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
