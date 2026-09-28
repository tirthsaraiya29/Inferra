package com.inferra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY downloadsCount DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("DELETE FROM models")
    suspend fun clearAll()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAtEpochMs DESC")
    fun getWatchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE modelId = :modelId)")
    fun isWatchlisted(modelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addWatchlist(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE modelId = :modelId")
    suspend fun removeWatchlist(modelId: String)
}

@Dao
interface DownloadJobDao {
    @Query("SELECT * FROM download_jobs ORDER BY id DESC")
    fun getAllJobs(): Flow<List<DownloadJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: DownloadJobEntity)

    @Query("DELETE FROM download_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)
}

@Dao
interface HardwareProfileDao {
    @Query("SELECT * FROM hardware_profiles")
    fun getAllProfiles(): Flow<List<HardwareProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: HardwareProfileEntity)

    @Query("DELETE FROM hardware_profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}

@Dao
interface DeviceTargetDao {
    @Query("SELECT * FROM device_targets")
    fun getAllDevices(): Flow<List<DeviceTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceTargetEntity)
}
