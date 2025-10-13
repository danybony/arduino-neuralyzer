package com.danielebonaldo.neuralyzer.scanner

import android.app.Application
import android.bluetooth.le.ScanResult
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val bleDeviceScanner by lazy { BleScanner(application) }

    val scanStatus = bleDeviceScanner.scanStatus
    val foundDevices = mutableStateListOf<ScanResult>()

    init {
        viewModelScope.launch {
            bleDeviceScanner.scanResultFlow.receiveAsFlow().collect {
                foundDevices.add(it)
            }
        }
    }

    fun startScan() {
        bleDeviceScanner.startScan()
        foundDevices.clear()
    }

    fun stopScan() {
        bleDeviceScanner.stopScan()
        bleDeviceScanner.clearCaches()
    }

}
