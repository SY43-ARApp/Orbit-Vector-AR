package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.google.ar.core.examples.kotlin.helloar.data.GlobalScore
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import kotlinx.coroutines.launch

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
            .baseUrl(ApiService.BASE_URL)
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
        // Header with home button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            Image(
                painter = painterResource(id = R.drawable.ui_home),
                contentDescription = "Home Button",
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onHome() }
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
                    contentDescription = "Leaderboard Title Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (13.dp))
                )

                // Title text
                Text(
                    text = "LEADERBOARD",
                    style = TextStyle(
                        fontFamily = font,
                        fontSize = 36.sp,
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

        // Leaderboard container
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
            // Table section
            Column(
                modifier = Modifier
                    .fillMaxWidth()

            ) {
                // Header for leaderboard
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x331A237E))
                        .padding(top = 12.dp, bottom = 10.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = " RANK",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(2.2f),
                        maxLines = 1
                    )
                    Text(
                        text = "USERNAME",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(2.2f),
                        maxLines = 1
                    )
                    Text(
                        text = "SCORE",
                        fontFamily = font,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(2.2f),
                        maxLines = 1
                    )
                }

                // Leaderboard list
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
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
                        val fontWeight =
                            if (isPlayer || isTop3) FontWeight.Bold else FontWeight.Normal

                        val baseFontSize = 20.sp
                        val minFontSize = 14.sp
                        val maxChars = 8
                        val adaptiveFontSize = if (usernameText.length > maxChars)
                            (baseFontSize.value * (maxChars.toFloat() / usernameText.length.toFloat())).coerceAtLeast(
                                minFontSize.value
                            )
                        else baseFontSize.value

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp))
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
                                    painter = painterResource(
                                        id = RankImageUtil.getRankImageRes(
                                            idx + 1,
                                            leaderboard.size
                                        )
                                    ),
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
            }
        }
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
