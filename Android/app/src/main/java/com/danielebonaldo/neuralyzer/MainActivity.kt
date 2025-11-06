package com.danielebonaldo.neuralyzer

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danielebonaldo.neuralyzer.client.NeuralyzerClientViewModel
import com.danielebonaldo.neuralyzer.scanner.ScannerViewModel
import com.danielebonaldo.neuralyzer.ui.DeviceScreen
import com.danielebonaldo.neuralyzer.ui.DeviceUiState
import com.danielebonaldo.neuralyzer.ui.Intensity
import com.danielebonaldo.neuralyzer.ui.ScanScreen
import com.danielebonaldo.neuralyzer.ui.theme.NeuralyzerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scannerViewModel: ScannerViewModel = viewModel()
            val scanStatus = scannerViewModel.scanStatus.collectAsState()
            val navController = rememberNavController()

            NeuralyzerTheme {
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
                    NavHost(
                        navController = navController,
                        startDestination = "main",
                    ) {
                        composable(route = "main") {
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
                                    modifier = Modifier.padding(innerPadding),
                                    scanStatus = scanStatus,
                                    scanResult = scannerViewModel.foundDevices,
                                    onSelectDevice = { macAddress ->
                                        scannerViewModel.stopScan()
                                        navController.navigate("detail/$macAddress") {
                                            launchSingleTop = true
                                        }
                                    }
                                )

                            } else {
                                Surface(
                                    modifier = Modifier
                                        .padding(innerPadding)
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

                        composable(route = "detail/{macAddress}") { backStackEntry ->
                            val macAddress = backStackEntry.arguments?.getString("macAddress")
                            macAddress?.let {
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
                                    modifier = Modifier.padding(innerPadding),
                                    onConnect = {
                                        viewModel.connect(macAddress)
                                        scannerViewModel.stopScan()
                                    },
                                    onDisconnect = { viewModel.disconnect() },
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
                    }
                }
            }
        }
    }
}
