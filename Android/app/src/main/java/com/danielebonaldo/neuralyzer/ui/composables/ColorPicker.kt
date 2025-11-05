package com.danielebonaldo.neuralyzer.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.danielebonaldo.neuralyzer.ui.theme.NeuralyzerTheme
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.Color as AndroidColor

@Composable
fun ColorPicker(
    modifier: Modifier = Modifier,
    initialColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    LaunchedEffect(initialColor) {
        selectedColor = initialColor
    }

    val colorWheelBrush = Brush.sweepGradient(
        colors = listOf(
            Color.Red,
            Color.Magenta,
            Color.Blue,
            Color.Cyan,
            Color.Green,
            Color.Yellow,
            Color.Red
        )
    )
    var size2 by remember { mutableStateOf(Size.Zero) }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newColor = colorForPosition(offset, size2.center)
                        selectedColor = newColor
                    },
                    onDrag = { change, _ ->
                        val newColor = colorForPosition(change.position, size2.center)
                        selectedColor = newColor
                        change.consume()
                    },
                    onDragEnd = {
                        onColorSelected(selectedColor)
                    }
                )
            }) {
        size2 = size
        val radius = size.minDimension / 2f
        val strokeWidth = 32.dp.toPx()

        drawCircle(
            brush = colorWheelBrush,
            radius = radius - strokeWidth / 2,
            style = Stroke(width = strokeWidth)
        )

        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(selectedColor.toArgb(), hsv)
        val angle = Math.toRadians(hsv[0].toDouble()).toFloat()
        val selectorRadius = radius - strokeWidth / 2
        val selectorX = center.x + cos(angle) * selectorRadius
        val selectorY = center.y - sin(angle) * selectorRadius

        drawPoints(
            points = listOf(Offset(selectorX, selectorY)),
            pointMode = PointMode.Points,
            color = Color.White,
            strokeWidth = strokeWidth / 2,
            cap = StrokeCap.Round
        )
    }
}

private fun colorForPosition(position: Offset, center: Offset): Color {
    val y = -(position.y - center.y)
    val x = position.x - center.x
    val angle = atan2(y, x)

    val hue = (Math.toDegrees(angle.toDouble()).toFloat() + 360) % 360
    return Color.hsv(hue, 1f, 1f)
}

@Preview(showBackground = true)
@Composable
fun ColorPickerPreview() {
    NeuralyzerTheme {
        ColorPicker(
            modifier = Modifier.size(400.dp),
            initialColor = Color.Magenta,
            onColorSelected = {}
        )
    }

}
