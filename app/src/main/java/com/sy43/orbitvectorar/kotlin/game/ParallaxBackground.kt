package com.sy43.orbitvectorar.kotlin.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sy43.orbitvectorar.R

@Composable
fun ParallaxBackground(
    modifier: Modifier = Modifier,
    imageRes: Int = R.drawable.ui_menu_bg,
    maxParallaxOffsetDp: Int = 24,
    scale: Float = 1.15f
) {
    // --- parallax 
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }
    var parallaxOffsetX by remember { mutableStateOf(0f) }
    var parallaxOffsetY by remember { mutableStateOf(0f) }
    val maxParallaxOffsetPx = with(LocalDensity.current) { maxParallaxOffsetDp.dp.toPx() }
    DisposableEffect(sensorManager, rotationSensor) {
        val sensorEventListener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent?) {
                val sensorType = rotationSensor?.type
                if (event != null && sensorType != null && event.sensor.type == sensorType) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val pitch = orientationAngles[1]
                    val roll = orientationAngles[2]
                    val maxTiltAngleRad = Math.toRadians(25.0).toFloat()
                    val normalizedPitch = (pitch / maxTiltAngleRad).coerceIn(-1f, 1f)
                    val normalizedRoll = (roll / maxTiltAngleRad).coerceIn(-1f, 1f)
                    parallaxOffsetY = -normalizedPitch * maxParallaxOffsetPx
                    parallaxOffsetX = -normalizedRoll * maxParallaxOffsetPx
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(sensorEventListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            if (rotationSensor != null) {
                sensorManager.unregisterListener(sensorEventListener)
            }
        }
    }
    // --- background image with overflow and parallax
    Image(
        painter = painterResource(id = imageRes),
        contentDescription = "Background Image",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = parallaxOffsetX
                translationY = parallaxOffsetY
                this.scaleX = scale
                this.scaleY = scale
            }
    )
}
