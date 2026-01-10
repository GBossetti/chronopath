package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting the count of stored locations.
 * More efficient than loading all data for UI statistics.
 */
class GetLocationCountUseCase(
    private val repository: LocationRepository
) : UseCase<Unit, Flow<Int>> {
    override suspend operator fun invoke(parameters: Unit): Flow<Int> {
        return repository.getLocationCount()
    }
}