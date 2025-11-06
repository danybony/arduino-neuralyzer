package com.danielebonaldo.neuralyzer

import android.Manifest
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danielebonaldo.neuralyzer.client.NeuralyzerClientViewModel
import com.danielebonaldo.neuralyzer.scanner.ScannerViewModel
import com.danielebonaldo.neuralyzer.ui.DeviceScreen
import com.danielebonaldo.neuralyzer.ui.DeviceUiState
import com.danielebonaldo.neuralyzer.ui.Intensity
import com.danielebonaldo.neuralyzer.ui.ScanScreen
import com.danielebonaldo.neuralyzer.ui.theme.NeuralyzerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scannerViewModel: ScannerViewModel = viewModel()
            val scanStatus = scannerViewModel.scanStatus.collectAsState()

            NeuralyzerTheme {
                val coroutineScope = rememberCoroutineScope()
                val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<NavItemData>()
                val selectedDestination = scaffoldNavigator.currentDestination?.contentKey

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(stringResource(R.string.app_name))
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    ListDetailPaneScaffold(
                        modifier = Modifier.padding(innerPadding),
                        directive = scaffoldNavigator.scaffoldDirective,
                        scaffoldState = scaffoldNavigator.scaffoldState,
                        listPane = {
                            AnimatedPane {
                                val bluetoothPermissionState = rememberMultiplePermissionsState(
                                    listOf(
                                        Manifest.permission.BLUETOOTH_SCAN,
                                        Manifest.permission.BLUETOOTH_CONNECT
                                    )
                                )

                                if (bluetoothPermissionState.allPermissionsGranted) {
                                    ScanScreen(
                                        onStartScan = {
                                            scannerViewModel.startScan()
                                        },
                                        onStopScan = {
                                            scannerViewModel.stopScan()
                                        },
                                        modifier = Modifier.width(100.dp),//.padding(innerPadding),
                                        scanStatus = scanStatus,
                                        scanResult = scannerViewModel.foundDevices,
                                        onSelectDevice = { macAddress ->
                                            scannerViewModel.stopScan()
                                            coroutineScope.launch {
                                                scaffoldNavigator.navigateTo(
                                                    pane = ListDetailPaneScaffoldRole.Detail,
                                                    contentKey = NavItemData(macAddress),
                                                )
                                            }
                                        }
                                    )

                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                    ) {
                                        Box {
                                            Button(modifier = Modifier.align(Alignment.Center), onClick = {
                                                bluetoothPermissionState.launchMultiplePermissionRequest()
                                            }) {
                                                Text("Grant BLE Permission")
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        detailPane = {
                            AnimatedPane {
                                val selectedMacAddress = selectedDestination?.macAddress
                                if (selectedMacAddress == null) {
                                    DeviceNotSelectedScreen()
                                } else selectedMacAddress?.let { macAddress ->
                                    val viewModel: NeuralyzerClientViewModel = viewModel()
                                    val deviceState by viewModel.bleDeviceStatus.collectAsState()
                                    val currentRGB by viewModel.rgbValue.collectAsState()
                                    val currentIntensity by viewModel.intensity.collectAsState()
                                    val currentActiveState by viewModel.activeState.collectAsState()

                                    val status = DeviceUiState(
                                        color = currentRGB.value,
                                        intensity = Intensity.fromInt(currentIntensity.value),
                                        activeState = currentActiveState,
                                        deviceConnectionStatus = deviceState,
                                    )

                                    DeviceScreen(
                                        deviceStatus = status,
                                        onConnect = {
                                            viewModel.connect(macAddress)
                                            scannerViewModel.stopScan()
                                        },
                                        onDisconnect = {
                                            viewModel.disconnect()
                                        },
                                        onColorSelected = {
                                            viewModel.setLEDColor(
                                                it.toArgb().red,
                                                it.toArgb().green,
                                                it.toArgb().blue
                                            )
                                        },
                                        onIntensitySelected = {
                                            viewModel.setLEDIntensity(it + 1)
                                        }
                                    )

                                }
                            }
                        },
                    )
                }

            }
        }
    }

    @Composable
    private fun DeviceNotSelectedScreen() {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Scan and select a nearby Neuralyzer",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
        }
    }

    private data class NavItemData(val macAddress: String?) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readString())

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(macAddress)
        }

        companion object CREATOR : Parcelable.Creator<NavItemData?> {
            override fun createFromParcel(source: Parcel) = NavItemData(source)

            override fun newArray(size: Int): Array<NavItemData?> = arrayOfNulls(size)
        }
    }
}
