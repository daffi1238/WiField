package com.wifield.app.domain.analyzer

import com.wifield.app.domain.model.*

object AlertAnalyzer {

    fun analyze(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        alerts.addAll(checkWeakSignals(accessPoints))
        alerts.addAll(checkSaturatedChannels(accessPoints))
        alerts.addAll(checkChannelOverlap(accessPoints))
        alerts.addAll(checkWeakSecurity(accessPoints))
        alerts.addAll(checkSuboptimalChannelWidth(accessPoints))
        alerts.addAll(checkExcessiveApDensity(accessPoints))
        return alerts.sortedByDescending { it.severity.ordinal }
    }

    private fun checkWeakSignals(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val connectedAp = accessPoints.find { it.isConnected }
        if (connectedAp != null) {
            val rssi = connectedAp.rssi
            when {
                rssi < -80 -> alerts.add(
                    Alert(
                        type = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.CRITICAL,
                        title = "Critical signal on connected AP",
                        description = "The connected AP '${connectedAp.ssid}' has a signal of ${rssi} dBm, which is extremely weak.",
                        recommendation = "Move closer to the AP or consider installing an additional AP in this area to improve coverage."
                    )
                )
                rssi in -80..-76 -> alerts.add(
                    Alert(
                        type = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.WARNING,
                        title = "Weak signal on connected AP",
                        description = "The connected AP '${connectedAp.ssid}' has a signal of ${rssi} dBm, which is poor.",
                        recommendation = "Connection quality may be affected. Consider repositioning the AP or installing a repeater."
                    )
                )
                rssi in -75..-71 -> alerts.add(
                    Alert(
                        type = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.WARNING,
                        title = "Acceptable but improvable signal",
                        description = "The connected AP '${connectedAp.ssid}' has a signal of ${rssi} dBm.",
                        recommendation = "The signal is functional but could degrade with interference. Consider optimizing the AP placement."
                    )
                )
            }
        }
        return alerts
    }

    private fun checkSaturatedChannels(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val channelGroups = accessPoints.groupBy { Pair(it.channel, it.band) }
        for ((key, aps) in channelGroups) {
            if (aps.size > 3) {
                val (channel, band) = key
                alerts.add(
                    Alert(
                        type = AlertType.SATURATED_CHANNEL,
                        severity = if (aps.size > 6) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        title = "Channel ${channel} saturated (${band.label})",
                        description = "${aps.size} APs are operating on channel ${channel} of ${band.label}. This causes contention and reduces performance.",
                        recommendation = "It is recommended to redistribute APs across less congested channels to reduce co-channel interference."
                    )
                )
            }
        }
        return alerts
    }

    private fun checkChannelOverlap(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val standardChannels = setOf(1, 6, 11)
        val aps24Ghz = accessPoints.filter { it.band == WifiBand.BAND_2_4_GHZ }
        val nonStandardAps = aps24Ghz.filter { it.channel !in standardChannels }
        if (nonStandardAps.isNotEmpty()) {
            val channels = nonStandardAps.map { it.channel }.distinct().sorted()
            alerts.add(
                Alert(
                    type = AlertType.CHANNEL_OVERLAP,
                    severity = AlertSeverity.WARNING,
                    title = "Channel overlap in 2.4 GHz",
                    description = "${nonStandardAps.size} AP(s) using non-standard channels (${channels.joinToString(", ")}). Non-standard channels cause interference with adjacent channels.",
                    recommendation = "It is recommended to migrate APs to channels 1, 6, or 11, which are the only non-overlapping channels in 2.4 GHz."
                )
            )
        }
        return alerts
    }

    private fun checkWeakSecurity(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val openAps = accessPoints.filter { it.security.equals("Open", ignoreCase = true) }
        if (openAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.CRITICAL,
                    title = "${openAps.size} open network(s) detected",
                    description = "Unencrypted networks: ${openAps.joinToString(", ") { "'${it.ssid}'" }}. All traffic can be intercepted.",
                    recommendation = "It is recommended to configure WPA2/WPA3 encryption on all networks. Open networks should only be used with a captive portal and client isolation."
                )
            )
        }

        val wepAps = accessPoints.filter { it.security.contains("WEP", ignoreCase = true) }
        if (wepAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.CRITICAL,
                    title = "${wepAps.size} network(s) with WEP detected",
                    description = "Networks with obsolete WEP encryption: ${wepAps.joinToString(", ") { "'${it.ssid}'" }}. WEP can be cracked in minutes.",
                    recommendation = "It is strongly recommended to migrate to WPA2 or WPA3. WEP provides no real security."
                )
            )
        }

        val wpsAps = accessPoints.filter { it.wpsEnabled }
        if (wpsAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.WARNING,
                    title = "${wpsAps.size} AP(s) with WPS enabled",
                    description = "APs with active WPS: ${wpsAps.joinToString(", ") { "'${it.ssid}'" }}. WPS is a known attack vector.",
                    recommendation = "It is recommended to disable WPS on all APs. Use WPA2/WPA3 with strong passwords."
                )
            )
        }
        return alerts
    }

    private fun checkSuboptimalChannelWidth(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val narrow5GhzAps = accessPoints.filter {
            it.band == WifiBand.BAND_5_GHZ && it.channelWidth == 20
        }
        if (narrow5GhzAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.SUBOPTIMAL_CHANNEL_WIDTH,
                    severity = AlertSeverity.INFO,
                    title = "${narrow5GhzAps.size} AP(s) on 5 GHz with 20 MHz channel width",
                    description = "APs on 5 GHz using only 20 MHz channel width: ${narrow5GhzAps.joinToString(", ") { "'${it.ssid}' (channel ${it.channel})" }}.",
                    recommendation = "Consider increasing the channel width to 40/80/160 MHz on 5 GHz to improve throughput, as long as it does not cause interference with neighboring APs."
                )
            )
        }
        return alerts
    }

    private fun checkExcessiveApDensity(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val strong24Ghz = accessPoints.filter {
            it.band == WifiBand.BAND_2_4_GHZ && it.rssi > -70
        }
        if (strong24Ghz.size > 10) {
            alerts.add(
                Alert(
                    type = AlertType.EXCESSIVE_AP_DENSITY,
                    severity = AlertSeverity.WARNING,
                    title = "Excessive density on 2.4 GHz",
                    description = "${strong24Ghz.size} APs with strong signal (> -70 dBm) detected on 2.4 GHz. This causes significant co-channel interference.",
                    recommendation = "Reduce the transmit power of APs or disable the 2.4 GHz band on dual-band APs where it is not needed."
                )
            )
        }

        val strong5Ghz = accessPoints.filter {
            it.band == WifiBand.BAND_5_GHZ && it.rssi > -70
        }
        if (strong5Ghz.size > 15) {
            alerts.add(
                Alert(
                    type = AlertType.EXCESSIVE_AP_DENSITY,
                    severity = AlertSeverity.WARNING,
                    title = "Excessive density on 5 GHz",
                    description = "${strong5Ghz.size} APs with strong signal (> -70 dBm) detected on 5 GHz. This may cause co-channel interference.",
                    recommendation = "Review the channel plan and reduce transmit power on nearby APs."
                )
            )
        }
        return alerts
    }
}
