package com.danielebonaldo.neuralyzer.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.danielebonaldo.neuralyzer.R
import com.danielebonaldo.neuralyzer.ui.Intensity
import kotlin.math.roundToInt

@Composable
fun IntensitySlider(
    intensity: Intensity,
    modifier: Modifier = Modifier,
    onIntensitySelected: (Int) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(intensity.ordinal.toFloat()) }
    LaunchedEffect(intensity) {
        sliderPosition = intensity.ordinal.toFloat()
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Companion.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.selected_intensity_format, Intensity.entries[sliderPosition.roundToInt()]))
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onIntensitySelected(sliderPosition.roundToInt())
            },
            valueRange = 0f..Intensity.entries.size.toFloat() - 1,
            steps = 1,
            modifier = Modifier.Companion.padding(top = 16.dp)
        )
    }
}
