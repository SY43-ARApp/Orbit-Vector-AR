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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.examples.kotlin.helloar.R
import com.google.ar.core.examples.kotlin.helloar.ParallaxBackground
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme

class TitleScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AudioManager.init(this)

        // first time user
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean("first_time", true)
        if (isFirstTime) {
            prefs.edit().putBoolean("first_time", false).apply()
            startActivity(Intent(this, FirstTimeScreenActivity::class.java))
            finish()
            return
        }

        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TitleScreen(onTap = {
                        AudioManager.playSfx("titletap")
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

    val font = DisketFont
    val currentContext = LocalContext.current
    // -------- screen layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() }
    ) {
        // --- parallax
        ParallaxBackground()

        // --- app version (from build.gradle)
        val versionName = try {
            val packageInfo = currentContext.packageManager.getPackageInfo(currentContext.packageName, 0)
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
                modifier = Modifier.size(350.dp)
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