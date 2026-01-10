package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.domain.repository.LocationRepository

/**
 * Use case for starting the location tracking service.
 * Will coordinate permissions, service startup, and initial configuration.
 */
class StartTrackingUseCase(
    private val repository: LocationRepository
) : UseCase<Unit, Result<Boolean>> {
    override suspend operator fun invoke(parameters: Unit): Result<Boolean> {
        // TODO: Implement with service coordination
        return Result.Success(true)
    }
}