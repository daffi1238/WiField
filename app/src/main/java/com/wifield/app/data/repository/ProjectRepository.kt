package com.wifield.app.data.repository

import com.wifield.app.data.local.dao.ProjectDao
import com.wifield.app.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    fun getAllProjects(): Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun insert(project: ProjectEntity): Long = projectDao.insert(project)

    suspend fun update(project: ProjectEntity) = projectDao.update(project)

    suspend fun delete(project: ProjectEntity) = projectDao.delete(project)

    suspend fun deleteById(id: Long) = projectDao.deleteById(id)
}
