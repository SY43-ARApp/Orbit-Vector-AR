package com.sy43.orbitvectorar.kotlin.game

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sy43.orbitvectorar.kotlin.game.data.UserPreferences
import com.sy43.orbitvectorar.kotlin.game.ui.theme.DisketFont
import com.sy43.orbitvectorar.kotlin.game.ui.theme.OrbitVectorARTheme
import com.sy43.orbitvectorar.R
class TitleScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AudioManager.init(this)

        // first time user or not registered
        val prefs = UserPreferences(this)
        val hasUsername = !prefs.username.isNullOrBlank()
        val hasUuid = !prefs.uuid.isNullOrBlank()
        if (!hasUsername || !hasUuid) {
            startActivity(Intent(this, FirstTimeScreenActivity::class.java))
            finish()
            return
        }

        setContent {
            OrbitVectorARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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