package com.wifield.app.data.local.dao

import androidx.room.*
import com.wifield.app.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getSnapshotsByProject(projectId: Long): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE id = :id")
    suspend fun getSnapshotById(id: Long): SnapshotEntity?

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: SnapshotEntity): Long

    @Update
    suspend fun update(snapshot: SnapshotEntity): Int

    @Delete
    suspend fun delete(snapshot: SnapshotEntity): Int

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
