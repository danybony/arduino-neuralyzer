package com.danielebonaldo.neuralyzer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielebonaldo.neuralyzer.R
import com.danielebonaldo.neuralyzer.client.NeuralyzerBleClient
import com.danielebonaldo.neuralyzer.ui.composables.ColorPicker
import com.danielebonaldo.neuralyzer.ui.composables.IntensitySlider

@Composable
fun DeviceScreen(
    deviceStatus: DeviceUiState,
    modifier: Modifier = Modifier,
    onConnect: () -> Unit = {},
    onDisconnect: () -> Unit = {},
    onIntensitySelected: (Int) -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        ConnectionRow(
            deviceConnectionStatus = deviceStatus.deviceConnectionStatus,
            onConnect = onConnect,
            onDisconnect = onDisconnect
        )
        ColorPicker(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .weight(1f),
            initialColor = deviceStatus.color,
            onColorSelected = onColorSelected
        )
        IntensitySlider(
            modifier = Modifier.Companion
                .fillMaxWidth(),
            intensity = deviceStatus.intensity,
            onIntensitySelected = onIntensitySelected
        )
    }
}

@Composable
private fun ConnectionRow(
    deviceConnectionStatus: NeuralyzerBleClient.SDeviceStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        when (deviceConnectionStatus) {
            is NeuralyzerBleClient.SDeviceStatus.DISCONNECTED,
            is NeuralyzerBleClient.SDeviceStatus.UNKNOWN -> Button(
                onClick = onConnect,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.connect))
            }

            else -> Button(
                onClick = onDisconnect,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.disconnect))
            }
        }

        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp)
                    .background(deviceConnectionStatus.statusColor(), shape = CircleShape)
                    .requiredSize(48.dp, 24.dp)
            )
        }
    }
}

private fun NeuralyzerBleClient.SDeviceStatus.statusColor(): Color {
    return when (this) {
        NeuralyzerBleClient.SDeviceStatus.CONNECTED -> Color.Yellow
        NeuralyzerBleClient.SDeviceStatus.READY -> Color.Green
        NeuralyzerBleClient.SDeviceStatus.UNKNOWN -> Color.Gray
        is NeuralyzerBleClient.SDeviceStatus.DISCONNECTED -> Color.Red
    }
}
