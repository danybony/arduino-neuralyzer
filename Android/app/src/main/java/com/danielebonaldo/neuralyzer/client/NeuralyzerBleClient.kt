package com.danielebonaldo.neuralyzer.client

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.IntRange
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


private const val TAG = "NeuralyzerLedBleClient"

class NeuralyzerBleClient(private val context: Context) {

    private var mBluetoothGatt: BluetoothGatt? = null
    private val bleManager =
        context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    private var bluetoothDevice: BluetoothDevice? = null
    private val localScopeStatus = CoroutineScope(Dispatchers.IO)

    private val _deviceConnectionStatus = MutableStateFlow<SDeviceStatus>(SDeviceStatus.UNKNOWN)
    val deviceConnectionStatus = _deviceConnectionStatus as StateFlow<SDeviceStatus>

    private val _ledIntensity = MutableSharedFlow<Int>()
    val ledIntensity = _ledIntensity as SharedFlow<Int>

    private val _ledColor = MutableSharedFlow<Int>()
    val ledColor = _ledColor as SharedFlow<Int>

    private lateinit var mMacAddress: String

    /**
     * Connect to the Cradle Smart Light by macAddress
     *
     * @param macAddress the macAddress of the safety cushion
     */
    @SuppressLint("MissingPermission")
    fun connect(macAddress: String) {
        mMacAddress = macAddress
        if (!hasConnectPermission()) {
            return
        }

        bleManager.adapter.getRemoteDevice(macAddress)?.let {
            bluetoothDevice = it
            mBluetoothGatt = bluetoothDevice?.connectGatt(context, false, gattCallback)
        } ?: run {
            throw Exception("Bluetooth device not found, please scan first")
        }
    }

    /**
     * Disconnect from the safety cushion
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (!hasConnectPermission()) {
            return
        }
        mBluetoothGatt?.disconnect()
    }

    /**
     * Turn ON / OFF the LED
     *
     * @param value ESwitch.ON select on/off status
     */
    fun setLEDStatus(value: ESwitch): Boolean? {
        val service = mBluetoothGatt?.getService(NeuralyzerLedUUID.NeuralyzerLightService.uuid)
        val characteristic =
            service?.getCharacteristic(NeuralyzerLedUUID.NeuralyzerLightService.LEDColor.uuid)

        val payload = byteArrayOf(value.value.toByte())
        return sendCommand(characteristic, payload)
    }

    /**
     * Change LED Color by using RGB value
     *
     * @param red Red Color
     * @param green Green Color
     * @param blue Blue Color
     */
    fun setLEDColor(
        @IntRange(from = 0L, to = 255L) red: Int,
        @IntRange(from = 0L, to = 255L) green: Int,
        @IntRange(from = 0L, to = 255L) blue: Int
    ): Boolean? {
        val service = mBluetoothGatt?.getService(NeuralyzerLedUUID.NeuralyzerLightService.uuid)
        val characteristic =
            service?.getCharacteristic(NeuralyzerLedUUID.NeuralyzerLightService.LEDColor.uuid)

        val payload = byteArrayOf(red.toByte(), green.toByte(), blue.toByte())
        return sendCommand(characteristic, payload)
    }

    /**
     * Sets the LED effect for a Bluetooth-connected LED device.
     *
     * Communicates with a Bluetooth LE (Low Energy) device to set the LED effect based on the specified effect index. The effect index should be within the range of 0 to 255.
     *
     * @param effectIndex An integer representing the LED effect index, constrained within the range 0 to 2 inclusive.
     * @return Returns a Boolean indicating the success of the operation. `true` if the command is successfully sent, `false` if the BluetoothGatt service or characteristic is null, or if the command fails. Returns `null` if an exception occurs.
     */
    fun setLEDIntensity(@IntRange(from = 0L, to = 3) effectIndex: Int): Boolean {
        val service = mBluetoothGatt?.getService(NeuralyzerLedUUID.NeuralyzerLightService.uuid)
        val characteristic = service?.getCharacteristic(NeuralyzerLedUUID.NeuralyzerLightService.LEDIntensity.uuid)

        val payload = byteArrayOf(effectIndex.toByte())
        return sendCommand(characteristic, payload)
    }

