package com.wifield.app.ui.screens.comparator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import com.wifield.app.data.repository.SnapshotRepository
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.WifiBand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SnapshotComparison(
    val snapshot: SnapshotEntity,
    val accessPoints: List<ScannedAccessPoint>,
    val activeTestResult: ActiveTestResultEntity?,
    val bestRssi: Int
)

data class ComparatorUiState(
    val availableSnapshots: List<SnapshotEntity> = emptyList(),
    val selectedSnapshots: List<SnapshotComparison> = emptyList(),
    val isLoading: Boolean = true,
    val showPicker: Boolean = false
)

class ComparatorViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val snapshotRepository = SnapshotRepository(
        db.snapshotDao(), db.accessPointDao(), db.activeTestResultDao()
    )

    private val _uiState = MutableStateFlow(ComparatorUiState())
    val uiState: StateFlow<ComparatorUiState> = _uiState.asStateFlow()

    fun loadSnapshots(projectId: Long) {
        viewModelScope.launch {
            snapshotRepository.getSnapshotsByProject(projectId).collect { snapshots ->
                _uiState.value = _uiState.value.copy(
                    availableSnapshots = snapshots,
                    isLoading = false
                )
            }
        }
    }

    fun toggleSnapshot(snapshot: SnapshotEntity) {
        viewModelScope.launch {
            val current = _uiState.value.selectedSnapshots
            val existing = current.find { it.snapshot.id == snapshot.id }

            if (existing != null) {
                _uiState.value = _uiState.value.copy(
                    selectedSnapshots = current - existing
                )
            } else {
                val aps = snapshotRepository.getAccessPointsBySnapshotOnce(snapshot.id)
                val activeResult = snapshotRepository.getActiveTestResultOnce(snapshot.id)
                val scannedAps = aps.map { it.toScannedAccessPoint() }

                val comparison = SnapshotComparison(
                    snapshot = snapshot,
                    accessPoints = scannedAps,
                    activeTestResult = activeResult,
                    bestRssi = aps.maxOfOrNull { it.rssi } ?: -100
                )

                _uiState.value = _uiState.value.copy(
                    selectedSnapshots = current + comparison
                )
            }
        }
    }

    fun showPicker() {
        _uiState.value = _uiState.value.copy(showPicker = true)
    }

    fun hidePicker() {
        _uiState.value = _uiState.value.copy(showPicker = false)
    }

    private fun AccessPointEntity.toScannedAccessPoint() = ScannedAccessPoint(
        ssid = ssid,
        bssid = bssid,
        rssi = rssi,
        channel = channel,
        frequency = frequency,
        band = WifiBand.fromFrequency(frequency),
        channelWidth = channelWidth,
        security = security,
        wpsEnabled = wpsEnabled,
        isConnected = isConnected
    )
}
