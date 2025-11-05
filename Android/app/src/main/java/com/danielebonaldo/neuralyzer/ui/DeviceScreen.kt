package com.danielebonaldo.neuralyzer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.danielebonaldo.neuralyzer.R
import com.danielebonaldo.neuralyzer.client.NeuralyzerBleClient
import com.danielebonaldo.neuralyzer.ui.composables.ColorPicker
import com.danielebonaldo.neuralyzer.ui.composables.IntensitySlider
import com.danielebonaldo.neuralyzer.ui.theme.NeuralyzerTheme

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LedStatus(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.Center)
                    .padding(32.dp),
                intensity = deviceStatus.intensity,
                color = deviceStatus.color,
                active = deviceStatus.activeState
            )
            ColorPicker(
                modifier = Modifier.fillMaxSize(),
                initialColor = deviceStatus.color,
                onColorSelected = onColorSelected
            )
        }

        IntensitySlider(
            modifier = Modifier.fillMaxWidth(),
            intensity = deviceStatus.intensity,
            onIntensitySelected = onIntensitySelected
        )
    }
}

@Composable
fun LedStatus(intensity: Intensity, color: Color, active: Boolean, modifier: Modifier) {
    val activeLeds = if (!active) {
        0
    } else {
        when (intensity) {
            Intensity.LOW -> 2
            Intensity.MEDIUM -> 4
            Intensity.HIGH -> 8
        }
    }
    val disabledColor = Color.DarkGray

    Canvas(modifier.fillMaxSize().clip(CircleShape)) {

        val neuralyzerColor = Color.Gray
        val neuralyzerWidth = (size.width / 3f).toDp()
        val ledDotWidth = neuralyzerWidth.toPx() / 10

        drawCircle(
            color = neuralyzerColor,
            radius = neuralyzerWidth.toPx() / 2f,
            center = Offset(size.width / 2f, size.height * 0.25f)
        )

        drawRect(
            color = neuralyzerColor,
            topLeft = Offset(
                x = size.width / 2f - neuralyzerWidth.toPx() / 2f,
                y = size.height * 0.25f
            ),
            size = androidx.compose.ui.geometry.Size(
                width = neuralyzerWidth.toPx(),
                height = size.height * 0.75f
            )
        )

        drawLine(
            color = disabledColor,
            start = Offset(
                x = size.width / 2f - neuralyzerWidth.toPx() / 2f,
                y = size.height * 0.25f
            ),
            end = Offset(
                x = size.width / 2f + neuralyzerWidth.toPx() / 2f,
                y = size.height * 0.25f
            ),
            strokeWidth = 2.dp.toPx()
        )

        repeat(8) {
            val isActive = it > 3 - activeLeds / 2 && it < 4 + activeLeds / 2
            val dotCenter = Offset(
                x = size.width / 2f,
                y = size.height * 0.25f + (it + 1) * size.height / 12
            )
            drawCircle(
                color = if (isActive) color else disabledColor,
                radius = ledDotWidth,
                center = dotCenter
            )
            if (isActive){
                val lightRadius = ledDotWidth * 10
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            color.copy(alpha = 0.5f),
                            Color.Transparent
                        ),
                        radius = lightRadius,
                        center = dotCenter
                    ),
                    radius = lightRadius,
                    center = dotCenter
                )
            }
        }
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

@PreviewScreenSizes
@Composable
private fun DeviceScreenPreview() {
    NeuralyzerTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.background)){
            DeviceScreen(
                deviceStatus = DeviceUiState(
                    color = Color.Cyan,
                    intensity = Intensity.HIGH,
                    activeState = true,
                    deviceConnectionStatus = NeuralyzerBleClient.SDeviceStatus.CONNECTED
                ),
                onConnect = {},
                onDisconnect = {},
                onColorSelected = {},
                onIntensitySelected = {}
            )
        }
    }
}
