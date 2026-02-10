package com.wifield.app.domain.model

data class ScannedAccessPoint(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val channel: Int,
    val frequency: Int,
    val band: WifiBand,
    val channelWidth: Int,
    val security: String,
    val wpsEnabled: Boolean,
    val isConnected: Boolean = false
)

enum class WifiBand(val label: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz");

    companion object {
        fun fromFrequency(frequency: Int): WifiBand = when {
            frequency in 2400..2500 -> BAND_2_4_GHZ
            frequency in 5000..5900 -> BAND_5_GHZ
            frequency in 5925..7125 -> BAND_6_GHZ
            else -> BAND_2_4_GHZ
        }
    }
}

data class ActiveTestResults(
    val downloadSpeed: Double = 0.0,
    val uploadSpeed: Double = 0.0,
    val latency: Double = 0.0,
    val jitter: Double = 0.0,
    val packetLoss: Double = 0.0,
    val linkSpeed: Int = 0,
    val gatewayLatency: Double = 0.0
)

data class SsidGroup(
    val ssid: String,
    val accessPoints: List<ScannedAccessPoint>
) {
    val bestRssi: Int get() = accessPoints.maxOfOrNull { it.rssi } ?: -100
    val apCount: Int get() = accessPoints.size
    val bands: Set<WifiBand> get() = accessPoints.map { it.band }.toSet()
}

data class ChannelInfo(
    val channel: Int,
    val band: WifiBand,
    val apCount: Int,
    val accessPoints: List<ScannedAccessPoint>
)
