package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.zIndex

class FirstTimeScreenActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    FirstTimeScreen(
                        onRegistered = {
                            startActivity(Intent(this, HelloArActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FirstTimeScreen(onRegistered: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val font = DisketFont

    var username by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var usernameChecked by remember { mutableStateOf(false) }
    var policyChecked by remember { mutableStateOf(false) }
    var termsChecked by remember { mutableStateOf(false) }
    var appSetId by remember { mutableStateOf<String?>(null) }
    var registering by remember { mutableStateOf(false) }
    var registrationError by remember { mutableStateOf<String?>(null) }

    var showLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }

    val api = remember {
        val moshi = com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        retrofit2.Retrofit.Builder()
            .baseUrl(com.google.ar.core.examples.kotlin.helloar.data.ApiService.BASE_URL)
            .addConverterFactory(retrofit2.converter.scalars.ScalarsConverterFactory.create())
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(com.google.ar.core.examples.kotlin.helloar.data.ApiService::class.java)
    }

    LaunchedEffect(Unit) {
        try {
            com.google.android.gms.appset.AppSet.getClient(context).appSetIdInfo
                .addOnSuccessListener { info ->
                    appSetId = info.id
                }
                .addOnFailureListener {
                    appSetId = UUID.randomUUID().toString()
                }
        } catch (e: Exception) {
            appSetId = UUID.randomUUID().toString()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    fun checkUsernameAvailable(name: String) {
        if (name.isBlank()) {
            usernameError = null
            usernameChecked = false
            return
        }
        usernameError = null
        usernameChecked = false
        coroutineScope.launch {
            try {
                Log.d("FirstTimeScreen", "Checking username: $name (via checkUsername API)")
                val resp = api.checkUsername(name)
                Log.d("FirstTimeScreen", "API Response: code=${resp.code()} body=${resp.body()} errorBody=${resp.errorBody()?.string()}")
                if (resp.isSuccessful && resp.body()?.available == true) {
                    usernameError = null
                    usernameChecked = true
                } else if (resp.body()?.available == false) {
                    usernameError = resp.body()?.message ?: "USERNAME ALREADY EXISTING"
                    usernameChecked = false
                } else {
                    usernameError = "Error checking username"
                    usernameChecked = false
                }
            } catch (e: Exception) {
                Log.e("FirstTimeScreen", "Exception during username check", e)
                usernameError = "Error checking username"
                usernameChecked = false
            }
        }
    }

    fun registerUser() {
        if (appSetId.isNullOrBlank() || !usernameChecked || !policyChecked || !termsChecked) return
        registering = true
        registrationError = null
        val prefs = UserPreferences(context)
        coroutineScope.launch {
            try {
                val resp = api.register(appSetId!!, username)
                if (resp?.isSuccessful == true && resp.body()?.startsWith("REGISTERED:") == true) {
                    prefs.username = username
                    prefs.uuid = appSetId!!
                    onRegistered()
                } else if (resp?.body()?.contains("USERNAME_EXISTS") == true) {
                    registrationError = "USERNAME ALREADY EXISTING"
                } else {
                    registrationError = "Registration failed"
                }
            } catch (e: Exception) {
                registrationError = "Registration error"
            }
            registering = false
        }
    }

    // --- anim: fade to black ---
    val fadeAlpha by animateFloatAsState(
        targetValue = if (showLoading) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
        label = "fadeAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1A36))
    ) {
        ParallaxBackground()

        // Logo
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_title),
                contentDescription = "Logo",
                modifier = Modifier.size(220.dp)
            )
        }

        // Welcome text
        Text(
            text = "WELCOME!",
            style = TextStyle(fontFamily = font, fontSize = 38.sp, color = Color.White),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 260.dp)
        )

        // Username input
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_playerprofile),
                    contentDescription = "User Icon",
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            if (it.length <= 10) {
                                username = it
                                usernameChecked = false
                                usernameError = null
                            }
                        },
                        label = { Text("ENTER USERNAME...", fontFamily = font, fontSize = 18.sp) },
                        singleLine = true,
                        isError = usernameError != null,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                checkUsernameAvailable(username)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            errorTextColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }
            }
            if (usernameError != null) {
                Text(
                    text = usernameError ?: "",
                    color = Color.Red,
                    fontFamily = font,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Start).padding(top = 2.dp)
                )
            }
            // --- T&C and Policy checkboxes ---
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = if (termsChecked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                    ),
                    contentDescription = "Terms Checkbox",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { 
                            AudioManager.playSfx("tap") 
                            termsChecked = !termsChecked 
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I ACCEPT ",
                    color = Color.White,
                    fontFamily = font,
                    fontSize = 16.sp,
                )
                Text(
                    text = "TERMS AND CONDITIONS",
                    color = Color(0xFF40BFFF),
                    fontFamily = font,
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        val url = "https://chaelpixserver.ddns.net/apis/ovar/terms.html"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(
                        id = if (policyChecked) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                    ),
                    contentDescription = "Policy Checkbox",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { 
                            AudioManager.playSfx("tap") 
                            AudioManager.playSfx("titletap")
                            policyChecked = !policyChecked 
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I ACCEPT ",
                    color = Color.White,
                    fontFamily = font,
                    fontSize = 16.sp,
                )
                Text(
                    text = "POLICY CONFIDENTIALITY",
                    color = Color(0xFF40BFFF),
                    fontFamily = font,
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        val url = "https://chaelpixserver.ddns.net/apis/ovar/policy.html"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }

        if (usernameChecked && policyChecked && termsChecked) {
            val scope = coroutineScope 
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ui_playbutton),
                    contentDescription = "Play Button",
                    modifier = Modifier
                        .size(160.dp)
                        .clickable(enabled = !registering) {
                            showLoading = true
                            scope.launch {
                                for (i in 1..10) {
                                    loadingProgress = i / 10f
                                    delay(80)
                                }
                                loadingProgress = 1f
                                delay(200)
                                registerUser()
                            }
                        }
                )
            }
        }

        if (registrationError != null) {
            Text(
                text = registrationError ?: "",
                color = Color.Red,
                fontFamily = font,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
            )
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
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
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
