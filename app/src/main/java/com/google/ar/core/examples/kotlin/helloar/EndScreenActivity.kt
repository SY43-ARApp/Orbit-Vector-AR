package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme

class EndScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score = intent.getIntExtra("score", 0)
        val points = intent.getIntExtra("points", 0)
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EndScreen(score = score, points = points, onMenu = {
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun EndScreen(score: Int, points: Int, onMenu: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Game Over!", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Score: $score", modifier = Modifier.padding(8.dp))
        Text(text = "Points: $points", modifier = Modifier.padding(8.dp))
        Button(onClick = onMenu, modifier = Modifier.padding(16.dp)) {
            Text(text = "Back to Menu")
        }
    }
}
