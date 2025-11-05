package com.danielebonaldo.neuralyzer.ui

import androidx.compose.ui.graphics.Color
import com.danielebonaldo.neuralyzer.client.NeuralyzerBleClient

data class DeviceUiState(
    val color: Color,
    val intensity: Intensity,
    val activeState: Boolean,
    val deviceConnectionStatus: NeuralyzerBleClient.SDeviceStatus
)

enum class Intensity {
    LOW, MEDIUM, HIGH;

    companion object {
        fun fromInt(value: Int) = when (value) {
            1 -> LOW
            2 -> MEDIUM
            3 -> HIGH
            else -> LOW
        }
    }
}


