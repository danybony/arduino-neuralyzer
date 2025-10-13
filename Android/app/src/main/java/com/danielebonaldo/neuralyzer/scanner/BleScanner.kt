package com.danielebonaldo.neuralyzer.scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import com.danielebonaldo.neuralyzer.client.NeuralyzerLedUUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val TAG = "BleScanner"

class BleScanner(private val context: Context) {

    private var localScope = CoroutineScope(Dispatchers.IO)

    private val _bleManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    private val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(NeuralyzerLedUUID.NeuralyzerLightService.uuid))
        .build()

    private val scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()

    private val setMatches = hashMapOf<String, ScanResult>()

    val scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.UNKNOWN)
    val scanResultFlow: Channel<ScanResult> by lazy { Channel {} }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                localScope.launch {
                    val elementExist = setMatches[it.device.address]
                    if (elementExist == null) {
                        Log.i(TAG, "Smart LED detected: " + it.device.address)
                        setMatches[it.device.address] = it
                        scanResultFlow.send(it)
                    } else {
                        Log.i(TAG, "Exist Element - SKIP " + it.device.address)
                    }
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            localScope.launch {
                scanStatus.emit(ScanStatus.ERROR(errorCode))
                stopScan()
            }
        }
    }

    /**
     * Start scanning for iBeacon devices.
     *
     * @param scanDuration The duration of the scan in seconds
     */
    @SuppressLint("MissingPermission")
    @Throws(SecurityException::class)
    fun startScan() {
        if (!hasScanPermission()) {
            throw buildSecurityException()
        } else {
            _bleManager.adapter.bluetoothLeScanner?.stopScan(scanCallback)

            localScope.cancel()

            _bleManager.adapter.bluetoothLeScanner?.startScan(
                mutableListOf(scanFilter), scanSettings, scanCallback
            )

            localScope = CoroutineScope(Dispatchers.IO)
            localScope.launch {
                Log.i(TAG, "Start Scan")
                scanStatus.emit(ScanStatus.START)
            }
        }
    }

    /**
     * Stop scanning for iBeacon devices.
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasScanPermission()) {
            throw buildSecurityException()
        } else {
            _bleManager.adapter.bluetoothLeScanner?.stopScan(
                scanCallback
            )

            localScope.launch {
                Log.i(TAG, "STOP Scan")
                scanStatus.emit(ScanStatus.STOP)
            }
        }
    }

    /**
     * Clear cache of found devices
     */
    fun clearCaches() = setMatches.clear()

    /**
     * Clear cache for specific device
     *
     * @param macAddress MacAddress of the device to remove from cache
     */
    fun clearCachesForMacAddress(macAddress: String) = setMatches.remove(macAddress)

    /**
     * Check if the app has the necessary permissions to scan for iBeacon devices.
     */
    private fun hasScanPermission(): Boolean {
        return context.hasPermissions(
            Manifest.permission.BLUETOOTH_SCAN
        )
    }

    /**
     * Build a SecurityException with a message that explains why the app does not have the necessary
     * permissions to scan for iBeacon devices.
     */
    private fun buildSecurityException(): SecurityException {
        val stringBuilder = StringBuilder()

        if (!context.hasPermissions(Manifest.permission.ACCESS_FINE_LOCATION)) {
            stringBuilder.append("Missing ACCESS_FINE_LOCATION permission")
        }

        if (!context.hasPermissions(Manifest.permission.BLUETOOTH_SCAN)) {
            stringBuilder.append("Missing BLUETOOTH_SCAN permission")
        }

        return SecurityException(stringBuilder.toString())
    }
}

fun Context.hasPermissions(vararg permissions: String): Boolean = permissions.all {
    ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

sealed class ScanStatus {
    object START : ScanStatus() {
        override fun toString() = "START"
    }

    object STOP : ScanStatus() {
        override fun toString() = "STOP"
    }

    class ERROR(val errorCode: Int) : ScanStatus() {
        override fun toString() = "ERROR $errorCode"
    }

    object UNKNOWN : ScanStatus() {
        override fun toString() = "UNKNOWN"
    }
}
