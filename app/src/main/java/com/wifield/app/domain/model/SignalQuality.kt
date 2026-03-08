package com.wifield.app.domain.model

import androidx.compose.ui.graphics.Color

enum class SignalQuality(val label: String, val color: Color, val minRssi: Int) {
    EXCELLENT("Excellent", Color(0xFF4CAF50), -50),
    GOOD("Good", Color(0xFF2196F3), -60),
    FAIR("Fair", Color(0xFFFFC107), -70),
    WEAK("Weak", Color(0xFFFF9800), -80),
    CRITICAL("Critical", Color(0xFFF44336), Int.MIN_VALUE);

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
