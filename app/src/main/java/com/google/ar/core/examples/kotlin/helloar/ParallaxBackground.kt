package com.google.ar.core.examples.kotlin.helloar

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import com.google.ar.core.examples.kotlin.helloar.R

@Composable
fun ParallaxBackground(
    modifier: Modifier = Modifier,
    imageRes: Int = R.drawable.ui_menu_bg,
    maxParallaxOffsetDp: Int = 24,
    scale: Float = 1.15f
) {
    // --- parallax 
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val rotationSensor = remember(sensorManager) {
        sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(android.hardware.Sensor.TYPE_ROTATION_VECTOR)
    }
    var parallaxOffsetX by remember { mutableStateOf(0f) }
    var parallaxOffsetY by remember { mutableStateOf(0f) }
    val maxParallaxOffsetPx = with(LocalDensity.current) { maxParallaxOffsetDp.dp.toPx() }
    DisposableEffect(sensorManager, rotationSensor) {
        val sensorEventListener = object : android.hardware.SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationAngles = FloatArray(3)
            override fun onSensorChanged(event: android.hardware.SensorEvent?) {
                val sensorType = rotationSensor?.type
                if (event != null && sensorType != null && event.sensor.type == sensorType) {
                    android.hardware.SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    android.hardware.SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val pitch = orientationAngles[1]
                    val roll = orientationAngles[2]
                    val maxTiltAngleRad = Math.toRadians(25.0).toFloat()
                    val normalizedPitch = (pitch / maxTiltAngleRad).coerceIn(-1f, 1f)
                    val normalizedRoll = (roll / maxTiltAngleRad).coerceIn(-1f, 1f)
                    parallaxOffsetY = -normalizedPitch * maxParallaxOffsetPx
                    parallaxOffsetX = -normalizedRoll * maxParallaxOffsetPx
                }
            }
            override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) { }
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(sensorEventListener, rotationSensor, android.hardware.SensorManager.SENSOR_DELAY_GAME)
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
