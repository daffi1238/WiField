package com.wifield.app.data.local.dao

import androidx.room.*
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveTestResultDao {
    @Query("SELECT * FROM active_test_results WHERE snapshotId = :snapshotId")
    fun getResultBySnapshot(snapshotId: Long): Flow<ActiveTestResultEntity?>

    @Query("SELECT * FROM active_test_results WHERE snapshotId = :snapshotId")
    suspend fun getResultBySnapshotOnce(snapshotId: Long): ActiveTestResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ActiveTestResultEntity): Long

    @Query("DELETE FROM active_test_results WHERE snapshotId = :snapshotId")
    suspend fun deleteBySnapshot(snapshotId: Long): Int
}
