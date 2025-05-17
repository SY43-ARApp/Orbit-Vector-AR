package com.google.ar.core.examples.kotlin.helloar

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.examples.kotlin.helloar.R
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme

class TitleScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TitleScreen(onTap = {
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun TitleScreen(onTap: () -> Unit) {
    // -------- Animation "Tap to play" text
    val infiniteTransition = rememberInfiniteTransition(label = "tapToPlayFade")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0
                1f at 600
                0.2f at 1200
            },
            repeatMode = RepeatMode.Restart
        ), label = "alphaAnim"
    )

    // -------- parallax
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    var parallaxOffsetX by remember { mutableStateOf(0f) }
    var parallaxOffsetY by remember { mutableStateOf(0f) }

    val maxParallaxOffsetDp = 24.dp
    val maxParallaxOffsetPx = with(LocalDensity.current) { maxParallaxOffsetDp.toPx() }

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

    val font = DisketFont 

    // -------- screen layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() }
    ) {
        // --- bg img
        Image(
            painter = painterResource(id = R.drawable.ui_menu_bg),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .scale(1.15f)
                .graphicsLayer {
                    translationX = parallaxOffsetX
                    translationY = parallaxOffsetY
                }
        )

        // --- app version (from build.gradle)
        val versionName = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: ""
        } catch (e: Exception) { "" }
        if (versionName.isNotEmpty()) {
            Text(
                text = "v$versionName",
                style = TextStyle(fontFamily = font, fontSize = 14.sp),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 8.dp)
            )
        }

        // --- foreground
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- logo
            Spacer(modifier = Modifier.height(80.dp))

            Image(
                painter = painterResource(id = R.drawable.app_title),
                contentDescription = "Title Logo",
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // --- Tap to play text
            Text(
                text = "Tap to play",
                style = TextStyle(fontFamily = font, fontSize = 32.sp),
                color = Color.White,
                modifier = Modifier
                    .alpha(alpha)
                    .padding(bottom = 100.dp)
            )
        }
    }
}