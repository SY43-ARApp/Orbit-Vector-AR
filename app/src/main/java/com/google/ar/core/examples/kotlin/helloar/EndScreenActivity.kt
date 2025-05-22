package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.collection.mutableScatterMapOf
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.ar.core.examples.kotlin.helloar.data.ApiService
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        val api = ApiService.run {
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
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B1A36)) {
                    EndScreen(
                        score = score,
                        points = points,
                        arrowsThrown = arrowsThrown,
                        objectsHit = objectsHit,
                        levelsPassed = levelsPassed,
                        onHome = {
                            startActivity(Intent(this, MenuScreenActivity::class.java))
                            finish()
                        },
                        onPlayAgain = {
                            startActivity(Intent(this, HelloArActivity::class.java))
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
    onHome: () -> Unit,
    onPlayAgain: () -> Unit
) {
    val font = DisketFont
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1A36))
    ) {
        // Parallax background
        ParallaxBackground()

        // Top bar: title only
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Title background image
                Image(
                    painter = painterResource(id = R.drawable.ui_game_finished),
                    contentDescription = "Game Finished Title Background",
                    modifier = Modifier
                        .fillMaxSize()
                )
                // Title text
                Text(
                    text = "GAME OVER",
                    style = TextStyle(
                        fontFamily = font,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                )
            }
        }

        // Card container
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E3B70))
                .border(
                    width = 2.dp,
                    color = Color(0xFF4D76CF),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Stats summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatBox(
                        label = "SCORE",
                        value = points.toString(),
                        fontFamily = font,
                        color = Color(0xFF90CAF9)
                    )
                    StatBox(
                        label = "ARROWS THROWN",
                        value = arrowsThrown.toString(),
                        fontFamily = font,
                        color = Color(0xFFFFB74D)
                    )
                    StatBox(
                        label = "PLANETS HIT",
                        value = objectsHit.toString(),
                        fontFamily = font,
                        color = Color(0xFFB39DDB)
                    )
                    StatBox(
                        label = "LEVELS PASSED",
                        value = levelsPassed.toString(),
                        fontFamily = font,
                        color = Color(0xFF81C784)
                    )
                }

                // Separator
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    thickness = 2.dp,
                    color = Color(0xFF4D76CF)
                )

                // Badges section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Section title
                    Text(
                        text = "BADGES EARNED",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9),
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Badge categories row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Calculate accuracy
                        val accuracy = if (arrowsThrown > 0) levelsPassed.toFloat() / arrowsThrown.toFloat() else 0f

                        // Accuracy Badge
                        BadgeObtained(
                            categoryIcon = R.drawable.accuracy,
                            categoryName = "ACCURACY",
                            badgeLevel = getBadgeLevelForAccuracy(accuracy),
                            fontFamily = font,
                            color = Color(0xFF90CAF9)
                        )

                        // Arrows Shot Badge
                        BadgeObtained(
                            categoryIcon = R.drawable.arrows_shot,
                            categoryName = "ARROWS",
                            badgeLevel = getBadgeLevelForArrows(arrowsThrown),
                            fontFamily = font,
                            color = Color(0xFFFFB74D)
                        )

                        // Levels Completed Badge
                        BadgeObtained(
                            categoryIcon = R.drawable.level_completed,
                            categoryName = "LEVELS",
                            badgeLevel = getBadgeLevelForLevels(levelsPassed),
                            fontFamily = font,
                            color = Color(0xFF81C784)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge level descriptions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF142C57))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF4D76CF),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = "BADGE PROGRESSION",
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                for (i in 0..5) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Image(
                                            painter = painterResource(id = getBadgeImageResource(i)),
                                            contentDescription = "Badge Level $i",
                                            modifier = Modifier.size(30.dp)
                                        )
                                        Text(
                                            text = when(i) {
                                                0 -> "NONE"
                                                1 -> "OKAY"
                                                2 -> "GOOD"
                                                3 -> "GREAT"
                                                4 -> "EXCELLENT"
                                                else -> "PERFECT"
                                            },
                                            fontFamily = font,
                                            fontSize = 8.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Home button
            Image(
                painter = painterResource(id = R.drawable.ui_home),
                contentDescription = "Home Button",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        AudioManager.playSfx("tap")
                        onHome()
                    }
            )

            // Play Again button
            Image(
                painter = painterResource(id = R.drawable.ui_playbutton),
                contentDescription = "Play Again Button",
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        AudioManager.playSfx("titletap")
                        onPlayAgain()
                    }
            )

        }
    }
}

// Badge component
@Composable
fun BadgeObtained(
    categoryIcon: Int,
    categoryName: String,
    badgeLevel: Int,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Category name
        Text(
            text = categoryName,
            fontFamily = fontFamily,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Badge
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Badge image
            Image(
                painter = painterResource(id = getBadgeImageResource(badgeLevel)),
                contentDescription = "Badge Level $badgeLevel",
                modifier = Modifier.size(70.dp)
            )
            
            // Badge icon
            Image(
                painter = painterResource(id = categoryIcon),
                contentDescription = "$categoryName Icon",
                modifier = Modifier
                    .size(40.dp)
            )
        }
    }
}


// Badge logic
private fun getBadgeLevelForAccuracy(accuracy: Float): Int {
    return when {
        accuracy <= 0f -> 0
        accuracy < 0.1f -> 1
        accuracy < 0.2f -> 2
        accuracy < 0.3f -> 3
        accuracy < 0.5f -> 4
        else -> 5
    }
}

private fun getBadgeLevelForArrows(arrowsThrown: Int): Int {
    return when {
        arrowsThrown <= 0 -> 0
        arrowsThrown < 10 -> 1
        arrowsThrown < 20 -> 2
        arrowsThrown < 30 -> 3
        arrowsThrown < 50 -> 4
        else -> 5
    }
}

private fun getBadgeLevelForLevels(levelsPassed: Int): Int {
    return when {
        levelsPassed <= 0 -> 0
        levelsPassed == 1 -> 1
        levelsPassed == 2 -> 2
        levelsPassed == 3 -> 3
        levelsPassed == 4 -> 4
        else -> 5
    }
}

private fun getBadgeImageResource(level: Int): Int {
    return when (level) {
        0 -> R.drawable.badge_0
        1 -> R.drawable.badge_1
        2 -> R.drawable.badge_2
        3 -> R.drawable.badge_3
        4 -> R.drawable.badge_4
        else -> R.drawable.badge_5
    }
}
