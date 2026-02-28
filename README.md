# ChronoPath

Android app that continuously records GPS location history with contextual device metadata and performs on-device movement analysis. All data is stored locally in an encrypted SQLite database — no network transmission, no cloud sync.

**Min SDK:** API 26 (Android 8.0) · **Target SDK:** API 34 · **Language:** Kotlin

---

## How It Works

### Tracking Pipeline

Tracking runs inside a **foreground service** (`LocationTrackingService`) that keeps the process alive while a persistent notification is shown. The pipeline from GPS to database is:

```
FusedLocationProviderClient
        │  location updates (Flow)
        ▼
  FusedLocationDataSource
        │
        ▼
    DataAggregator           ← synchronously reads battery + network at fix time
        │  emits Location domain objects
        ▼
 LocationRepositoryImpl
        │  Room + SQLCipher
        ▼
  location_tracker_encrypted.db
```

`DataAggregator` maps the Android `Location` object to the domain `Location` entity by attaching battery percentage, charging state, and network type. These are read **synchronously** when the GPS fix arrives — not via `combine()` — to prevent duplicate saves caused by battery broadcast emissions.

The service uses `START_STICKY`, so the OS will restart it after a process kill. It also preserves tracking state in DataStore: on `onDestroy()` the flag is intentionally **not** cleared, so the `TrackingHealthWorker` can detect the unexpected stop and restart the service.

### Configurable Intervals

Tracking interval and minimum displacement are user-configurable. The `FusedLocationDataSource` passes these directly to `FusedLocationProviderClient`:

| Interval | GPS wakeups/hour | Estimated daily drain |
|----------|------------------|-----------------------|
| 1 min    | 60               | ~50–80%               |
| 3 min    | 20               | ~16–32%               |
| 5 min    | 12               | ~8–24%                |
| 10 min   | 6                | ~5–12%                |
| 20 min   | 3                | ~2–5%                 |

Settings are persisted with Jetpack DataStore and read by the service on each start.

---

## Movement Analysis

`AnalyzeMovementUseCase` processes a time-sorted `List<Location>` into a sequence of `MovementEvent` objects using a two-state machine:

```
States: STAY | TRIP
Transition condition: haversine distance between consecutive fixes ≥ 50 m
```

**Algorithm:**

```
state = STAY, stayPoints = [first fix]

for each subsequent fix:
  dist = haversine(prev, current)

  if state == STAY:
    dist < 50m  → accumulate fix into stayPoints
    dist ≥ 50m  → emit Stay(centroid, duration), switch to TRIP

  if state == TRIP:
    dist ≥ 50m  → accumulate fix, add to totalDistance
    dist < 50m  → emit Trip(start, end, totalDistance), switch to STAY

emit final open segment
```

**Distance calculation** uses the Haversine formula with Earth radius = 6,371,000 m:

```
a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlon/2)
d = 2R·arcsin(√a)
```

### Event Types

| Type | Fields |
|------|--------|
| `MovementEvent.Stay` | `centroidLat`, `centroidLon`, `startTime`, `endTime`, `pointCount` |
| `MovementEvent.Trip` | `startTime`, `endTime`, `totalDistanceMeters`, `pointCount` |

Centroid for stays is computed as the arithmetic mean of all accumulated lat/lon values.

`summarizeByDay()` groups events by local date (system timezone) and produces `DaySummary` objects:

| Field | Description |
|-------|-------------|
| `totalDistanceMeters` | Sum of trip distances |
| `totalStayDurationMs` | Cumulative stay time |
| `totalTripDurationMs` | Cumulative trip time |
| `tripCount` / `stayCount` | Event counts |

---

## Data Schema

Each recorded fix is a `Location` domain entity:

```kotlin
data class Location(
    val latitude: Double,          // WGS-84
    val longitude: Double,         // WGS-84
    val timestamp: Instant,        // kotlinx-datetime, UTC

    val accuracy: Float?,          // Horizontal accuracy in meters
    val altitude: Double?,         // Meters above sea level (GPS only)
    val speed: Float?,             // m/s (Android-calculated)
    val bearing: Float?,           // Degrees from true north
    val provider: String?,         // "gps" | "network" | "fused"

    val batteryPercentage: Int?,   // 0–100
    val isCharging: Boolean?,
    val networkType: String?,      // "WIFI" | "MOBILE" | "OFFLINE"

    val installationId: String,    // Stable per-install UUID
    val advertisingId: String?     // AAID (unused, null)
)
```

`altitude`, `speed`, and `bearing` are only set when the Android `Location` object reports them as available (`hasAltitude()`, `hasSpeed()`, `hasBearing()`).

