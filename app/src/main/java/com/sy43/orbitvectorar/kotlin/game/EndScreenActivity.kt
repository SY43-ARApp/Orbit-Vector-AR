package com.sy43.orbitvectorar.kotlin.game

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
import com.sy43.orbitvectorar.kotlin.game.data.ApiService
import com.sy43.orbitvectorar.kotlin.game.data.UserPreferences
import com.sy43.orbitvectorar.kotlin.game.ui.theme.DisketFont
import com.sy43.orbitvectorar.kotlin.game.ui.theme.OrbitVectorARTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.sy43.orbitvectorar.R

class EndScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score = intent.getIntExtra("score", 0)
        val points = intent.getIntExtra("points", 0)
        val arrowsThrown = intent.getIntExtra("arrowsThrown", 0)
        val objectsHit = intent.getIntExtra("objectsHit", 0)
        val levelsPassed = intent.getIntExtra("levelsPassed", 0)
        AudioManager.playSfx("gameover")

        // --- Send score to server ---
        val prefs = UserPreferences(this)
        val uuid = prefs.uuid
        val api = ApiService.Companion.run {
            val moshi = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
            retrofit2.Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
                .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi).asLenient())
                .build()
                .create(ApiService::class.java)
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.sendScore(
                    uuid = uuid,
                    score = points,
                    arrowsThrown = arrowsThrown,
                    planetsHit = objectsHit,
                    levelsPassed = levelsPassed
                )
            } catch (_: Exception) { }
        }

        setContent {
            OrbitVectorARTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EndScreen(
                        score = score,
                        points = points,
                        arrowsThrown = arrowsThrown,
                        objectsHit = objectsHit,
                        levelsPassed = levelsPassed,
                        onHome = {
                            startActivity(Intent(this, MenuScreenActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EndScreen(
    score: Int,
    points: Int,
    arrowsThrown: Int = 0,
    objectsHit: Int = 0,
    levelsPassed: Int = 0,
    onHome: () -> Unit
) {
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

        // --- Score, Points, and Stats
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SCORE :  $points",
                style = TextStyle(fontFamily = font, fontSize = 32.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ARROWS THROWN: $arrowsThrown",
                style = TextStyle(fontFamily = font, fontSize = 22.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = "OBJECTS HIT: $objectsHit",
                style = TextStyle(fontFamily = font, fontSize = 22.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 2.dp)
            )
            Text(
                text = "LEVELS PASSED: $levelsPassed",
                style = TextStyle(fontFamily = font, fontSize = 22.sp, color = Color.White),
                modifier = Modifier.padding(vertical = 2.dp)
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
                .clickable {  
                    AudioManager.playSfx("tap")
                    onHome() 
                }
        )
    }
}
