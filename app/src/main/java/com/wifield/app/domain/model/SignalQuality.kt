package com.wifield.app.domain.model

import androidx.compose.ui.graphics.Color

enum class SignalQuality(val label: String, val color: Color, val minRssi: Int) {
    EXCELLENT("Excelente", Color(0xFF4CAF50), -50),
    GOOD("Bueno", Color(0xFF2196F3), -60),
    FAIR("Aceptable", Color(0xFFFFC107), -70),
    WEAK("Débil", Color(0xFFFF9800), -80),
    CRITICAL("Crítico", Color(0xFFF44336), Int.MIN_VALUE);

    companion object {
        fun fromRssi(rssi: Int): SignalQuality = when {
            rssi > -50 -> EXCELLENT
            rssi > -60 -> GOOD
            rssi > -70 -> FAIR
            rssi > -80 -> WEAK
            else -> CRITICAL
        }
    }
}
