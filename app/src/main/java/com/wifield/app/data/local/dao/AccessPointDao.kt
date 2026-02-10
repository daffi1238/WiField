package com.wifield.app.data.local.dao

import androidx.room.*
import com.wifield.app.data.local.entity.AccessPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccessPointDao {
    @Query("SELECT * FROM access_points WHERE snapshotId = :snapshotId ORDER BY rssi DESC")
    fun getAccessPointsBySnapshot(snapshotId: Long): Flow<List<AccessPointEntity>>

    @Query("SELECT * FROM access_points WHERE snapshotId = :snapshotId ORDER BY rssi DESC")
    suspend fun getAccessPointsBySnapshotOnce(snapshotId: Long): List<AccessPointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accessPoints: List<AccessPointEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accessPoint: AccessPointEntity): Long

    @Query("DELETE FROM access_points WHERE snapshotId = :snapshotId")
    suspend fun deleteBySnapshot(snapshotId: Long): Int
}