---

## Architecture

Clean Architecture with three layers separated by package:

```
domain/          Pure Kotlin — no Android imports
├── model/       Location, MovementEvent, StayPeriod, Trip, DaySummary
├── repository/  LocationRepository interface
├── controller/  TrackingController interface
└── usecase/     UseCase<Input, Output> base type; one class per operation

data/            Android implementations
├── local/       Room database, DAO, entities, type converters
├── repository/  LocationRepositoryImpl
├── mapper/      LocationEntity ↔ Location domain mapping
├── settings/    SettingsRepository (DataStore)
└── source/      FusedLocationDataSource, AndroidBatteryDataSource,
                 AndroidNetworkDataSource, DeviceIdManager, DataAggregator

ui/              Jetpack Compose
├── MainViewModel, MainScreen
├── settings/    SettingsViewModel, SettingsScreen
├── analytics/   AnalyticsViewModel, AnalyticsScreen
└── components/  TrackingButton, PermissionHandler

core/
├── di/          AppModule — manual service locator (no Hilt)
├── common/      Constants, Result<T> sealed class
├── security/    SecureKeyManager, DatabaseMigrationHelper
├── services/    LocationTrackingService, NotificationHelper
└── workers/     TrackingHealthWorker, AppStartupWorker, WorkerUtils
```

Dependency injection is manual via `AppModule` (singleton object). ViewModels extend `AndroidViewModel` and pull dependencies from `AppModule` in their constructors.

Data flows upward through `StateFlow`: `Room Flow → ViewModel StateFlow → Compose collectAsState()`. There is no polling.

---

## Background Resilience

Two WorkManager workers maintain tracking across system events:

**`TrackingHealthWorker`** — periodic, every 30 minutes
1. Reads `isTrackingActive` flag from DataStore
2. If active, checks whether `LocationTrackingService` is actually running
3. If not running (system-killed), restarts it and verifies the restart
4. Uses exponential backoff on failure

**`AppStartupWorker`** — one-time, triggered on app launch
- Restores tracking if `isTrackingActive` was true before the app was killed or the device rebooted

WorkManager is initialized early via the App Startup library (`WorkManagerInitializer`), before `Application.onCreate()` completes, to close the window where a reboot could occur before workers are registered.

---

## Security

All sensitive data is encrypted at rest. Nothing leaves the device.

| Layer | Mechanism |
|-------|-----------|
| Database | SQLCipher 4.5.4 — AES-256 encryption via `SupportFactory` |
| Encryption key | Android Keystore — hardware-backed on supported devices |
| Preferences | `EncryptedSharedPreferences` (AES-256-GCM + AES-256-SIV) |
| Network | `network_security_config.xml` enforces HTTPS; cleartext disabled |
| Cloud backup | `android:allowBackup="false"` in manifest |
| Release builds | ProGuard/R8 obfuscation + resource shrinking |
| Logging | GPS coordinates are never written to logcat in any build variant |

`SecureKeyManager` generates and retrieves the SQLCipher key from the Android Keystore. `DatabaseMigrationHelper` handles one-time migration from the legacy unencrypted `location_tracker.db` to `location_tracker_encrypted.db`, then deletes the plaintext file.

---

## Tech Stack

| Component | Library / Version |
|-----------|-------------------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material3 |
| Database | Room 2.6.1 |
| Encryption | SQLCipher 4.5.4, AndroidX Security Crypto 1.1.0-alpha06 |
| Location | Google Play Services Location 21.0.1 |
| Background | WorkManager 2.9.0 |
| Async | Coroutines 1.7.3, Kotlin Flow |
| Date/Time | kotlinx-datetime |
| Preferences | Jetpack DataStore |
| Logging | Timber |

---

## Build

Requires Android SDK and a connected device or emulator with Google Play Services.

```bash
# Debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Unit tests
./gradlew test

# Clean build
./gradlew clean assembleDebug
```

Output: `app/build/outputs/apk/debug/ChronoPath_v{version}_(Build{code})_debug.apk`

See [DEVELOPMENT.md](DEVELOPMENT.md) for tracking configuration details and architecture notes.

## Permissions

| Permission | Reason |
|------------|--------|
| `ACCESS_FINE_LOCATION` | GPS fix |
| `ACCESS_COARSE_LOCATION` | Network-based fallback |
| `ACCESS_BACKGROUND_LOCATION` | Track when app is not in foreground |
| `FOREGROUND_SERVICE` | Keep tracking service running |
| `POST_NOTIFICATIONS` | Persistent tracking notification |
| `RECEIVE_BOOT_COMPLETED` | Restore tracking after device reboot |
