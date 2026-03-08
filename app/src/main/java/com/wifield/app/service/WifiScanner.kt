package com.wifield.app.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.wifield.app.domain.model.ScannedAccessPoint
import com.wifield.app.domain.model.WifiBand
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "WifiScanner"
private const val PLACEHOLDER_BSSID = "02:00:00:00:00:00"

class WifiScanner(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    val isWifiEnabled: Boolean get() = wifiManager.isWifiEnabled

    fun scanAccessPoints(): Flow<List<ScannedAccessPoint>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val results = parseScanResults()
                    trySend(results)
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        // Send current results immediately
        trySend(parseScanResults())

        // Trigger a scan
        @Suppress("DEPRECATION")
        wifiManager.startScan()

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun triggerScan(): Boolean {
        @Suppress("DEPRECATION")
        return wifiManager.startScan()
    }

    fun getCurrentResults(): List<ScannedAccessPoint> = parseScanResults()

    @Suppress("DEPRECATION")
    private fun parseScanResults(): List<ScannedAccessPoint> {
        if (!hasPermissions()) return emptyList()

        val connectedBssid = getConnectedBssid()

        @Suppress("MissingPermission")
        val results = wifiManager.scanResults ?: return emptyList()

        return results.map { result ->
            val frequency = result.frequency
            val band = WifiBand.fromFrequency(frequency)
            val channel = frequencyToChannel(frequency)
            val channelWidth = getChannelWidth(result)
            val security = getSecurityType(result)
            val wpsEnabled = result.capabilities.contains("[WPS]")

            ScannedAccessPoint(
                ssid = result.SSID.orEmpty(),
                bssid = result.BSSID.orEmpty(),
                rssi = result.level,
                channel = channel,
                frequency = frequency,
                band = band,
                channelWidth = channelWidth,
                security = security,
                wpsEnabled = wpsEnabled,
                isConnected = result.BSSID == connectedBssid
            )
        }.sortedByDescending { it.rssi }
    }

    fun getConnectedBssid(): String? {
        return getWifiInfo()?.let { info ->
            val bssid = info.bssid
            if (isValidBssid(bssid)) bssid else null
        }
    }

    fun getConnectionInfo(): ConnectionInfo? {
        val wifiInfo = getWifiInfo() ?: return null
        val bssid = wifiInfo.bssid
        if (!isValidBssid(bssid)) return null

        @Suppress("DEPRECATION")
        return ConnectionInfo(
            ssid = wifiInfo.ssid?.removePrefix("\"")?.removeSuffix("\"").orEmpty(),
            bssid = bssid,
            rssi = wifiInfo.rssi,
            linkSpeed = wifiInfo.linkSpeed,
            frequency = wifiInfo.frequency
        )
    }

    /**
     * Gets WifiInfo using multiple strategies for compatibility across Android versions.
     * Strategy 1: ConnectivityManager (Android 12+, preferred)
     * Strategy 2: WifiManager.connectionInfo (deprecated but works on all versions)
     */
    @Suppress("DEPRECATION", "MissingPermission")
    private fun getWifiInfo(): WifiInfo? {
        // Strategy 1: ConnectivityManager (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork
                if (network != null) {
                    val caps = cm.getNetworkCapabilities(network)
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        val info = caps.transportInfo as? WifiInfo
                        if (info != null && isValidBssid(info.bssid)) {
                            Log.d(TAG, "Got WifiInfo via ConnectivityManager: bssid=${info.bssid}")
                            return info
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ConnectivityManager strategy failed", e)
            }
        }

        // Strategy 2: WifiManager (works on all versions, deprecated on 12+)
        try {
            val info = wifiManager.connectionInfo
            if (info != null && isValidBssid(info.bssid)) {
                Log.d(TAG, "Got WifiInfo via WifiManager: bssid=${info.bssid}")
                return info
            }
            Log.d(TAG, "WifiManager returned invalid info: bssid=${info?.bssid}")
        } catch (e: Exception) {
            Log.w(TAG, "WifiManager strategy failed", e)
        }

        Log.d(TAG, "No valid WifiInfo found from any strategy")
        return null
    }

    private fun isValidBssid(bssid: String?): Boolean {
        return bssid != null && bssid != PLACEHOLDER_BSSID && bssid.isNotEmpty()
    }

    private fun hasPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyWifi = ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            return fineLocation || nearbyWifi
        }
        return fineLocation
    }

    private fun getChannelWidth(result: android.net.wifi.ScanResult): Int {
        return when (result.channelWidth) {
            android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ -> 20
            android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ -> 40
            android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ -> 80
            android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ -> 160
            android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
            else -> 20
        }
    }

    private fun getSecurityType(result: android.net.wifi.ScanResult): String {
        val capabilities = result.capabilities
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") && capabilities.contains("WPA3") -> "WPA2/WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            capabilities.contains("ESS") && !capabilities.contains("WPA") && !capabilities.contains("WEP") -> "Open"
            else -> "Open"
        }
    }

    companion object {
        fun frequencyToChannel(frequency: Int): Int = when {
            frequency in 2412..2484 -> {
                if (frequency == 2484) 14
                else (frequency - 2412) / 5 + 1
            }
            frequency in 5170..5825 -> (frequency - 5000) / 5
            frequency in 5925..7125 -> (frequency - 5950) / 5 + 1
            else -> 0
        }
    }
}

data class ConnectionInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val linkSpeed: Int,
    val frequency: Int
)
