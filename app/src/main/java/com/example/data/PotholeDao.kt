package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PotholeDao {
    @Query("SELECT * FROM potholes ORDER BY timestamp DESC")
    fun getAllPotholes(): Flow<List<PotholeEntity>>

    @Query("SELECT * FROM potholes WHERE id = :id")
    suspend fun getPotholeById(id: String): PotholeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPothole(pothole: PotholeEntity)

    @Update
    suspend fun updatePothole(pothole: PotholeEntity)

    @Query("UPDATE potholes SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("SELECT * FROM potholes WHERE isSynced = 0")
    suspend fun getUnsyncedPotholes(): List<PotholeEntity>

    @Query("UPDATE potholes SET repairStatus = :status WHERE id = :id")
    suspend fun updateRepairStatus(id: String, status: String)

    @Delete
    suspend fun deletePothole(pothole: PotholeEntity)

    @Query("DELETE FROM potholes")
    suspend fun deleteAll()
}
