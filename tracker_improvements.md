# Location Tracker - Improvements Analysis

This document analyzes potential improvements to the location tracking system, focusing on the trade-offs between battery consumption and data accuracy.

---

## Current Configuration

**File:** `app/src/main/java/com/chronopath/locationtracker/core/common/Constants.kt`

```kotlin
const val DEFAULT_TRACKING_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes
const val DEFAULT_MIN_DISTANCE_METERS = 100f             // 100 meters
```

### Current Behavior

The FusedLocationProvider uses **BOTH** parameters together:

```
Location saved ONLY when:
   (time since last update >= 5 minutes)
   AND
   (distance moved >= 100 meters)
```

| Scenario | Locations Saved |
|----------|-----------------|
| Stationary at home for 8 hours | 0 |
| Walking continuously for 1 hour | ~12 (every 5 min) |
| Commute 10km in 30 min | ~6 |

### Current Battery Impact

- GPS polls: 12 per hour
- Estimated drain: ~2-3% per hour of active tracking
- Priority: `PRIORITY_HIGH_ACCURACY`

---

## How FusedLocationProvider Parameters Work

### intervalMillis (Polling Interval)

Controls how often the GPS hardware is activated to check position.

- **Lower value** = More frequent GPS activation = More battery drain
- **Higher value** = Less frequent checks = Less battery drain but delayed detection

### minDistanceMeters (Distance Filter)

Filters out location updates if the user hasn't moved enough.

- This is a **filter only** - GPS still activates based on `intervalMillis`
- If you haven't moved the minimum distance, the update is discarded (battery already spent)
- Does NOT reduce GPS polling frequency

### Priority Levels

| Priority | Accuracy | Battery | Use Case |
|----------|----------|---------|----------|
| `PRIORITY_HIGH_ACCURACY` | ~10m | High | Precise tracking (current) |
| `PRIORITY_BALANCED_POWER_ACCURACY` | ~40m | Medium | City-level tracking |
| `PRIORITY_LOW_POWER` | ~100m | Low | Rough area tracking |
| `PRIORITY_PASSIVE` | Varies | Minimal | Opportunistic only |

---

## Option 1: Fixed Parameter Changes

### Conservative (Better Battery)

```kotlin
const val DEFAULT_TRACKING_INTERVAL_MS = 2 * 60 * 1000L  // 2 minutes
const val DEFAULT_MIN_DISTANCE_METERS = 50f              // 50 meters
```

| Metric | Current | This Option |
|--------|---------|-------------|
| GPS polls/hour | 12 | 30 |
| Battery drain/hour | ~2-3% | ~4-5% |
| Min detectable movement | 100m in 5 min | 50m in 2 min |
| Walking detection | Every 5 min | Every 2 min |

### Balanced (Recommended for 50m tracking)

```kotlin
const val DEFAULT_TRACKING_INTERVAL_MS = 1 * 60 * 1000L  // 1 minute
const val DEFAULT_MIN_DISTANCE_METERS = 50f              // 50 meters
```

| Metric | Current | This Option |
|--------|---------|-------------|
| GPS polls/hour | 12 | 60 |
| Battery drain/hour | ~2-3% | ~5-8% |
| Min detectable movement | 100m in 5 min | 50m in 1 min |
| Walking detection | Every 5 min | Every 1 min |

### Aggressive (High Accuracy)

```kotlin
const val DEFAULT_TRACKING_INTERVAL_MS = 30 * 1000L  // 30 seconds
const val DEFAULT_MIN_DISTANCE_METERS = 50f          // 50 meters
```

| Metric | Current | This Option |
|--------|---------|-------------|
| GPS polls/hour | 12 | 120 |
| Battery drain/hour | ~2-3% | ~10-15% |
| Min detectable movement | 100m in 5 min | 50m in 30 sec |
| Walking detection | Every 5 min | Near real-time |

### Real-Time (Maximum Accuracy)

```kotlin
const val DEFAULT_TRACKING_INTERVAL_MS = 10 * 1000L  // 10 seconds
const val DEFAULT_MIN_DISTANCE_METERS = 50f          // 50 meters
```

| Metric | Current | This Option |
|--------|---------|-------------|
| GPS polls/hour | 12 | 360 |
| Battery drain/hour | ~2-3% | ~20-30% |
| Min detectable movement | 100m in 5 min | 50m in 10 sec |
| Walking detection | Every 5 min | Real-time |

---

## Option 2: Adaptive Tracking (Recommended)

Dynamically adjusts tracking frequency based on movement detection.

### Concept

```
┌─────────────────────────────────────────────────────────────┐
│                      IDLE MODE                               │
│              Interval: 1 minute                              │
│              Battery: Low                                    │
│              Use: When stationary or slow movement           │
└─────────────────────────────────────────────────────────────┘
         │                                    ↑
         │ Movement > 50m detected            │ No movement > 50m
         ↓                                    │ for 2 minutes
┌─────────────────────────────────────────────────────────────┐
│                     ACTIVE MODE                              │
│              Interval: 10 seconds                            │
│              Battery: Higher                                 │
│              Use: When actively moving                       │
└─────────────────────────────────────────────────────────────┘
```

