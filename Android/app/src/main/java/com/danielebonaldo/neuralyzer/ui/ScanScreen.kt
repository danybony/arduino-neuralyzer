package com.danielebonaldo.neuralyzer.ui

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danielebonaldo.neuralyzer.scanner.ScanStatus

@SuppressLint("MissingPermission")
@Composable
fun ScanScreen(
    modifier: Modifier = Modifier,
    onStartScan: () -> Unit = {},
    onStopScan: () -> Unit = {},
    scanStatus: State<ScanStatus?>,
    scanResult: SnapshotStateList<ScanResult>,
    onSelectDevice: (String) -> Unit
) {
    // A surface container using the 'background' color from the theme
    Surface(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
        ) {
            Button(modifier = Modifier.fillMaxWidth(), onClick = onStartScan) {
                Text(text = "Start Scan")
            }
            Button(modifier = Modifier.fillMaxWidth(), onClick = onStopScan) {
                Text(text = "Stop Scan!")
            }

            var counter = 1.0f
            var color = Color.Red

            when (scanStatus.value) {
                is ScanStatus.ERROR -> {
                    counter = 1f
                    color = Color.Red
                }

                ScanStatus.START -> {
                    counter = 1f
                    color = Color.Green
                }

                ScanStatus.STOP -> {
                    counter = 1f
                    color = Color.Red
                }

                ScanStatus.UNKNOWN -> {
                    counter = 1f
                    color = Color.Gray
                }

                null -> {
                    counter = 0f
                }
            }

            ProgressScan(counter, color)

            ScanStatusText(scanStatus.value)

            Divider()
            LazyColumn {
                items(scanResult) {
                    Spacer(Modifier.size(8.dp))
                    DevicePickerItem(
                        macAddress = it.device.address,
                        rssi = it.rssi,
                        name = it.device.name ?: "-",
                        onSelect = onSelectDevice
                    )
                }
            }
        }
    }
}

@Composable
fun DevicePickerItem(macAddress: String, rssi: Int, name: String, onSelect: (String) -> Unit) {
    Card(Modifier.clickable {
        onSelect(macAddress)
    }) {
        Column(Modifier.padding(10.dp)) {
            Text(
                text = "$name ($macAddress)",
                style = TextStyle(
                    fontSize = 20.sp,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
            Text(text = "RSSI: $rssi", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
fun ScanStatusText(scanStatus: ScanStatus?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Scan Status: ${scanStatus.toString()}",
            modifier = Modifier
                .padding(16.dp),
            color = Color.White
        )
        if (scanStatus == ScanStatus.START) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ProgressScan(value: Float, colorState: Color) {
    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        color = colorState,
        progress = value
    )
}
