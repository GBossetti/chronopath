package com.chronopath.locationtracker.domain.usecase

/**
 * Base interface for all use cases in the application.
 * Follows the Command Pattern.
 *
 * @param P Type of input parameters
 * @param R Type of return value
 */
interface UseCase<in P, out R> {
    suspend operator fun invoke(parameters: P): R
}