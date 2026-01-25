# ChronoPath

Android location tracking app built with Clean Architecture and Jetpack Compose. Records your location history with battery and network context for personal tracking and diary purposes.

## Features

- **Background Location Tracking** - Continuous GPS tracking via foreground service
- **Configurable Interval** - Choose from 1, 3, 5, 10, or 20 minute intervals
- **Context Capture** - Records battery level, charging state, and network type with each location
- **Automatic Recovery** - Workers restore tracking after app kill or device restart
- **Battery Estimates** - Settings show expected battery consumption for each interval

## Requirements

- Android 8.0+ (API 26)
- Google Play Services (for Fused Location Provider)

## Permissions

| Permission | Purpose |
|------------|---------|
| `ACCESS_FINE_LOCATION` | GPS location tracking |
| `ACCESS_COARSE_LOCATION` | Network-based location fallback |
| `ACCESS_BACKGROUND_LOCATION` | Track when app is in background |
| `FOREGROUND_SERVICE` | Keep tracking service running |
| `POST_NOTIFICATIONS` | Show tracking notification |
| `RECEIVE_BOOT_COMPLETED` | Restore tracking after reboot |

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Run tests
./gradlew test
```

Output APK: `app/build/outputs/apk/debug/ChronoPath_v{version}_(Build{code})_debug.apk`

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material3)
- **Database**: Room
- **Background**: WorkManager
- **Location**: Google Play Services Location
- **Async**: Coroutines + Flow

## Architecture

Clean Architecture with three layers:
- **Domain** - Business logic, use cases, repository interfaces
- **Data** - Room database, data sources, repository implementations
- **UI** - Compose screens, ViewModels

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed architecture documentation.

## License

Private project.
