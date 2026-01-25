# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-01-25

### Changed
- **Simplified to single app variant** - Removed lite flavor, now single configurable app
- Removed unused dependencies: appcompat, lifecycle-livedata-ktx, compose-ui-tooling-preview, espresso-core
- Consolidated duplicate code in workers via new `WorkerUtils` class
- Extracted `isChargingStatus()` helper in `AndroidBatteryDataSource`

### Added
- Unit tests for `MainViewModel` (12 tests)
- Unit tests for `TrackingControllerImpl` (8 tests)
- Expanded `SettingsViewModelTest` with ViewModel behavior tests (10 new tests)

### Technical
- Smaller APK size due to removed dependencies
- Better test coverage for critical components
- Cleaner worker code with shared utilities

## [1.1.0] - 2026-01-24

### Added
- **Build flavors**: Two product variants from single codebase
  - `full`: Configurable tracking interval with Settings UI
  - `lite`: Fixed 20-minute interval, minimal battery (~2-5%/day)
- **Settings screen** (full flavor): Choose interval from 1, 3, 5, 10, or 20 minutes
- **SettingsRepository**: DataStore-based persistence for user preferences
- **Battery estimates**: Settings UI shows expected battery consumption per option
- Unit tests for SettingsViewModel and Constants

### Changed
- LocationTrackingService reads interval from SettingsRepository
- APK naming includes flavor: `ChronoPath_{flavor}_v{version}_{build}.apk`
- Updated DEVELOPMENT.md with flavor documentation and build commands

### Technical
- Added DataStore dependency for preferences
- Each flavor has separate package name and isolated database
- BuildConfig flags: `HAS_SETTINGS_UI`, `FIXED_TRACKING_INTERVAL_MS`

## [1.0.0] - 2026-01-17

### Added
- Location tracking with foreground service
- Periodic health worker to recover service if killed by system
- Startup worker to restore tracking state on app launch
- Comprehensive Timber logging for debugging:
  - Service lifecycle events (start/stop/destroy)
  - Location updates from GPS
  - Database operations
  - Worker execution
  - Activity lifecycle (foreground/background)
- Battery percentage and charging state capture with each location
- Network type capture with each location

### Fixed
- Fixed critical bug causing ~175x duplicate location saves per actual GPS update
  - Root cause: `combine()` operator emitted on every battery broadcast
  - Solution: Changed to `map()` on location flow with synchronous battery/network reads

### Technical
- Room database for local storage
- FusedLocationProviderClient for GPS
- WorkManager for background task scheduling
- Jetpack Compose for UI
