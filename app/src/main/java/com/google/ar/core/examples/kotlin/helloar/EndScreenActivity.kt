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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale

import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme


class EndScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score = intent.getIntExtra("score", 0)
        val points = intent.getIntExtra("points", 0)
        AudioManager.playSfx("gameover")
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EndScreen(score = score, points = points, onHome = {
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        AudioManager.playSfx("tap")
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun EndScreen(score: Int, points: Int, onHome: () -> Unit) {
    // --- font
    val font = DisketFont
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- parallax background
        ParallaxBackground()

        // --- banner
        Image(
            painter = painterResource(id = R.drawable.ui_game_finished),
            contentDescription = "Game Finished Banner",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 60.dp)
        )

        // --- Score and Points
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SCORE:  $score",
                style = TextStyle(fontFamily = font, fontSize = 32.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "POINTS :  $points",
                style = TextStyle(fontFamily = font, fontSize = 32.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // --- Home button
        Image(
            painter = painterResource(id = R.drawable.ui_home),
            contentDescription = "Home Button",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .size(120.dp)
                .clickable { onHome() }
        )
    }
}
