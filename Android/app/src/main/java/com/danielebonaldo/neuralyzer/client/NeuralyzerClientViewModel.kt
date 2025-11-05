package com.danielebonaldo.neuralyzer.client

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

private const val TAG = "NeuralyzerClientViewModel"

class NeuralyzerClientViewModel(application: Application) : AndroidViewModel(application) {

    private val bleClient by lazy { NeuralyzerBleClient(application) }

    val bleDeviceStatus = bleClient.deviceConnectionStatus
    val rgbValue = MutableStateFlow(TimedValue(Color(0, 0, 0), Instant.now()))
    val intensity = MutableStateFlow(TimedValue(1, Instant.now()))
    val activeState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            bleClient.ledColor.collect {
                val color = Color(it)
                Log.i(TAG, "Color r: " + color.toArgb().red)
                Log.i(TAG, "Color g: " + color.toArgb().green)
                Log.i(TAG, "Color b: " + color.toArgb().blue)
                rgbValue.value = TimedValue(Color(color.toArgb().red, color.toArgb().green, color.toArgb().blue))
            }
        }

        viewModelScope.launch {
            bleClient.ledIntensity.collect {
                Log.i(TAG, "LED Effect: $it")
                intensity.value = TimedValue(it)
            }
        }

        viewModelScope.launch {
            bleClient.ledActive.collect {
                Log.i(TAG, "LED Active status: $it")
                activeState.value = it
            }
        }
    }

    fun connect(macAddress: String) {
        bleClient.connect(macAddress)
    }

    fun disconnect() {
        bleClient.disconnect()
    }

    fun setLEDColor(red: Int, green: Int, blue: Int) {
        bleClient.setLEDColor(red, green, blue)
        rgbValue.value = TimedValue(Color(red, green, blue))
    }

    fun readLEDColor() {
        bleClient.readLEDColor()
    }

    fun setLEDIntensity(intensity: Int) {
        bleClient.setLEDIntensity(intensity).let {
            Log.d(TAG, "intensity set to $intensity. Result: $it")
        }
        this.intensity.value = TimedValue(intensity)
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG, "onCleared: ")
    }

    data class TimedValue<T>(val value: T, val timestamp: Instant = Instant.now())
}
