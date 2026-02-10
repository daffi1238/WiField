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
                        title = "Señal crítica en AP conectado",
                        description = "El AP conectado '${connectedAp.ssid}' tiene una señal de ${rssi} dBm, que es extremadamente débil.",
                        recommendation = "Acérquese al AP o considere instalar un AP adicional en esta zona para mejorar la cobertura."
                    )
                )
                rssi in -80..-76 -> alerts.add(
                    Alert(
                        type = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.WARNING,
                        title = "Señal débil en AP conectado",
                        description = "El AP conectado '${connectedAp.ssid}' tiene una señal de ${rssi} dBm, que es pobre.",
                        recommendation = "La calidad de la conexión puede verse afectada. Considere reposicionar el AP o instalar un repetidor."
                    )
                )
                rssi in -75..-71 -> alerts.add(
                    Alert(
                        type = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.WARNING,
                        title = "Señal aceptable pero mejorable",
                        description = "El AP conectado '${connectedAp.ssid}' tiene una señal de ${rssi} dBm.",
                        recommendation = "La señal es funcional pero podría degradarse con interferencias. Considere optimizar la ubicación del AP."
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
                        title = "Canal ${channel} saturado (${band.label})",
                        description = "${aps.size} APs están operando en el canal ${channel} de ${band.label}. Esto causa contención y reduce el rendimiento.",
                        recommendation = "Se recomienda redistribuir los APs en canales menos congestionados para reducir la interferencia co-canal."
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
                    title = "Solapamiento de canales en 2.4 GHz",
                    description = "${nonStandardAps.size} AP(s) usando canales no estándar (${channels.joinToString(", ")}). Los canales no estándar causan interferencia con canales adyacentes.",
                    recommendation = "Se recomienda migrar los APs a los canales 1, 6 u 11 que son los únicos que no se solapan en 2.4 GHz."
                )
            )
        }
        return alerts
    }

    private fun checkWeakSecurity(accessPoints: List<ScannedAccessPoint>): List<Alert> {
        val alerts = mutableListOf<Alert>()
        val openAps = accessPoints.filter { it.security.equals("Open", ignoreCase = true) || it.security.equals("Abierta", ignoreCase = true) }
        if (openAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.CRITICAL,
                    title = "${openAps.size} red(es) abierta(s) detectada(s)",
                    description = "Redes sin cifrado: ${openAps.joinToString(", ") { "'${it.ssid}'" }}. Todo el tráfico puede ser interceptado.",
                    recommendation = "Se recomienda configurar cifrado WPA2/WPA3 en todas las redes. Las redes abiertas solo deben usarse con portal cautivo y aislamiento de clientes."
                )
            )
        }

        val wepAps = accessPoints.filter { it.security.contains("WEP", ignoreCase = true) }
        if (wepAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.CRITICAL,
                    title = "${wepAps.size} red(es) con WEP detectada(s)",
                    description = "Redes con cifrado WEP obsoleto: ${wepAps.joinToString(", ") { "'${it.ssid}'" }}. WEP puede ser vulnerado en minutos.",
                    recommendation = "Se recomienda migrar urgentemente a WPA2 o WPA3. WEP no proporciona seguridad real."
                )
            )
        }

        val wpsAps = accessPoints.filter { it.wpsEnabled }
        if (wpsAps.isNotEmpty()) {
            alerts.add(
                Alert(
                    type = AlertType.WEAK_SECURITY,
                    severity = AlertSeverity.WARNING,
                    title = "${wpsAps.size} AP(s) con WPS habilitado",
                    description = "APs con WPS activo: ${wpsAps.joinToString(", ") { "'${it.ssid}'" }}. WPS es un vector de ataque conocido.",
                    recommendation = "Se recomienda deshabilitar WPS en todos los APs. Utilice WPA2/WPA3 con contraseñas robustas."
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
                    title = "${narrow5GhzAps.size} AP(s) en 5 GHz con ancho de canal de 20 MHz",
                    description = "APs en 5 GHz usando solo 20 MHz de ancho de canal: ${narrow5GhzAps.joinToString(", ") { "'${it.ssid}' (canal ${it.channel})" }}.",
                    recommendation = "Considere aumentar el ancho de canal a 40/80/160 MHz en 5 GHz para mejorar el throughput, siempre que no cause interferencia con APs vecinos."
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
                    title = "Densidad excesiva en 2.4 GHz",
                    description = "${strong24Ghz.size} APs con señal fuerte (> -70 dBm) detectados en 2.4 GHz. Esto causa interferencia co-canal significativa.",
                    recommendation = "Reduzca la potencia de transmisión de los APs o desactive la banda de 2.4 GHz en APs duales donde no sea necesaria."
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
                    title = "Densidad excesiva en 5 GHz",
                    description = "${strong5Ghz.size} APs con señal fuerte (> -70 dBm) detectados en 5 GHz. Esto puede causar interferencia co-canal.",
                    recommendation = "Revise la planificación de canales y reduzca la potencia de transmisión en APs cercanos."
                )
            )
        }
        return alerts
    }
}