    @SuppressLint("MissingPermission")
    private fun sendCommand(
        characteristic: BluetoothGattCharacteristic?,
        payload: ByteArray
    ): Boolean {
        if (characteristic == null) {
            Log.d(TAG, "null characteristic on sendCommand")
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = mBluetoothGatt?.writeCharacteristic(
                characteristic,
                payload,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            )

            val res = when (result) {
                BluetoothStatusCodes.SUCCESS -> true
                else -> {
                    Log.d(TAG, "sendCommand: Error with result $result")
                    false
                }
            }
            return res
        } else {
            characteristic.value = payload
            return mBluetoothGatt?.writeCharacteristic(characteristic) == true
        }
    }

    @SuppressLint("MissingPermission")
    fun readLEDColor(): Boolean? {
        val service = mBluetoothGatt?.getService(NeuralyzerLedUUID.NeuralyzerLightService.uuid)
        val characteristic =
            service?.getCharacteristic(NeuralyzerLedUUID.NeuralyzerLightService.LEDColor.uuid)

        return mBluetoothGatt?.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    fun readLEDIntensity(): Boolean? {
        val service = mBluetoothGatt?.getService(NeuralyzerLedUUID.NeuralyzerLightService.uuid)
        val characteristic =
            service?.getCharacteristic(NeuralyzerLedUUID.NeuralyzerLightService.LEDIntensity.uuid)

        return mBluetoothGatt?.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
        }

        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(gatt, txPhy, rxPhy, status)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            localScopeStatus.launch {
                val state = when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> {
                        if (!hasConnectPermission()) {
                            return@launch
                        }
                        Log.i(TAG, "Successful connect to device. Status: Success")

                        mBluetoothGatt?.discoverServices()
                        SDeviceStatus.CONNECTED
                    }

                    BluetoothGatt.STATE_DISCONNECTED -> {
                        mBluetoothGatt?.close()
                        SDeviceStatus.DISCONNECTED
                    }

                    else -> SDeviceStatus.UNKNOWN
                }

                _deviceConnectionStatus.emit(state)
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG, "Successfully discovered services")
            Log.i(TAG, "onServicesDiscovered: " + gatt?.device?.address)
            localScopeStatus.launch {
                _deviceConnectionStatus.emit(SDeviceStatus.READY)
                readLEDColor()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.d(
                TAG,
                "📖 onCharacteristicRead: ${characteristic?.uuid}, Data: ${characteristic?.value.contentToString()}"
            )
            when (characteristic?.uuid) {
                NeuralyzerLedUUID.NeuralyzerLightService.LEDIntensity.uuid -> {
                    val ledIntensity = characteristic.value[0]
                    localScopeStatus.launch {
                        _ledIntensity.emit(ledIntensity.toInt())
                    }
                }

                NeuralyzerLedUUID.NeuralyzerLightService.LEDColor.uuid -> {
                    val red: UByte = characteristic.value[0].toUByte()
                    val green: UByte = characteristic.value[1].toUByte()
                    val blue: UByte = characteristic.value[2].toUByte()
                    val color = Color.rgb(red.toInt(), green.toInt(), blue.toInt())
                    localScopeStatus.launch {
                        _ledColor.emit(color)
                    }
                    readLEDIntensity()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i(TAG, "🖊 ️onCharacteristicWrite: ${characteristic?.uuid}")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            Log.i(TAG, "onCharacteristicChanged: ${characteristic?.uuid}")
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.i(TAG, "onMtuChanged: $mtu")
            Log.i(TAG, "onMtuChanged: MTU = $mtu, status = $status")
        }
    }

    sealed class SDeviceStatus {
        object UNKNOWN : SDeviceStatus()
        object CONNECTED : SDeviceStatus()
        object READY : SDeviceStatus()
        object DISCONNECTED : SDeviceStatus()
    }

    enum class ESwitch(val value: Int) {
        OFF(0x0),
        ON(0x1);

        fun toBool() = value == ON.value

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    /**
     * Check if the app has the necessary permissions to scan for iBeacon devices.
     */
    private fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.hasPermissions(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            true
        }
    }

    private fun Context.hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

}
