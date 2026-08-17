package com.jasond.homeflix.ui.screens

import android.app.Activity
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.PlayerProgressState
import com.jasond.homeflix.ui.PlayerViewModel
import com.jasond.homeflix.ui.player.buildMediaItem
import com.jasond.homeflix.ui.player.seekStepMs
import com.jasond.homeflix.ui.player.seekTarget
import com.jasond.homeflix.ui.theme.HomeFlixColors
import kotlinx.coroutines.delay

private const val PROGRESS_SAVE_INTERVAL_MS = 15_000L
private const val CONTROLS_TIMEOUT_MS = 4_000L

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(movie: Movie?, onBack: () -> Unit, progressViewModel: PlayerViewModel = viewModel()) {
    if (movie == null) {
        PlayerUnavailable(onBack)
        return
    }
    when (val state = progressViewModel.state.collectAsStateWithLifecycle().value) {
        PlayerProgressState.Loading -> PlaybackLoading("Loading your position")
        is PlayerProgressState.Ready -> PlaybackPlayer(movie, state.initialPositionMs, progressViewModel, onBack)
    }
}

@OptIn(UnstableApi::class)
@Suppress("DEPRECATION")
@Composable
private fun PlaybackPlayer(
    movie: Movie,
    initialPositionMs: Long,
    progressViewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var wantsToPlay by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<PlaybackException?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var settingsVisible by remember { mutableStateOf(false) }
    var interactionVersion by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableLongStateOf(initialPositionMs) }
    var durationMs by remember { mutableLongStateOf(C.TIME_UNSET) }
    var seekIndicator by remember { mutableStateOf<String?>(null) }
    var seekIndicatorVersion by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf(false) }
    var focusDestination by remember { mutableStateOf(ControlFocus.PLAY) }
    val surfaceFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val firstSettingFocus = remember { FocusRequester() }

    val player = remember(movie.id, initialPositionMs) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 45_000, 750, 2_000)
            .setBackBuffer(10_000, false)
            .build()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        playbackState = state
                        durationMs = duration
                    }
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        wantsToPlay = playWhenReady
                    }
                    override fun onPlayerError(playerError: PlaybackException) {
                        error = playerError
                        controlsVisible = false
                    }
                })
                setMediaItem(buildMediaItem(movie), initialPositionMs)
                playWhenReady = true
                prepare()
            }
    }

    fun touchControls(requestPlayFocus: Boolean = false) {
        controlsVisible = true
        interactionVersion++
        if (requestPlayFocus) focusDestination = ControlFocus.PLAY
    }
    fun save() = progressViewModel.save(player.currentPosition, player.duration)
    fun exit() {
        if (leaving) return
        leaving = true
        player.pause()
        progressViewModel.saveThen(player.currentPosition, player.duration, onBack)
    }
    fun seek(direction: Int, repeatCount: Int) {
        val amount = seekStepMs(repeatCount) * direction
        player.seekTo(seekTarget(player.currentPosition, player.duration, amount))
        positionMs = player.currentPosition
        seekIndicator = if (amount > 0) "+${amount / 1_000}s" else "${amount / 1_000}s"
        seekIndicatorVersion++
        touchControls()
    }

    LaunchedEffect(player, controlsVisible) {
        while (true) {
            positionMs = player.currentPosition
            durationMs = player.duration
            delay(if (controlsVisible) 250 else 1_000)
        }
    }
    LaunchedEffect(interactionVersion, controlsVisible, settingsVisible, wantsToPlay) {
        if (controlsVisible && !settingsVisible && wantsToPlay) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
            surfaceFocus.requestFocus()
        }
    }
    LaunchedEffect(seekIndicatorVersion) {
        if (seekIndicator != null) {
            delay(900)
            seekIndicator = null
        }
    }
    LaunchedEffect(controlsVisible, settingsVisible, focusDestination) {
        if (!controlsVisible && !settingsVisible) return@LaunchedEffect
        withFrameNanos { }
        when {
            settingsVisible -> firstSettingFocus.requestFocus()
            focusDestination == ControlFocus.SETTINGS -> settingsFocus.requestFocus()
            else -> playFocus.requestFocus()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(PROGRESS_SAVE_INTERVAL_MS)
            if (player.isPlaying) save()
        }
    }
    DisposableEffect(player, lifecycleOwner) {
        var resumePlayback = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> save()
                Lifecycle.Event.ON_STOP -> {
                    save()
                    resumePlayback = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_START -> if (resumePlayback) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            save()
            playerView?.player = null
            player.release()
        }
    }
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val decor = activity?.window?.decorView
        val previousVisibility = decor?.systemUiVisibility ?: 0
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        decor?.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            decor?.systemUiVisibility = previousVisibility
        }
    }

    BackHandler {
        when {
            settingsVisible -> {
                settingsVisible = false
                focusDestination = ControlFocus.SETTINGS
                touchControls()
            }
            else -> exit()
        }
    }
    fun handleBack() {
        if (settingsVisible) {
            settingsVisible = false
            focusDestination = ControlFocus.SETTINGS
            touchControls()
        } else exit()
    }
    LaunchedEffect(movie.id) {
        surfaceFocus.requestFocus()
        touchControls(requestPlayFocus = true)
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyUp) handleBack()
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown || error != null) return@onPreviewKeyEvent false
                val repeat = event.nativeKeyEvent.repeatCount
                when (event.key) {
                    Key.DirectionLeft -> if (!settingsVisible) { seek(-1, repeat); true } else false
                    Key.DirectionRight -> if (!settingsVisible) { seek(1, repeat); true } else false
                    Key.DirectionUp, Key.DirectionDown -> if (!controlsVisible) {
                        touchControls(requestPlayFocus = true); true
                    } else false
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter -> if (!controlsVisible) {
                        touchControls(requestPlayFocus = true); true
                    } else false
                    Key.MediaPlayPause -> {
                        if (player.playWhenReady) player.pause() else player.play()
                        touchControls(requestPlayFocus = true); true
                    }
                    else -> false
                }
            }
            .focusRequester(surfaceFocus).focusable(),
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    this.player = player
                    isFocusable = false
                    isFocusableInTouchMode = false
                    playerView = this
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (playbackState == Player.STATE_BUFFERING && error == null) PlaybackLoading("Buffering", overlay = true)

        AnimatedVisibility(
            visible = controlsVisible && error == null,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(180)),
        ) {
            PlayerControls(
                movie = movie,
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = wantsToPlay,
                playFocus = playFocus,
                settingsFocus = settingsFocus,
                onInteraction = { touchControls() },
                onSeekBack = { seek(-1, 0) },
                onPlayPause = { if (player.playWhenReady) player.pause() else player.play(); touchControls() },
                onSeekForward = { seek(1, 0) },
                onSettings = {
                    settingsVisible = true
                    focusDestination = ControlFocus.FIRST_SETTING
                    interactionVersion++
                },
            )
        }

        seekIndicator?.let { SeekIndicator(it) }

        if (settingsVisible && error == null) {
            PlaybackSettings(
                selectedSpeed = player.playbackParameters.speed,
                firstFocus = firstSettingFocus,
                onSelect = { speed ->
                    player.setPlaybackSpeed(speed)
                    settingsVisible = false
                    focusDestination = ControlFocus.SETTINGS
                    touchControls()
                },
            )
        }

        error?.let {
            PlaybackError(
                onRetry = {
                    error = null
                    player.prepare()
                    player.play()
                    touchControls(requestPlayFocus = true)
                },
                onBack = ::exit,
            )
        }
    }
}

