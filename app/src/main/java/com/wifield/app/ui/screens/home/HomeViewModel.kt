package com.wifield.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.ProjectEntity
import com.wifield.app.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingProject: ProjectEntity? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val repository = ProjectRepository(db.projectDao())

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(
                    projects = projects,
                    isLoading = false
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun hideCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun showEditDialog(project: ProjectEntity) {
        _uiState.value = _uiState.value.copy(showEditDialog = true, editingProject = project)
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(showEditDialog = false, editingProject = null)
    }

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            repository.insert(ProjectEntity(name = name, description = description))
            _uiState.value = _uiState.value.copy(showCreateDialog = false)
        }
    }

    fun updateProject(project: ProjectEntity, name: String, description: String) {
        viewModelScope.launch {
            repository.update(project.copy(name = name, description = description))
            _uiState.value = _uiState.value.copy(showEditDialog = false, editingProject = null)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.delete(project)
        }
    }
}
