package com.danielebonaldo.neuralyzer.ui

import androidx.compose.ui.graphics.Color
import com.danielebonaldo.neuralyzer.client.NeuralyzerBleClient

data class DeviceUiState(
    val color: Color,
    val intensity: Intensity,
    val deviceConnectionStatus: NeuralyzerBleClient.SDeviceStatus
)

enum class Intensity {
    LOW, MEDIUM, HIGH
}


