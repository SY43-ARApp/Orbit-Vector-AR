package com.sy43.orbitvectorar.kotlin.game

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sy43.orbitvectorar.kotlin.game.ui.theme.OrbitVectorARTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sy43.orbitvectorar.kotlin.game.data.ApiService
import com.sy43.orbitvectorar.kotlin.game.data.GlobalScore
import com.sy43.orbitvectorar.kotlin.game.data.UserPreferences
import com.sy43.orbitvectorar.kotlin.game.ui.theme.DisketFont
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.sy43.orbitvectorar.R
@Composable
fun LeaderboardScreen(onHome: () -> Unit = {}) {
    val context = LocalContext.current
    val font = DisketFont
    val prefs = remember { UserPreferences(context) }
    val playerName = prefs.username ?: ""
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

    var leaderboard by remember { mutableStateOf<List<GlobalScore>>(emptyList()) }
    var playerIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Fetch leaderboard
    LaunchedEffect(Unit) {
        val resp = api.getGlobalScores(100)
        if (resp.isSuccessful) {
            leaderboard = resp.body() ?: emptyList()
            playerIndex = leaderboard.indexOfFirst { it.username == playerName }.takeIf { it >= 0 } ?: 0
            scope.launch {
                listState.scrollToItem(playerIndex.coerceAtLeast(0))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1A36))
    ) {
        ParallaxBackground()

        // Top: Leaderboard title image
        Image(
            painter = painterResource(id = R.drawable.leaderboard_title),
            contentDescription = "Leaderboard Title",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .size(width = 380.dp, height = 80.dp)
        )

        // Panel background 
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 100.dp, bottom = 90.dp)
                .fillMaxWidth(0.96f)
                .background(Color(0xFF101B3A), shape = RoundedCornerShape(28.dp))
        )

        // Leaderboard list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 146.dp, bottom = 100.dp, start = 18.dp, end = 18.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 0.dp),
        ) {
            itemsIndexed(leaderboard) { idx, entry ->
                val isPlayer = entry.username == playerName
                val isTop3 = idx < 3
                val rowBgColor = when {
                    isPlayer -> Color(0x33FFA726)
                    idx % 2 == 0 -> Color(0x22000000)
                    else -> Color.Transparent
                }
                val rowTextColor = when {
                    isTop3 && idx == 0 -> Color(0xFFFFD700)
                    isTop3 && idx == 1 -> Color(0xFFC0C0C0) 
                    isTop3 && idx == 2 -> Color(0xFFCD7F32)
                    isPlayer -> Color(0xFFFFA726)
                    else -> Color.White
                }
                val scoreColor = rowTextColor
                val usernameText = if (isPlayer) "YOU" else entry.username
                val fontWeight = if (isPlayer || isTop3) FontWeight.Bold else FontWeight.Normal

                val baseFontSize = 20.sp
                val minFontSize = 14.sp
                val maxChars = 14
                val adaptiveFontSize = if (usernameText.length > maxChars)
                    (baseFontSize.value * (maxChars.toFloat() / usernameText.length.toFloat())).coerceAtLeast(minFontSize.value)
                else baseFontSize.value

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(rowBgColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank number
                        Text(
                            text = "#${idx + 1}",
                            fontFamily = font,
                            fontSize = 18.sp,
                            color = rowTextColor,
                            fontWeight = fontWeight,
                            modifier = Modifier.weight(1.1f),
                            maxLines = 1
                        )
                        // Username
                        Text(
                            text = usernameText,
                            fontFamily = font,
                            fontWeight = fontWeight,
                            color = rowTextColor,
                            fontSize = adaptiveFontSize.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(2.7f)
                                .padding(start = 8.dp, end = 8.dp)
                        )
                        // Score
                        Text(
                            text = entry.actualScore.toString(),
                            fontFamily = font,
                            fontWeight = fontWeight,
                            color = scoreColor,
                            fontSize = 18.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(end = 8.dp),
                            softWrap = false
                        )
                        // Badge
                        Image(
                            painter = painterResource(id = RankImageUtil.getRankImageRes(idx + 1, leaderboard.size)),
                            contentDescription = "Rank Badge",
                            modifier = Modifier
                                .size(28.dp)
                                .weight(0.8f)
                                .padding(start = 4.dp)
                        )
                    }
                    // Row separator
                    if (idx < leaderboard.lastIndex) {
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x22FFFFFF))
                        )
                    }
                }
            }
        }

        // Home button
        Image(
            painter = painterResource(id = R.drawable.ui_home),
            contentDescription = "Home Button",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .size(72.dp)
                .clickable {
                    onHome()
                }
        )
    }
}

class LeaderboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                LeaderboardScreen(
                    onHome = {
                        AudioManager.playSfx("tap")
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
