package com.wifield.app.data.repository

import com.wifield.app.data.local.dao.AccessPointDao
import com.wifield.app.data.local.dao.ActiveTestResultDao
import com.wifield.app.data.local.dao.SnapshotDao
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import kotlinx.coroutines.flow.Flow

class SnapshotRepository(
    private val snapshotDao: SnapshotDao,
    private val accessPointDao: AccessPointDao,
    private val activeTestResultDao: ActiveTestResultDao
) {
    fun getSnapshotsByProject(projectId: Long): Flow<List<SnapshotEntity>> =
        snapshotDao.getSnapshotsByProject(projectId)

    suspend fun getSnapshotById(id: Long): SnapshotEntity? =
        snapshotDao.getSnapshotById(id)

    fun getAllSnapshots(): Flow<List<SnapshotEntity>> =
        snapshotDao.getAllSnapshots()

    suspend fun createSnapshot(
        snapshot: SnapshotEntity,
        accessPoints: List<AccessPointEntity>,
        activeTestResult: ActiveTestResultEntity? = null
    ): Long {
        val snapshotId = snapshotDao.insert(snapshot)
        val apsWithSnapshotId = accessPoints.map { it.copy(snapshotId = snapshotId) }
        accessPointDao.insertAll(apsWithSnapshotId)
        activeTestResult?.let {
            activeTestResultDao.insert(it.copy(snapshotId = snapshotId))
        }
        return snapshotId
    }

    suspend fun updateSnapshot(snapshot: SnapshotEntity) =
        snapshotDao.update(snapshot)

    suspend fun deleteSnapshot(id: Long) =
        snapshotDao.deleteById(id)

    fun getAccessPointsBySnapshot(snapshotId: Long): Flow<List<AccessPointEntity>> =
        accessPointDao.getAccessPointsBySnapshot(snapshotId)

    suspend fun getAccessPointsBySnapshotOnce(snapshotId: Long): List<AccessPointEntity> =
        accessPointDao.getAccessPointsBySnapshotOnce(snapshotId)

    fun getActiveTestResult(snapshotId: Long): Flow<ActiveTestResultEntity?> =
        activeTestResultDao.getResultBySnapshot(snapshotId)

    suspend fun getActiveTestResultOnce(snapshotId: Long): ActiveTestResultEntity? =
        activeTestResultDao.getResultBySnapshotOnce(snapshotId)
}
