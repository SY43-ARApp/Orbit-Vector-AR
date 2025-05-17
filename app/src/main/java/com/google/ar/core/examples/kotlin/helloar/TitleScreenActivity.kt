package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.google.ar.core.examples.kotlin.helloar.R
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import com.google.ar.core.examples.kotlin.helloar.MenuScreenActivity
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import androidx.compose.ui.draw.alpha

class TitleScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TitleScreen(onTap = {
                        startActivity(Intent(this, MenuScreenActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@Composable
fun TitleScreen(onTap: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "fade")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.2f at 0
                1f at 600
                0.2f at 1200
            },
            repeatMode = RepeatMode.Restart
        ), label = "fadeAnim"
    )
    val font = DisketFont
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onTap() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Image(
            painter = painterResource(id = R.drawable.app_title),
            contentDescription = "Logo",
            modifier = Modifier.size(220.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "Tap to play",
            style = TextStyle(fontFamily = font, fontSize = 32.sp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(alpha)
        )
    }
}
