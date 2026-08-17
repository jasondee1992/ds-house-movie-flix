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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.PlayerProgressState
import com.jasond.homeflix.ui.PlayerViewModel
import com.jasond.homeflix.ui.player.buildMediaItem
import com.jasond.homeflix.ui.player.seekStepMs
import com.jasond.homeflix.ui.player.seekTarget
import com.jasond.homeflix.ui.player.subtitleLanguageCode
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
    var seekIndicator by remember { mutableStateOf<SeekFeedback?>(null) }
    var seekIndicatorVersion by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf(false) }
    var focusedControl by remember { mutableStateOf(ControlFocus.PLAY) }
    var menuReturnFocus by remember { mutableStateOf(ControlFocus.SETTINGS) }
    val surfaceFocus = remember { FocusRequester() }
    val backFocus = remember { FocusRequester() }
    val rewindFocus = remember { FocusRequester() }
    val playFocus = remember { FocusRequester() }
    val forwardFocus = remember { FocusRequester() }
    val scrubFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }
    val subtitlesFocus = remember { FocusRequester() }
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
        val wasHidden = !controlsVisible
        controlsVisible = true
        interactionVersion++
        if (requestPlayFocus || wasHidden) focusedControl = ControlFocus.PLAY
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
        seekIndicator = SeekFeedback(direction = direction, seconds = kotlin.math.abs(amount / 1_000).toInt())
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
    LaunchedEffect(interactionVersion, controlsVisible, settingsVisible, wantsToPlay, focusedControl) {
        if (controlsVisible && !settingsVisible && wantsToPlay && focusedControl != ControlFocus.SCRUB) {
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
    // Only restore focus when an overlay opens/closes. Focus changes inside the row must not
    // restart this effect, otherwise DPAD navigation is immediately pulled back to Play/Pause.
    LaunchedEffect(controlsVisible, settingsVisible) {
        if (!controlsVisible && !settingsVisible) return@LaunchedEffect
        withFrameNanos { }
        when {
            settingsVisible -> firstSettingFocus.requestFocus()
            focusedControl == ControlFocus.SETTINGS -> settingsFocus.requestFocus()
            focusedControl == ControlFocus.SUBTITLES -> subtitlesFocus.requestFocus()
            focusedControl == ControlFocus.FORWARD -> forwardFocus.requestFocus()
            focusedControl == ControlFocus.REWIND -> rewindFocus.requestFocus()
            focusedControl == ControlFocus.SCRUB -> scrubFocus.requestFocus()
            focusedControl == ControlFocus.BACK -> backFocus.requestFocus()
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
                focusedControl = menuReturnFocus
                touchControls()
            }
            else -> exit()
        }
    }
    fun handleBack() {
        if (settingsVisible) {
            settingsVisible = false
            focusedControl = menuReturnFocus
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
                if (controlsVisible) interactionVersion++
                val repeat = event.nativeKeyEvent.repeatCount
                when (event.key) {
                    Key.DirectionLeft -> if (!settingsVisible && !controlsVisible) {
                        seek(-1, repeat); true
                    } else false
                    Key.DirectionRight -> if (!settingsVisible && !controlsVisible) {
                        seek(1, repeat); true
                    } else false
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
                backFocus = backFocus,
                rewindFocus = rewindFocus,
                forwardFocus = forwardFocus,
                scrubFocus = scrubFocus,
                settingsFocus = settingsFocus,
                subtitlesFocus = subtitlesFocus,
                onInteraction = { touchControls() },
                onControlFocused = { focusedControl = it; interactionVersion++ },
                onBack = ::exit,
                onSeekBack = { seek(-1, 0) },
                onPlayPause = { if (player.playWhenReady) player.pause() else player.play(); touchControls() },
                onSeekForward = { seek(1, 0) },
                onSubtitles = {
                    menuReturnFocus = ControlFocus.SUBTITLES
                    settingsVisible = true
                    focusedControl = ControlFocus.SUBTITLES
                    interactionVersion++
                },
                onSettings = {
                    menuReturnFocus = ControlFocus.SETTINGS
                    settingsVisible = true
                    focusedControl = ControlFocus.SETTINGS
                    interactionVersion++
                },
            )
        }

        seekIndicator?.let { SeekIndicator(it) }

        if (settingsVisible && error == null) {
            PlaybackSettings(
                selectedSpeed = player.playbackParameters.speed,
                subtitles = movie.subtitles.map { it.language }.distinct(),
                firstFocus = firstSettingFocus,
                onSelect = { speed ->
                    player.setPlaybackSpeed(speed)
                    settingsVisible = false
                    focusedControl = menuReturnFocus
                    touchControls()
                },
                onSubtitle = { language ->
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, language == null)
                        .setPreferredTextLanguage(language?.let(::subtitleLanguageCode))
                        .build()
                    settingsVisible = false
                    focusedControl = ControlFocus.SUBTITLES
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
    backFocus: FocusRequester,
    rewindFocus: FocusRequester,
    playFocus: FocusRequester,
    forwardFocus: FocusRequester,
    scrubFocus: FocusRequester,
    settingsFocus: FocusRequester,
    subtitlesFocus: FocusRequester,
    onInteraction: () -> Unit,
    onControlFocused: (ControlFocus) -> Unit,
    onBack: () -> Unit,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    onSubtitles: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0x66000000), Color.Transparent, Color(0xE6000000))),
        ),
    ) {
        Row(
            Modifier.align(Alignment.TopStart).padding(start = 42.dp, top = 34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(
                PlayerControlIcon.BACK, "Exit playback", onBack,
                Modifier.focusRequester(backFocus).focusProperties { down = scrubFocus },
                onInteraction, { onControlFocused(ControlFocus.BACK) }, compact = true,
            )
            Text(movie.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp))
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 54.dp, vertical = 36.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), color = Color.White, fontSize = 15.sp)
                Text(formatTime(durationMs), color = Color.White, fontSize = 15.sp)
            }
            PlaybackProgress(
                positionMs = positionMs,
                durationMs = durationMs,
                modifier = Modifier.focusRequester(scrubFocus).focusProperties {
                    up = backFocus
                    down = playFocus
                },
                onFocused = { onControlFocused(ControlFocus.SCRUB); onInteraction() },
                onSeekBack = onSeekBack,
                onSeekForward = onSeekForward,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp).focusGroup(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerIconButton(PlayerControlIcon.REWIND, "Rewind 10 seconds", onSeekBack,
                    Modifier.focusRequester(rewindFocus).focusProperties { right = playFocus; up = scrubFocus }, onInteraction,
                    { onControlFocused(ControlFocus.REWIND) })
                Spacer(Modifier.width(18.dp))
                PlayerIconButton(if (isPlaying) PlayerControlIcon.PAUSE else PlayerControlIcon.PLAY,
                    if (isPlaying) "Pause" else "Play", onPlayPause,
                    Modifier.focusRequester(playFocus).focusProperties {
                        left = rewindFocus; right = forwardFocus; up = scrubFocus
                    }, onInteraction, { onControlFocused(ControlFocus.PLAY) }, primary = true)
                Spacer(Modifier.width(18.dp))
                PlayerIconButton(PlayerControlIcon.FORWARD, "Forward 10 seconds", onSeekForward,
                    Modifier.focusRequester(forwardFocus).focusProperties {
                        left = playFocus; right = subtitlesFocus; up = scrubFocus
                    }, onInteraction,
                    { onControlFocused(ControlFocus.FORWARD) })
                Spacer(Modifier.width(34.dp))
                PlayerIconButton(PlayerControlIcon.SUBTITLES, "Audio and subtitles", onSubtitles,
                    Modifier.focusRequester(subtitlesFocus).focusProperties {
                        left = forwardFocus; right = settingsFocus; up = scrubFocus
                    }, onInteraction, { onControlFocused(ControlFocus.SUBTITLES) })
                Spacer(Modifier.width(14.dp))
                PlayerIconButton(PlayerControlIcon.SETTINGS, "Settings", onSettings,
                    Modifier.focusRequester(settingsFocus).focusProperties { left = subtitlesFocus; up = scrubFocus },
                    onInteraction, { onControlFocused(ControlFocus.SETTINGS) })
            }
        }
    }
}

