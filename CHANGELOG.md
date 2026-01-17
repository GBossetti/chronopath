# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
