package com.chronopath.locationtracker.data.repository

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.mapper.LocationMapper
import com.chronopath.locationtracker.data.source.aggregator.DataAggregator
import com.chronopath.locationtracker.data.source.id.DeviceIdManager
import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocationRepositoryImpl(
    private val database: LocationDatabase,
    private val aggregator: DataAggregator,
    private val deviceIdManager: DeviceIdManager
) : LocationRepository {

    private val locationDao = database.locationDao()

    override fun getAllLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { LocationMapper.mapToDomain(it) }
        }
    }

    override suspend fun insertLocation(location: Location) {
        locationDao.insertLocation(LocationMapper.mapToEntity(location))
    }

    override fun getTrackedLocation(): Flow<Location> {
        return aggregator.getAggregatedLocations()
    }

    override suspend fun saveLocation(location: Location): Result<Boolean> {
        return try {
            insertLocation(location)
            Result.Success(true)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getLocationCount(): Flow<Int> {
        return kotlinx.coroutines.flow.flow {
            emit(locationDao.getLocationCount())
        }
    }
}