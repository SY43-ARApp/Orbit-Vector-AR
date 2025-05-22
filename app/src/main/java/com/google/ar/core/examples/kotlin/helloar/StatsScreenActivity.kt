package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.google.ar.core.examples.kotlin.helloar.data.ApiService
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import com.google.ar.core.examples.kotlin.helloar.data.UserScore
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider

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
            .baseUrl(ApiService.BASE_URL)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            Image(
                painter = painterResource(id = R.drawable.ui_home),
                contentDescription = "Menu Button",
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onMenu() }
            )
            
            // Title
            Box(
                modifier = Modifier
                    .weight(3f)
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background title image
                Image(
                    painter = painterResource(id = R.drawable.page_title),
                    contentDescription = "Stats Title Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (13.dp))
                )
                
                // Title text
                Text(
                    text = "STATS",
                    style = TextStyle(
                        fontFamily = font, 
                        fontSize = 40.sp,
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

        // Stats container
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 140.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .fillMaxHeight()
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
                    // Game stats
                    StatBox(
                        label = "GAMES PLAYED",
                        value = totalGames.toString(),
                        fontFamily = font,
                        color = Color(0xFF90CAF9)
                    )

                    StatBox(
                        label = "PLANETS HIT",
                        value = totalPlanetsHit.toString(),
                        fontFamily = font,
                        color = Color(0xFFB39DDB)
                    )

                    StatBox(
                        label = "APPLES HIT",
                        value = totalLevelsPassed.toString(),
                        fontFamily = font,
                        color = Color(0xFF81C784)
                    )

                    StatBox(
                        label = "ARROWS THROWN",
                        value = totalArrowsThrown.toString(),
                        fontFamily = font,
                        color = Color(0xFFFFB74D)
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

                // Table section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Table header
                    Row(
                        modifier = Modifier
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

                    // Show empty state message or table rows
                    if (userScores.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "NO GAME HISTORY YET",
                                    fontFamily = font,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Play your first game to see your stats here!",
                                    fontFamily = font,
                                    fontSize = 14.sp,
                                    color = Color(0xFFB0BEC5),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    } else {
                        // Table rows
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
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
                                            val input = java.time.LocalDateTime.parse(entry.time, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                            input.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy"))
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
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF142C57))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = fontFamily,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = value,
                fontFamily = fontFamily,
                fontSize = 20.sp,
                color = color,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

class StatsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme {
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
