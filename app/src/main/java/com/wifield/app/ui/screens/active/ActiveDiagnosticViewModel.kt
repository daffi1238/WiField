package com.wifield.app.ui.screens.active

import android.app.Application
import android.net.DhcpInfo
import android.net.wifi.WifiManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wifield.app.data.local.WiFieldDatabase
import com.wifield.app.data.local.entity.AccessPointEntity
import com.wifield.app.data.local.entity.ActiveTestResultEntity
import com.wifield.app.data.local.entity.ProjectEntity
import com.wifield.app.data.local.entity.SnapshotEntity
import com.wifield.app.data.repository.ProjectRepository
import com.wifield.app.data.repository.SnapshotRepository
import com.wifield.app.domain.model.ActiveTestResults
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.service.ConnectionInfo
import com.wifield.app.service.PingService
import com.wifield.app.service.SpeedTestPhase
import com.wifield.app.service.SpeedTestService
import com.wifield.app.service.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveDiagUiState(
    val isConnected: Boolean = false,
    val connectionInfo: ConnectionInfo? = null,
    val accessPoints: List<ScannedAccessPoint> = emptyList(),
    val testResults: ActiveTestResults = ActiveTestResults(),
    val isRunningSpeedTest: Boolean = false,
    val speedTestPhase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val speedTestProgress: Int = 0,
    val currentSpeed: Double = 0.0,
    val isRunningPing: Boolean = false,
    val pingProgress: String = "",
    val rssiHistory: List<Int> = emptyList(),
    val latencyHistory: List<Double> = emptyList(),
    val monitoring: Boolean = false,
    val projects: List<ProjectEntity> = emptyList(),
    val showProjectPicker: Boolean = false,
    val showSnapshotDialog: Boolean = false,
    val selectedProjectId: Long? = null,
    val snapshotSaved: Boolean = false
)

class ActiveDiagnosticViewModel(application: Application) : AndroidViewModel(application) {
    private val db = WiFieldDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(db.projectDao())
    private val snapshotRepository = SnapshotRepository(
        db.snapshotDao(), db.accessPointDao(), db.activeTestResultDao()
    )
    private val wifiScanner = WifiScanner(application)
    private val wifiManager = application.getSystemService(android.content.Context.WIFI_SERVICE) as WifiManager

    private val _uiState = MutableStateFlow(ActiveDiagUiState())
    val uiState: StateFlow<ActiveDiagUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null