@Composable
private fun PlayerControls(
    movie: Movie,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    playFocus: FocusRequester,
    settingsFocus: FocusRequester,
    onInteraction: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0xE6000000))),
        ),
    ) {
        Text(
            movie.title,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(42.dp),
        )
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 54.dp, vertical = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = Color.White, fontSize = 15.sp)
                Text(formatTime(durationMs), color = Color.White, fontSize = 15.sp)
            }
            PlaybackProgress(positionMs, durationMs)
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerButton("-10s", onSeekBack, Modifier, onInteraction)
                Spacer(Modifier.width(18.dp))
                PlayerButton(if (isPlaying) "Pause" else "Play", onPlayPause,
                    Modifier.focusRequester(playFocus), onInteraction)
                Spacer(Modifier.width(18.dp))
                PlayerButton("+10s", onSeekForward, Modifier, onInteraction)
            }
            PlayerButton(
                "Settings",
                onSettings,
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).focusRequester(settingsFocus),
                onInteraction,
            )
        }
    }
}

@Composable
private fun PlayerButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit,
) {
    Button(onClick = { onInteraction(); onClick() }, modifier = modifier) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
private fun PlaybackProgress(positionMs: Long, durationMs: Long) {
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).background(Color(0xFF67676F), RoundedCornerShape(4.dp))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(progress).background(HomeFlixColors.Brand, RoundedCornerShape(4.dp)))
    }
}

@Composable
private fun PlaybackSettings(selectedSpeed: Float, firstFocus: FocusRequester, onSelect: (Float) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.CenterEnd) {
        Column(
            Modifier.fillMaxHeight().width(320.dp).background(Color(0xF21A1A20)).padding(30.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Playback speed", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            listOf(.75f, 1f, 1.25f, 1.5f).forEachIndexed { index, speed ->
                Button(
                    onClick = { onSelect(speed) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier),
                ) {
                    Text(if (speed == selectedSpeed) "${speedLabel(speed)}  Selected" else speedLabel(speed))
                }
            }
        }
    }
}

@Composable
private fun SeekIndicator(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.background(Color(0xCC16161B), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 30.dp, vertical = 18.dp),
        ) { Text(label, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PlaybackError(onRetry: () -> Unit, onBack: () -> Unit) {
    val retryFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { retryFocus.requestFocus() }
    Box(Modifier.fillMaxSize().background(Color(0xEE09090C)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Unable to play this movie", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Check the HomeFlix server or network connection, then try again.",
                color = Color.LightGray, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 24.dp)) {
                Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) { Text("Retry") }
                Button(onClick = onBack) { Text("Back") }
            }
        }
    }
}

@Composable
private fun PlaybackLoading(message: String, overlay: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "playback loading")
    val dotScales = List(3) { index ->
        transition.animateFloat(
            initialValue = .55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(520, delayMillis = index * 130), RepeatMode.Reverse),
            label = "loading dot $index",
        )
    }
    Box(
        Modifier.fillMaxSize().background(
            if (overlay) HomeFlixColors.Background.copy(alpha = .72f) else HomeFlixColors.Background,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dotScales.forEach { scale ->
                    Box(Modifier.size(15.dp).scale(scale.value).alpha(scale.value)
                        .background(HomeFlixColors.Brand, CircleShape))
                }
            }
            Text(message, fontSize = 20.sp, color = HomeFlixColors.TextSecondary, modifier = Modifier.padding(top = 18.dp))
        }
    }
}

@Composable
private fun PlayerUnavailable(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Movie details unavailable", fontSize = 28.sp)
            Button(onClick = onBack, modifier = Modifier.padding(top = 20.dp)) { Text("Back") }
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    if (milliseconds <= 0 || milliseconds == C.TIME_UNSET) return "0:00"
    val totalSeconds = milliseconds / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun speedLabel(speed: Float): String = if (speed == 1f) "Normal" else "${speed}x"

private enum class ControlFocus { PLAY, SETTINGS, FIRST_SETTING }