@Composable
private fun PlayerIconButton(
    icon: PlayerControlIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit,
    onFocused: () -> Unit,
    primary: Boolean = false,
    compact: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val focusScale by animateFloatAsState(if (focused) 1.1f else 1f, tween(150), label = "player control focus")
    val size = when {
        primary -> 66.dp
        compact -> 48.dp
        else -> 54.dp
    }
    Button(
        onClick = { onInteraction(); onClick() },
        modifier = modifier.size(size).scale(focusScale)
            .onFocusChanged { state -> focused = state.isFocused; if (state.isFocused) onFocused() }
            .then(if (focused) Modifier.border(2.dp, HomeFlixColors.Brand, CircleShape) else Modifier)
            .semantics { contentDescription = label },
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.colors(
            containerColor = Color(0xB326262B),
            focusedContainerColor = Color(0xF2505057),
            contentColor = Color(0xFFD7D7DB),
            focusedContentColor = Color.White,
        ),
        shape = ButtonDefaults.shape(CircleShape),
    ) {
        PlayerControlGlyph(icon, Modifier.size(if (primary) 30.dp else if (compact) 23.dp else 25.dp))
    }
}

@Composable
private fun PlayerControlGlyph(icon: PlayerControlIcon, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val white = Color.White
        when (icon) {
            PlayerControlIcon.BACK -> {
                drawLine(white, androidx.compose.ui.geometry.Offset(size.width * .78f, size.height * .5f),
                    androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .5f), size.minDimension * .1f)
                drawLine(white, androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .5f),
                    androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .23f), size.minDimension * .1f)
                drawLine(white, androidx.compose.ui.geometry.Offset(size.width * .2f, size.height * .5f),
                    androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .77f), size.minDimension * .1f)
            }
            PlayerControlIcon.PLAY -> drawPath(Path().apply {
                moveTo(size.width * .28f, size.height * .15f)
                lineTo(size.width * .82f, size.height * .5f)
                lineTo(size.width * .28f, size.height * .85f)
                close()
            }, white)
            PlayerControlIcon.PAUSE -> {
                drawRoundRect(white, topLeft = androidx.compose.ui.geometry.Offset(size.width * .22f, size.height * .15f),
                    size = androidx.compose.ui.geometry.Size(size.width * .2f, size.height * .7f))
                drawRoundRect(white, topLeft = androidx.compose.ui.geometry.Offset(size.width * .58f, size.height * .15f),
                    size = androidx.compose.ui.geometry.Size(size.width * .2f, size.height * .7f))
            }
            PlayerControlIcon.REWIND, PlayerControlIcon.FORWARD -> {
                val forward = icon == PlayerControlIcon.FORWARD
                fun triangle(start: Float) = Path().apply {
                    if (forward) {
                        moveTo(size.width * start, size.height * .2f)
                        lineTo(size.width * (start + .34f), size.height * .5f)
                        lineTo(size.width * start, size.height * .8f)
                    } else {
                        moveTo(size.width * (1f - start), size.height * .2f)
                        lineTo(size.width * (1f - start - .34f), size.height * .5f)
                        lineTo(size.width * (1f - start), size.height * .8f)
                    }
                    close()
                }
                drawPath(triangle(.12f), white)
                drawPath(triangle(.46f), white)
            }
            PlayerControlIcon.SETTINGS -> {
                drawCircle(white, radius = size.minDimension * .34f, style = Stroke(size.minDimension * .12f))
                drawCircle(white, radius = size.minDimension * .09f)
            }
            PlayerControlIcon.SUBTITLES -> {
                drawRoundRect(white, topLeft = androidx.compose.ui.geometry.Offset(size.width * .1f, size.height * .2f),
                    size = androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .6f), style = Stroke(size.minDimension * .09f))
                drawLine(white, androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .58f),
                    androidx.compose.ui.geometry.Offset(size.width * .48f, size.height * .58f), size.minDimension * .07f)
                drawLine(white, androidx.compose.ui.geometry.Offset(size.width * .55f, size.height * .58f),
                    androidx.compose.ui.geometry.Offset(size.width * .76f, size.height * .58f), size.minDimension * .07f)
            }
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val progress = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    BoxWithConstraints(
        modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp).height(if (focused) 52.dp else 22.dp)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> { onSeekBack(); true }
                    Key.DirectionRight -> { onSeekForward(); true }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocused() }
            .focusable()
            .semantics { contentDescription = "Playback timeline. Use left and right to seek." },
        contentAlignment = Alignment.BottomStart,
    ) {
        val thumbSize = if (focused) 18.dp else 12.dp
        if (focused) {
            Box(
                Modifier.offset(x = (maxWidth - 72.dp) * progress).width(72.dp).height(28.dp)
                    .background(Color(0xEB17171A), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(formatTime(positionMs), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        }
        Box(Modifier.fillMaxWidth().height(if (focused) 9.dp else 6.dp).align(Alignment.BottomStart)
            .background(Color(0xFF67676F), RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress)
                .background(HomeFlixColors.Brand, RoundedCornerShape(5.dp)))
        }
        Box(
            Modifier.offset(x = (maxWidth - thumbSize) * progress).align(Alignment.BottomStart).size(thumbSize)
                .background(Color.White, CircleShape)
                .then(if (focused) Modifier.border(3.dp, HomeFlixColors.Brand, CircleShape) else Modifier),
        )
    }
}

@Composable
private fun PlaybackSettings(
    selectedSpeed: Float,
    subtitles: List<String>,
    firstFocus: FocusRequester,
    onSelect: (Float) -> Unit,
    onSubtitle: (String?) -> Unit,
) {
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
            if (subtitles.isNotEmpty()) {
                Text("Subtitles", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 22.dp))
                Button(onClick = { onSubtitle(null) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Text("Off")
                }
                subtitles.forEach { language ->
                    Button(onClick = { onSubtitle(language) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(language)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeekIndicator(feedback: SeekFeedback) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.background(Color(0xCC16161B), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 24.dp, vertical = 15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Canvas(Modifier.size(30.dp)) {
                    val start = if (feedback.direction > 0) 215f else -35f
                    drawArc(Color.White, start, 285f, false, style = Stroke(size.minDimension * .09f))
                    val x = if (feedback.direction > 0) size.width * .82f else size.width * .18f
                    drawPath(Path().apply {
                        moveTo(x, size.height * .14f)
                        lineTo(x + if (feedback.direction > 0) size.width * .14f else -size.width * .14f, size.height * .28f)
                        lineTo(x, size.height * .34f); close()
                    }, Color.White)
                }
                Text("${feedback.seconds}s", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
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
            if (overlay) Color.Transparent else HomeFlixColors.Background,
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
            if (!overlay) Text(message, fontSize = 20.sp, color = HomeFlixColors.TextSecondary,
                modifier = Modifier.padding(top = 18.dp))
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

private enum class PlayerControlIcon { BACK, REWIND, PLAY, PAUSE, FORWARD, SUBTITLES, SETTINGS }

private data class SeekFeedback(val direction: Int, val seconds: Int)

private enum class ControlFocus { BACK, REWIND, PLAY, FORWARD, SCRUB, SUBTITLES, SETTINGS }
