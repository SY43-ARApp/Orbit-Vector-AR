package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import androidx.compose.ui.zIndex

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
    // font
    val font = DisketFont

    // music/sfx state
    var musicEnabled by remember { mutableStateOf(AudioManager.isMusicEnabled()) }
    var sfxEnabled by remember { mutableStateOf(AudioManager.isSfxEnabled()) }

    // --- spin animation sync key ---
    var spinAnimKey by remember { mutableStateOf(0) }

    // --- anim: logo pulse ---
    val logoScale by rememberInfiniteTransition(label = "logoPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ), label = "logoScale"
    )

    // --- anim: play button pulse (exaggerated, only play) ---
    val playButtonPulse by rememberInfiniteTransition(label = "buttonPulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ), label = "buttonPulse"
    )

    // --- anim: button press scale (additive) ---
    val playButtonPress = remember { Animatable(1f) }
    val leaderboardPress = remember { Animatable(1f) }
    val battlePassPress = remember { Animatable(1f) }

    // --- anim: button rotation ---
    val buttonRotation = remember { Animatable(0f) }

    // --- anim: spin all buttons every 8.6s ---
    LaunchedEffect(spinAnimKey) {
        while (true) {
            delay(8657)
            buttonRotation.animateTo(
                360f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
            buttonRotation.snapTo(0f)
            delay(150)
        }
    }

    // --- loading overlay state ---
    var showLoading by remember { mutableStateOf(false) }
    var loadingAlpha by remember { mutableStateOf(0f) }
    var loadingProgress by remember { mutableStateOf(0f) }

    // --- anim: fade to black ---
    val fadeAlpha by animateFloatAsState(
        targetValue = if (showLoading) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "fadeAlpha"
    )

    val context = LocalContext.current

    // --- fix: trigger VFX and scene change on tap, ensure press VFX always works ---
    // Use a local lambda for play to ensure coroutine scope and VFX
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- parallax ---
        ParallaxBackground()
        // --- bg music ---
        DisposableEffect(musicEnabled) {
            if (musicEnabled) {
                AudioManager.playBackground(R.raw.menubgmusic)
                spinAnimKey++
            } else {
                AudioManager.stopBackground()
            }
            onDispose { }
        }

        // --- gamertag ---
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

        // --- music/sfx toggles ---
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
                            spinAnimKey++
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

        // --- logo ---
        Image(
            painter = painterResource(id = R.drawable.app_title),
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .size(260.dp)
                .graphicsLayer(
                    scaleX = logoScale,
                    scaleY = logoScale
                )
        )

        // --- main menu buttons ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- play ---
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(
                        scaleX = playButtonPulse * playButtonPress.value,
                        scaleY = playButtonPulse * playButtonPress.value,
                        rotationZ = buttonRotation.value
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                playButtonPress.animateTo(1.25f, animationSpec = tween(120))
                                tryAwaitRelease()
                                playButtonPress.animateTo(1f, animationSpec = tween(120))
                            },
                            onTap = {
                                // VFX and loading overlay
                                showLoading = true
                                scope.launch {
                                    // Simulate loading progress (replace with real if possible)
                                    for (i in 1..10) {
                                        loadingProgress = i / 10f
                                        kotlinx.coroutines.delay(80)
                                    }
                                    loadingProgress = 1f
                                    kotlinx.coroutines.delay(200)
                                    AudioManager.stopBackground()
                                    AudioManager.playSfx("titletap")
                                    context.startActivity(Intent(context, HelloArActivity::class.java))
                                    if (context is ComponentActivity) context.finish()
                                }
                            }
                        )
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_playbutton),
                    contentDescription = "Play Button",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            // --- leaderboard & battlepass ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // --- leaderboard ---
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(
                            scaleX = leaderboardPress.value,
                            scaleY = leaderboardPress.value,
                            rotationZ = buttonRotation.value
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    leaderboardPress.animateTo(1.25f, animationSpec = tween(120))
                                    tryAwaitRelease()
                                    leaderboardPress.animateTo(1f, animationSpec = tween(120))
                                },
                                onTap = { onLeaderboard() }
                            )
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ui_leaderboard),
                        contentDescription = "Leaderboard Button",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // --- battlepass ---
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer(
                            scaleX = battlePassPress.value,
                            scaleY = battlePassPress.value,
                            rotationZ = buttonRotation.value
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    battlePassPress.animateTo(1.25f, animationSpec = tween(120))
                                    tryAwaitRelease()
                                    battlePassPress.animateTo(1f, animationSpec = tween(120))
                                },
                                onTap = { onBattlePass() }
                            )
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ui_battlepass),
                        contentDescription = "BattlePass Button",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // --- loading overlay ---
        if (showLoading || fadeAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = fadeAlpha))
                    .zIndex(10f)
            ) {
                if (fadeAlpha > 0.8f) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Loading...",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontFamily = DisketFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                        )
                        // Progress %
                        Text(
                            text = "${(loadingProgress * 100).toInt()}%",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 22.sp,
                            fontFamily = DisketFont
                        )
                    }
                }
            }
        }
    }
}
