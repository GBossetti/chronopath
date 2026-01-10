package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.domain.controller.TrackingController

/**
 * Use case for starting the location tracking service.
 * Delegates to TrackingController abstraction (Dependency Inversion Principle).
 */
class StartTrackingUseCase(
    private val controller: TrackingController
) : UseCase<Unit, Result<Boolean>> {
    override suspend operator fun invoke(parameters: Unit): Result<Boolean> {
        return controller.startTracking()
    }
}
