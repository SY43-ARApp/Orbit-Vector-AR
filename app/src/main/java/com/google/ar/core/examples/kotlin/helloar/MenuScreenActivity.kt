package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.examples.kotlin.helloar.R
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer

class MenuScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MenuScreen(onPlay = {
                        AudioManager.stopBackground()
                        AudioManager.playSfx("titletap")
                        startActivity(Intent(this, HelloArActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun MenuScreen(
    onPlay: () -> Unit = { },
    onLeaderboard: () -> Unit = { AudioManager.playSfx("tap") },
    onBattlePass: () -> Unit = { AudioManager.playSfx("tap") }
) {
    // --- font
    val font = DisketFont

    // --- music/sfx state
    var musicEnabled by remember { mutableStateOf(AudioManager.isMusicEnabled()) }
    var sfxEnabled by remember { mutableStateOf(AudioManager.isSfxEnabled()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- parallax
        ParallaxBackground()
        // Only play if enabled
        DisposableEffect(musicEnabled) {
            if (musicEnabled) {
                AudioManager.playBackground(R.raw.menubgmusic)
            } else {
                AudioManager.stopBackground()
            }
            onDispose { }
        }

        // --- gamertag area
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ui_playerprofile),
                contentDescription = "Gamertag Icon",
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "GAMERTAG",
                    style = TextStyle(fontFamily = font, fontSize = 20.sp),
                    color = Color.White
                )
                Text(
                    text = "# 2D54FS64FDFDSDFS",
                    style = TextStyle(fontFamily = font, fontSize = 12.sp),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // --- top right music/sfx toggle buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Music toggle button
            Image(
                painter = painterResource(
                    id = if (musicEnabled) R.drawable.ic_music_on else R.drawable.ic_music_off
                ),
                contentDescription = "Toggle Music",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        val newState = !musicEnabled
                        AudioManager.setMusicEnabled(newState)
                        musicEnabled = newState
                        if (newState) {
                            AudioManager.playBackground(R.raw.menubgmusic)
                        } else {
                            AudioManager.stopBackground()
                        }
                    }
            )
            // --- SFX toggle button
            Image(
                painter = painterResource(
                    id = if (sfxEnabled) R.drawable.ic_sfx_on else R.drawable.ic_sfx_off
                ),
                contentDescription = "Toggle SFX",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        val newState = !sfxEnabled
                        AudioManager.setSfxEnabled(newState)
                        AudioManager.playSfx("tap")
                        sfxEnabled = newState
                    }
            )
        }

        // --- logo
        Image(
            painter = painterResource(id = R.drawable.app_title),
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(260.dp)
        )

        // --- Play, Leaderboard & BattlePass buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Play button
            Image(
                painter = painterResource(id = R.drawable.ui_playbutton),
                contentDescription = "Play Button",
                modifier = Modifier
                    .size(160.dp)
                    .clickable { onPlay() }
            )
            Spacer(modifier = Modifier.height(16.dp))
            // --- Leaderboard & BattlePass buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_leaderboard),
                    contentDescription = "Leaderboard Button",
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { onLeaderboard() }
                )
                Image(
                    painter = painterResource(id = R.drawable.ui_battlepass),
                    contentDescription = "BattlePass Button",
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { onBattlePass() }
                )
            }
        }
    }
}
