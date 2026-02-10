package com.wifield.app.ui.screens.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ProjectEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import com.wifield.app.data.repository.ProjectRepository
import com.wifield.app.data.repository.SnapshotRepository
import com.wifield.app.domain.analyzer.AlertAnalyzer
import com.wifield.app.domain.model.Alert
import com.wifield.app.domain.model.ChannelInfo
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.SsidGroup
import com.wifield.app.domain.model.WifiBand
import com.wifield.app.service.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScannerTab { LIST, GROUPS, CHANNELS }

data class ScannerUiState(
    val accessPoints: List<ScannedAccessPoint> = emptyList(),
    val ssidGroups: List<SsidGroup> = emptyList(),
    val channelInfo24: List<ChannelInfo> = emptyList(),
    val channelInfo5: List<ChannelInfo> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val isScanning: Boolean = false,
    val lastScanTime: Long = 0,
    val scanCount: Int = 0,
    val autoRefresh: Boolean = true,
    val selectedTab: ScannerTab = ScannerTab.LIST,
    val showSnapshotDialog: Boolean = false,
    val showProjectPicker: Boolean = false,
    val selectedProjectId: Long? = null,
    val projects: List<ProjectEntity> = emptyList(),
    val wifiEnabled: Boolean = true,
    val snapshotSaved: Boolean = false
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(db.projectDao())
    private val snapshotRepository = SnapshotRepository(
        db.snapshotDao(), db.accessPointDao(), db.activeTestResultDao()
    )
    val wifiScanner = WifiScanner(application)

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var scanCollectionJob: Job? = null

    init {
        loadProjects()
        startScanCollection()
        startAutoRefresh()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(projects = projects)
            }
        }
    }

    private fun startScanCollection() {
        scanCollectionJob?.cancel()
        scanCollectionJob = viewModelScope.launch {
            wifiScanner.scanAccessPoints().collect { results ->
                updateResults(results)
            }
        }
    }

    private fun updateResults(results: List<ScannedAccessPoint>) {
        val groups = results.groupBy { it.ssid }
            .map { (ssid, aps) -> SsidGroup(ssid = ssid, accessPoints = aps) }
            .sortedByDescending { it.bestRssi }

        val channels24 = results.filter { it.band == WifiBand.BAND_2_4_GHZ }
            .groupBy { it.channel }
            .map { (ch, aps) -> ChannelInfo(ch, WifiBand.BAND_2_4_GHZ, aps.size, aps) }
            .sortedBy { it.channel }

        val channels5 = results.filter { it.band == WifiBand.BAND_5_GHZ }
            .groupBy { it.channel }
            .map { (ch, aps) -> ChannelInfo(ch, WifiBand.BAND_5_GHZ, aps.size, aps) }
            .sortedBy { it.channel }

        val alerts = AlertAnalyzer.analyze(results)

        _uiState.value = _uiState.value.copy(
            accessPoints = results,
            ssidGroups = groups,
            channelInfo24 = channels24,
            channelInfo5 = channels5,
            alerts = alerts,
            isScanning = false,
            lastScanTime = System.currentTimeMillis(),
            wifiEnabled = wifiScanner.isWifiEnabled
        )
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(32000) // Respect throttling: ~2 scans per minute
                if (_uiState.value.autoRefresh) {
                    triggerScan()
                }
            }
        }
    }

    fun triggerScan() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        wifiScanner.triggerScan()
        _uiState.value = _uiState.value.copy(
            scanCount = _uiState.value.scanCount + 1
        )
    }

    fun setAutoRefresh(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoRefresh = enabled)
    }

    fun selectTab(tab: ScannerTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun showSnapshotDialog() {
        _uiState.value = _uiState.value.copy(showSnapshotDialog = true)
    }

    fun hideSnapshotDialog() {
        _uiState.value = _uiState.value.copy(showSnapshotDialog = false)
    }

    fun showProjectPicker() {
        _uiState.value = _uiState.value.copy(showProjectPicker = true, selectedProjectId = null)
    }

    fun hideProjectPicker() {
        _uiState.value = _uiState.value.copy(showProjectPicker = false)
    }

    fun selectProject(projectId: Long) {
        _uiState.value = _uiState.value.copy(selectedProjectId = projectId)
    }

    fun saveSnapshot(label: String, projectId: Long) {
        viewModelScope.launch {
            val snapshot = SnapshotEntity(
                projectId = projectId,
                label = label,
                isActiveMode = false
            )
            val accessPointEntities = _uiState.value.accessPoints.map { ap ->
                AccessPointEntity(
                    snapshotId = 0, // will be set by repository
                    ssid = ap.ssid,
                    bssid = ap.bssid,
                    rssi = ap.rssi,
                    channel = ap.channel,
                    frequency = ap.frequency,
                    band = ap.band.label,
                    channelWidth = ap.channelWidth,
                    security = ap.security,
                    wpsEnabled = ap.wpsEnabled,
                    isConnected = ap.isConnected
                )
            }
            snapshotRepository.createSnapshot(snapshot, accessPointEntities)
            _uiState.value = _uiState.value.copy(
                showSnapshotDialog = false,
                showProjectPicker = false,
                snapshotSaved = true
            )
            delay(2000)
            _uiState.value = _uiState.value.copy(snapshotSaved = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        scanCollectionJob?.cancel()
    }
}