### Configuration

```kotlin
// Constants for adaptive tracking
const val IDLE_INTERVAL_MS = 60 * 1000L         // 1 minute when idle
const val ACTIVE_INTERVAL_MS = 10 * 1000L       // 10 seconds when moving
const val MOVEMENT_THRESHOLD_METERS = 50f       // Threshold to switch modes
const val IDLE_TIMEOUT_MS = 2 * 60 * 1000L      // Return to idle after 2 min no movement
const val MIN_DISTANCE_METERS = 10f             // Minimum distance to save (avoid duplicates)
```

### State Machine

| Current State | Condition | Action | New State |
|---------------|-----------|--------|-----------|
| IDLE | Distance from last location > 50m | Switch to 10s interval | ACTIVE |
| ACTIVE | Continuous movement detected | Keep 10s interval | ACTIVE |
| ACTIVE | No >50m movement for 2 min | Switch to 1min interval | IDLE |

### Battery Impact Comparison

| Scenario | Fixed 10s | Fixed 1min | Adaptive |
|----------|-----------|------------|----------|
| Sitting 8 hours | 2880 polls | 480 polls | 480 polls (IDLE) |
| Walking 1 hour | 360 polls | 60 polls | 360 polls (ACTIVE) |
| Mixed day (2h move, 6h still) | 2880 polls | 480 polls | 720 + 360 = 1080 polls |

**Adaptive gives real-time tracking when moving, but conserves battery when stationary.**

### Implementation Requirements

#### 1. New Constants (`Constants.kt`)

```kotlin
// Adaptive tracking configuration
const val IDLE_INTERVAL_MS = 60 * 1000L
const val ACTIVE_INTERVAL_MS = 10 * 1000L
const val MOVEMENT_THRESHOLD_METERS = 50f
const val IDLE_TIMEOUT_MS = 2 * 60 * 1000L
const val MIN_SAVE_DISTANCE_METERS = 10f
```

#### 2. Modify FusedLocationDataSource

Add method to dynamically update the location request:

```kotlin
suspend fun updateTrackingInterval(intervalMillis: Long) {
    // Stop current tracking
    stopTracking()
    // Restart with new interval
    startTracking(intervalMillis, currentMinDistance)
}
```

#### 3. New AdaptiveTrackingManager

```kotlin
class AdaptiveTrackingManager(
    private val locationDataSource: FusedLocationDataSource
) {
    enum class TrackingState { IDLE, ACTIVE }

    private var currentState = TrackingState.IDLE
    private var lastSignificantLocation: Location? = null
    private var lastMovementTime: Long = 0

    fun onLocationReceived(location: Location) {
        val distance = lastSignificantLocation?.distanceTo(location) ?: 0f

        when (currentState) {
            IDLE -> {
                if (distance > MOVEMENT_THRESHOLD_METERS) {
                    switchToActiveMode()
                }
            }
            ACTIVE -> {
                if (distance > MOVEMENT_THRESHOLD_METERS) {
                    lastMovementTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - lastMovementTime > IDLE_TIMEOUT_MS) {
                    switchToIdleMode()
                }
            }
        }
    }
}
```

#### 4. Update LocationTrackingService

Integrate AdaptiveTrackingManager into the service's location collection flow.

---

## Comparison Summary

| Configuration | Battery/Hour | Accuracy | Complexity | Best For |
|---------------|-------------|----------|------------|----------|
| Current (5min/100m) | ~2-3% | Low | Simple | Background logging |
| Fixed 1min/50m | ~5-8% | Medium | Simple | Regular tracking |
| Fixed 10s/50m | ~20-30% | High | Simple | Short-term precise tracking |
| **Adaptive** | ~5-10% avg | High when moving | Medium | **Daily use (recommended)** |

---

## Recommendation

**For daily personal location tracking:** Implement **Adaptive Tracking**

- Provides real-time accuracy when you're actually moving
- Conserves battery when stationary (work, home, sleeping)
- Average battery impact similar to fixed 1-minute tracking
- Better data quality for route reconstruction

---

## Files to Modify

| File | Changes |
|------|---------|
| `Constants.kt` | Add adaptive tracking constants |
| `FusedLocationDataSource.kt` | Add `updateTrackingInterval()` method |
| `LocationTrackingService.kt` | Integrate adaptive manager |
| **New:** `AdaptiveTrackingManager.kt` | State machine for mode switching |

---

## Testing Checklist

After implementation:

1. [ ] Verify IDLE mode uses 1-minute interval (check logs)
2. [ ] Walk >50m and verify switch to ACTIVE mode (10s interval)
3. [ ] Stop moving for 2+ minutes and verify return to IDLE
4. [ ] Monitor battery usage over a full day
5. [ ] Verify database entries reflect expected frequency
6. [ ] Test behavior when app is in background
7. [ ] Test behavior after device restart
