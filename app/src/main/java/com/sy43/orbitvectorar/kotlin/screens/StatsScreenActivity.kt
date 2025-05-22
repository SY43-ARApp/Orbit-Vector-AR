package com.sy43.orbitvectorar.kotlin.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sy43.orbitvectorar.R
import com.sy43.orbitvectorar.kotlin.utils.AudioManager
import com.sy43.orbitvectorar.kotlin.game.ParallaxBackground
import com.sy43.orbitvectorar.kotlin.data.ApiService
import com.sy43.orbitvectorar.kotlin.data.UserPreferences
import com.sy43.orbitvectorar.kotlin.data.UserScore
import com.sy43.orbitvectorar.kotlin.theme.DisketFont
import com.sy43.orbitvectorar.kotlin.theme.OrbitVectorARTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(onMenu: () -> Unit = {}) {
    val context = LocalContext.current
    val font = DisketFont
    val prefs = remember { UserPreferences(context) }
    val uuid = prefs.uuid

    val api = remember {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        retrofit2.Retrofit.Builder()
            .baseUrl(ApiService.Companion.BASE_URL)
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ApiService::class.java)
    }

    var userScores by remember { mutableStateOf<List<UserScore>>(emptyList()) }
    var totalGames by remember { mutableStateOf(0) }
    var totalPlanetsHit by remember { mutableStateOf(0) }
    var totalLevelsPassed by remember { mutableStateOf(0) }
    var totalArrowsThrown by remember { mutableStateOf(0) }

    // Fetch user scores
    LaunchedEffect(uuid) {
        try {
            val resp = api.getUserScores(uuid)
            if (resp.isSuccessful) {
                val scores = resp.body()
                if (scores != null) {
                    userScores = scores
                    totalGames = scores.size
                    totalPlanetsHit = scores.sumOf { it.planets_hit ?: 0 }
                    totalLevelsPassed = scores.sumOf { it.levels_passed ?: 0 }
                    totalArrowsThrown = scores.sumOf { it.arrows_thrown ?: 0 }
                } else {
                    userScores = emptyList()
                    totalGames = 0
                    totalPlanetsHit = 0
                    totalLevelsPassed = 0
                    totalArrowsThrown = 0
                }
            } else {
                userScores = emptyList()
                totalGames = 0
                totalPlanetsHit = 0
                totalLevelsPassed = 0
                totalArrowsThrown = 0
            }
        } catch (e: Exception) {
            userScores = emptyList()
            totalGames = 0
            totalPlanetsHit = 0
            totalLevelsPassed = 0
            totalArrowsThrown = 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1A36))
    ) {
        ParallaxBackground()

        // Top: Stats title image
        Image(
            painter = painterResource(id = R.drawable.stats_title),
            contentDescription = "Stats Title",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .size(width = 380.dp, height = 80.dp)
        )

        // Panel background
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp, bottom = 90.dp)
                .fillMaxWidth(0.96f)
                .background(Color(0xFF101B3A), shape = RoundedCornerShape(28.dp))
        )

        // Stats summary
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL GAMES PLAYED: $totalGames",
                fontFamily = font,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TOTAL PLANETS HIT: $totalPlanetsHit",
                fontFamily = font,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TOTAL APPLES HIT: $totalLevelsPassed",
                fontFamily = font,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TOTAL ARROWS THROWN: $totalArrowsThrown",
                fontFamily = font,
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Table header
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 208.dp, start = 18.dp, end = 18.dp)
                .fillMaxWidth()
                .background(Color(0x331A237E), shape = RoundedCornerShape(8.dp))
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DATE",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                modifier = Modifier.weight(2.2f),
                maxLines = 1
            )
            Text(
                text = "SCORE",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                modifier = Modifier.weight(1.2f),
                maxLines = 1
            )
            Text(
                text = "ARROWS",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                modifier = Modifier.weight(1.2f),
                maxLines = 1
            )
            Text(
                text = "PLANET",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                modifier = Modifier.weight(1.2f),
                maxLines = 1
            )
            Text(
                text = "APPLES",
                fontFamily = font,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
                modifier = Modifier.weight(1.2f),
                maxLines = 1
            )
        }

        // Table rows
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 238.dp, bottom = 100.dp, start = 18.dp, end = 18.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            itemsIndexed(userScores) { idx, entry ->
                val rowBgColor = if (idx % 2 == 0) Color(0x22000000) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(rowBgColor)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Format date as MM/dd/yy
                    val formattedDate = remember(entry.time) {
                        try {
                            val input = LocalDateTime.parse(entry.time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            input.format(DateTimeFormatter.ofPattern("MM/dd/yy"))
                        } catch (e: Exception) {
                            entry.time.take(10)
                        }
                    }
                    Text(
                        text = formattedDate,
                        fontFamily = font,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(2.2f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = entry.actualScore.toString(),
                        fontFamily = font,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        text = (entry.arrows_thrown ?: 0).toString(),
                        fontFamily = font,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        text = (entry.planets_hit ?: 0).toString(),
                        fontFamily = font,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                    Text(
                        text = (entry.levels_passed ?: 0).toString(),
                        fontFamily = font,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1.2f),
                        maxLines = 1
                    )
                }
            }
        }

        // Menu button
        Image(
            painter = painterResource(id = R.drawable.ui_home),
            contentDescription = "Menu Button",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(72.dp)
                .clickable { onMenu() }
        )
    }
}

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                StatsScreen(
                    onMenu = {
                        AudioManager.playSfx("tap")
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
