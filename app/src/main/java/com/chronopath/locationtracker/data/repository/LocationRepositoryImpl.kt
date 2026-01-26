package com.chronopath.locationtracker.data.repository

import com.chronopath.locationtracker.core.common.Result
import com.chronopath.locationtracker.data.local.LocationDatabase
import com.chronopath.locationtracker.data.mapper.LocationMapper
import com.chronopath.locationtracker.data.source.aggregator.DataAggregator
import com.chronopath.locationtracker.domain.model.Location
import com.chronopath.locationtracker.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

class LocationRepositoryImpl(
    private val database: LocationDatabase,
    private val aggregator: DataAggregator
) : LocationRepository {

    private val locationDao = database.locationDao()

    override fun getAllLocations(): Flow<List<Location>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { LocationMapper.mapToDomain(it) }
        }
    }

    override suspend fun insertLocation(location: Location) {
        Timber.tag("DB").d("insertLocation - lat: %.6f, lon: %.6f, timestamp: %s".format(
            location.latitude, location.longitude, location.timestamp
        ))
        locationDao.insertLocation(LocationMapper.mapToEntity(location))
    }

    override fun getTrackedLocation(): Flow<Location> {
        return aggregator.getAggregatedLocations()
    }

    override suspend fun saveLocation(location: Location): Result<Boolean> {
        return try {
            insertLocation(location)
            Timber.tag("DB").i("saveLocation - Location saved successfully")
            Result.Success(true)
        } catch (e: Exception) {
            Timber.tag("DB").e(e, "saveLocation - Failed to save location")
            Result.Error(e)
        }
    }

    override fun getLocationCount(): Flow<Int> {
        return locationDao.getLocationCount()
            .map { count ->
                Timber.tag("DB").d("getLocationCount - Total locations: $count")
                count
            }
            .catch { e ->
                Timber.tag("DB").e(e, "getLocationCount - Failed to get location count")
                emit(0)
            }
    }
}