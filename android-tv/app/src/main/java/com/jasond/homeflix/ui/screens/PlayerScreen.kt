package com.jasond.homeflix.ui.screens

import android.app.Activity
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.jasond.homeflix.data.model.Movie
import com.jasond.homeflix.ui.player.buildMediaItem
import androidx.compose.ui.graphics.Color

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(movie: Movie?, onBack: () -> Unit) {
    if (movie == null) {
        PlayerUnavailable(onBack)
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var error by remember { mutableStateOf<PlaybackException?>(null) }
    val focusRequester = remember { FocusRequester() }
    val player = remember(movie.id) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) { playbackState = state }
                    override fun onPlayerError(playerError: PlaybackException) { error = playerError }
                })
                setMediaItem(buildMediaItem(movie))
                playWhenReady = true
                prepare()
            }
    }
    DisposableEffect(player, lifecycleOwner) {
        var resumePlayback = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> { resumePlayback = player.playWhenReady; player.pause() }
                Lifecycle.Event.ON_START -> if (resumePlayback) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        if (playerView?.isControllerFullyVisible == true) playerView?.hideController() else onBack()
    }
    LaunchedEffect(movie.id) { focusRequester.requestFocus() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = true
                    controllerShowTimeoutMs = 4_000
                    controllerAutoShow = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    setShowSubtitleButton(true)
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    this.player = player
                    requestFocus()
                    playerView = this
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || playerView?.isControllerFullyVisible != true) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> { player.seekBack(); true }
                        Key.DirectionRight -> { player.seekForward(); true }
                        else -> false
                    }
                },
        )
        if (playbackState == Player.STATE_BUFFERING && error == null) {
            Text("Loading movie...", fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
        }
        error?.let {
            Box(Modifier.fillMaxSize().background(Color(0xDD09090C)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Unable to play this movie", fontSize = 28.sp)
                    Text("Check that the HomeFlix server is running and the movie file still exists.", color = Color.LightGray, modifier = Modifier.padding(top = 12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 24.dp)) {
                        Button(onClick = { error = null; player.prepare(); player.play() }) { Text("Retry") }
                        Button(onClick = onBack) { Text("Back") }
                    }
                }
            }
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
