package com.chronopath.locationtracker.domain.usecase

import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for retrieving all stored locations.
 * Returns a Flow that emits new lists when data changes.
 */
class GetLocationsUseCase(
    private val repository: LocationRepository
) : UseCase<Unit, Flow<List<Location>>> {
    override suspend operator fun invoke(parameters: Unit): Flow<List<Location>> {
        return repository.getAllLocations()
    }
}