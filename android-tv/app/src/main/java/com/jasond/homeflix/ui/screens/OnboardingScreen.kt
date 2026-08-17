package com.jasond.homeflix.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.jasond.homeflix.R
import com.jasond.homeflix.data.remote.ApiClient
import com.jasond.homeflix.ui.theme.HomeFlixColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.net.URI

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var animateIn by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (animateIn) 1f else .68f, tween(850), label = "logo scale")
    val alpha by animateFloatAsState(if (animateIn) 1f else 0f, tween(650), label = "logo alpha")
    LaunchedEffect(Unit) {
        animateIn = true
        delay(1_750)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize().background(Brush.radialGradient(
            listOf(HomeFlixColors.BackgroundLight, HomeFlixColors.Background, HomeFlixColors.BackgroundDark))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale).alpha(alpha)) {
            Image(painterResource(R.drawable.ds_brand_logo), "Delos Santos Cinema logo", Modifier.size(210.dp))
            Text("DELOS SANTOS PERSONAL CINEMA", color = HomeFlixColors.TextSecondary, fontSize = 13.sp,
                letterSpacing = 2.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun ServerSetupScreen(initialAddress: String = "", onConnected: (String) -> Unit) {
    var address by remember { mutableStateOf(initialAddress.removePrefix("http://").removePrefix("https://").trimEnd('/')) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val inputFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var inputFocused by remember { mutableStateOf(false) }

    fun leaveInput() {
        keyboard?.hide()
        if (!focusManager.moveFocus(FocusDirection.Down)) focusManager.clearFocus(force = true)
    }
    fun connect() {
        val normalized = normalizeServerAddress(address)
        if (normalized == null) {
            error = "Enter a valid server IP address."
            return
        }
        keyboard?.hide()
        checking = true
        error = null
        scope.launch {
            runCatching {
                val response = withTimeout(6_000) { ApiClient.createService(normalized).health() }
                check(response.status == "ok" && response.service == "homeflix")
            }.onSuccess {
                ApiClient.configure(normalized)
                onConnected(normalized)
            }.onFailure {
                error = "Unable to connect. Check the IP address and FastAPI server."
                checking = false
            }
        }
    }
    BackHandler(enabled = inputFocused, onBack = ::leaveInput)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        inputFocus.requestFocus()
    }

    Box(
        Modifier.fillMaxSize().background(Brush.horizontalGradient(
            listOf(HomeFlixColors.BackgroundLight, HomeFlixColors.Background, HomeFlixColors.BackgroundDark))),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 120.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(.72f), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.ds_brand_logo), "Delos Santos Cinema logo", Modifier.size(190.dp))
            }
            Spacer(Modifier.width(90.dp))
            Column(Modifier.weight(1.28f)) {
                BasicTextField(
                    value = address,
                    onValueChange = { address = it.trim(); error = null },
                    singleLine = true,
                    enabled = !checking,
                    textStyle = TextStyle(color = HomeFlixColors.TextPrimary, fontSize = 21.sp),
                    cursorBrush = SolidColor(HomeFlixColors.Brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { leaveInput() }),
                    modifier = Modifier.fillMaxWidth().height(62.dp)
                        .focusRequester(inputFocus)
                        .onFocusChanged { inputFocused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                                leaveInput(); true
                            } else false
                        }
                        .background(HomeFlixColors.SurfaceRaised, RoundedCornerShape(8.dp))
                        .border(2.dp, if (inputFocused) Color.White else Color(0xFF5B5B63), RoundedCornerShape(8.dp))
                        .padding(horizontal = 20.dp, vertical = 15.dp),
                    decorationBox = { field -> Box {
                        if (address.isBlank()) Text("Please enter Server address",
                            color = HomeFlixColors.TextSecondary, fontSize = 19.sp)
                        field()
                    } },
                )
                error?.let { Text(it, color = Color(0xFFFF8A8F), fontSize = 15.sp,
                    modifier = Modifier.padding(top = 14.dp)) }
                Button(
                    onClick = ::connect,
                    enabled = address.isNotBlank() && !checking,
                    colors = ButtonDefaults.colors(
                        containerColor = HomeFlixColors.Brand,
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = .7f),
                    ),
                    modifier = Modifier.padding(top = 24.dp),
                ) { Text(if (checking) "TESTING CONNECTION..." else "CONNECT", color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
            }
        }
    }
}

internal fun normalizeServerAddress(input: String): String? = runCatching {
    val raw = input.trim().trimEnd('/')
    if (raw.isBlank()) return null
    val withScheme = if (raw.contains("://")) raw else "http://$raw"
    val uri = URI(withScheme)
    if (uri.host.isNullOrBlank()) return null
    val port = if (uri.port == -1) 8000 else uri.port
    URI(uri.scheme ?: "http", null, uri.host, port, "/", null, null).toString()
}.getOrNull()