    init {
        loadProjects()
        refreshConnection()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(projects = projects)
            }
        }
    }

    fun refreshConnection() {
        viewModelScope.launch {
            val connInfo = wifiScanner.getConnectionInfo()
            val aps = wifiScanner.getCurrentResults()
            _uiState.value = _uiState.value.copy(
                isConnected = connInfo != null,
                connectionInfo = connInfo,
                accessPoints = aps
            )
        }
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningSpeedTest = true)
            SpeedTestService.runSpeedTest().collect { progress ->
                _uiState.value = _uiState.value.copy(
                    speedTestPhase = progress.phase,
                    speedTestProgress = progress.progressPercent,
                    currentSpeed = progress.currentSpeed,
                    testResults = when (progress.phase) {
                        SpeedTestPhase.COMPLETE -> _uiState.value.testResults
                        SpeedTestPhase.DOWNLOAD -> _uiState.value.testResults.copy(downloadSpeed = progress.currentSpeed)
                        SpeedTestPhase.UPLOAD -> _uiState.value.testResults.copy(uploadSpeed = progress.currentSpeed)
                        else -> _uiState.value.testResults
                    },
                    isRunningSpeedTest = progress.phase != SpeedTestPhase.COMPLETE && progress.phase != SpeedTestPhase.ERROR
                )
            }
        }
    }

    fun runPingTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningPing = true, pingProgress = "Gateway...")

            // Ping gateway with progress
            val gatewayIp = getGatewayIp()
            var gatewayLatency = 0.0
            if (gatewayIp != null) {
                PingService.pingWithProgress(gatewayIp, count = 10).collect { progress ->
                    gatewayLatency = progress.averageLatency
                    _uiState.value = _uiState.value.copy(
                        pingProgress = "Gateway: ${progress.sent}/10 (${String.format("%.0f", progress.averageLatency)} ms)"
                    )
                }
            }

            // Ping external with progress
            _uiState.value = _uiState.value.copy(pingProgress = "Internet...")
            var extLatency = 0.0
            var extJitter = 0.0
            var extPacketLoss = 0.0
            PingService.pingWithProgress("8.8.8.8", count = 20).collect { progress ->
                extLatency = progress.averageLatency
                _uiState.value = _uiState.value.copy(
                    pingProgress = "Internet: ${progress.sent}/20 (${String.format("%.0f", progress.averageLatency)} ms)"
                )
            }

            // Get final stats from a quick dedicated measurement
            val externalResult = PingService.ping("8.8.8.8", count = 5, timeoutMs = 2000)
            if (externalResult.latency > 0) {
                extLatency = externalResult.latency
                extJitter = externalResult.jitter
                extPacketLoss = externalResult.packetLoss
            }

            val connInfo = wifiScanner.getConnectionInfo()

            _uiState.value = _uiState.value.copy(
                isRunningPing = false,
                pingProgress = "",
                testResults = _uiState.value.testResults.copy(
                    latency = extLatency,
                    jitter = extJitter,
                    packetLoss = extPacketLoss,
                    gatewayLatency = gatewayLatency,
                    linkSpeed = connInfo?.linkSpeed ?: 0
                )
            )
        }
    }

    fun toggleMonitoring() {
        if (_uiState.value.monitoring) {
            stopMonitoring()
        } else {
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        _uiState.value = _uiState.value.copy(monitoring = true, rssiHistory = emptyList(), latencyHistory = emptyList())
        monitoringJob = viewModelScope.launch {
            while (true) {
                val connInfo = wifiScanner.getConnectionInfo()
                if (connInfo != null) {
                    val rssiList = _uiState.value.rssiHistory.takeLast(59) + connInfo.rssi
                    val pingResult = PingService.ping("8.8.8.8", count = 3, timeoutMs = 1000)
                    val latencyList = _uiState.value.latencyHistory.takeLast(59) + pingResult.latency

                    _uiState.value = _uiState.value.copy(
                        connectionInfo = connInfo,
                        rssiHistory = rssiList,
                        latencyHistory = latencyList
                    )
                }
                delay(2000)
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        _uiState.value = _uiState.value.copy(monitoring = false)
    }

    @Suppress("DEPRECATION")
    private fun getGatewayIp(): String? {
        val dhcpInfo: DhcpInfo = wifiManager.dhcpInfo ?: return null
        val gateway = dhcpInfo.gateway
        if (gateway == 0) return null
        return "${gateway and 0xFF}.${gateway shr 8 and 0xFF}.${gateway shr 16 and 0xFF}.${gateway shr 24 and 0xFF}"
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

    fun showSnapshotDialog() {
        _uiState.value = _uiState.value.copy(showSnapshotDialog = true)
    }

    fun hideSnapshotDialog() {
        _uiState.value = _uiState.value.copy(showSnapshotDialog = false)
    }

    fun saveSnapshot(label: String, projectId: Long) {
        viewModelScope.launch {
            val snapshot = SnapshotEntity(
                projectId = projectId,
                label = label,
                isActiveMode = true
            )
            val accessPointEntities = _uiState.value.accessPoints.map { ap ->
                AccessPointEntity(
                    snapshotId = 0,
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
            val activeResult = ActiveTestResultEntity(
                snapshotId = 0,
                downloadSpeed = _uiState.value.testResults.downloadSpeed,
                uploadSpeed = _uiState.value.testResults.uploadSpeed,
                latency = _uiState.value.testResults.latency,
                jitter = _uiState.value.testResults.jitter,
                packetLoss = _uiState.value.testResults.packetLoss,
                linkSpeed = _uiState.value.testResults.linkSpeed,
                gatewayLatency = _uiState.value.testResults.gatewayLatency
            )
            snapshotRepository.createSnapshot(snapshot, accessPointEntities, activeResult)
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
        monitoringJob?.cancel()
    }
}
