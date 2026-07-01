package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.*

class PotholeRepository(private val potholeDao: PotholeDao) {
    val allPotholes: Flow<List<PotholeEntity>> = potholeDao.getAllPotholes()

    suspend fun getPotholeById(id: String): PotholeEntity? {
        return potholeDao.getPotholeById(id)
    }

    suspend fun updateRepairStatus(id: String, status: String) {
        potholeDao.updateRepairStatus(id, status)
    }

    /**
     * Inserts a new pothole, or merges it with an existing one if it is a duplicate.
     * Implements Requirement 9 & 10.
     * Returns a Pair: the final entity saved/updated, and a Boolean indicating if it was a duplicate (merged).
     */
    suspend fun insertOrMergePothole(newPothole: PotholeEntity): Pair<PotholeEntity, Boolean> {
        // Fetch current list of potholes to scan for duplicates
        val currentPotholes = allPotholes.first()

        for (existing in currentPotholes) {
            val distance = calculateDistance(
                newPothole.latitude, newPothole.longitude,
                existing.latitude, existing.longitude
            )

            // 1. Proximity check (e.g., within 15 meters)
            val isNearby = distance <= 15.0

            // 2. Dimension similarity check (width, length, depth similarity within 30%)
            val widthDiff = abs(newPothole.widthCm - existing.widthCm) / max(existing.widthCm, 1.0)
            val lengthDiff = abs(newPothole.lengthCm - existing.lengthCm) / max(existing.lengthCm, 1.0)
            val depthDiff = abs(newPothole.depthCm - existing.depthCm) / max(existing.depthCm, 1.0)
            val isDimensionSimilar = widthDiff <= 0.30 && lengthDiff <= 0.30 && depthDiff <= 0.30

            // 3. Time interval check (if seen very recently at nearly the same spot)
            val timeDiffSec = abs(newPothole.timestamp - existing.lastSeen) / 1000L
            val isRecent = timeDiffSec <= 15 // within 15 seconds

            // Duplicate condition: Either extremely close (< 5m) OR nearby (< 15m) with similar dimensions / high frequency
            if (distance <= 5.0 || (isNearby && (isDimensionSimilar || isRecent))) {
                // Merge duplicate: Update "last seen" and average the dimensions to get progressive accuracy
                val mergedPothole = existing.copy(
                    lastSeen = newPothole.timestamp,
                    timestamp = min(existing.timestamp, newPothole.timestamp), // keep original first-seen
                    widthCm = ((existing.widthCm + newPothole.widthCm) / 2.0).roundTo(1),
                    lengthCm = ((existing.lengthCm + newPothole.lengthCm) / 2.0).roundTo(1),
                    depthCm = ((existing.depthCm + newPothole.depthCm) / 2.0).roundTo(1),
                    areaSqM = ((existing.areaSqM + newPothole.areaSqM) / 2.0).roundTo(3),
                    volumeCuM = ((existing.volumeCuM + newPothole.volumeCuM) / 2.0).roundTo(4),
                    confidence = max(existing.confidence, newPothole.confidence),
                    isSynced = false // Mark unsynced since it was modified
                )
                potholeDao.insertPothole(mergedPothole)
                return Pair(mergedPothole, true)
            }
        }

        // No duplicate found: insert as new
        potholeDao.insertPothole(newPothole)
        return Pair(newPothole, false)
    }

    suspend fun getUnsyncedPotholes(): List<PotholeEntity> {
        return potholeDao.getUnsyncedPotholes()
    }

    suspend fun markAsSynced(ids: List<String>) {
        potholeDao.markAsSynced(ids)
    }

    suspend fun deletePothole(pothole: PotholeEntity) {
        potholeDao.deletePothole(pothole)
    }

    suspend fun clearAll() {
        potholeDao.deleteAll()
    }

    // Haversine Distance Formula
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    // Extension helper to round Double values
    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }
}
