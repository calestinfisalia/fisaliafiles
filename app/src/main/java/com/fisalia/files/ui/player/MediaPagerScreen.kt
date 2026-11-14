package com.fisalia.files.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPagerScreen(
    activeMedia: ActiveMedia,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isFullscreen by rememberSaveable { mutableStateOf(false) }

    val playlist = activeMedia.playlist.ifEmpty { listOf(activeMedia.fileItem) }
    val initialPage = activeMedia.currentIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { playlist.size }
    )

    LaunchedEffect(isFullscreen, pagerState.currentPage) {
        activity?.let { act ->
            val insetsController = WindowCompat.getInsetsController(act.window, act.window.decorView)
            if (isFullscreen) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(act.window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> playlist.getOrNull(index)?.id ?: index }
        ) { page ->
            val item = playlist.getOrNull(page) ?: activeMedia.fileItem
            val isCurrentPage = page == pagerState.currentPage

            val localFile = if (item.id == activeMedia.fileItem.id) activeMedia.localPlaybackFile else File(item.path)
            val uri = if (item.id == activeMedia.fileItem.id && activeMedia.playbackUri != null) {
                activeMedia.playbackUri
            } else if (item.protocol != com.fisalia.files.data.model.ProtocolType.LOCAL && !item.connectionId.isNullOrBlank()) {
                val streamUrl = com.fisalia.files.data.network.MediaStreamingServer.getInstance().getStreamUrl(
                    connectionId = item.connectionId,
                    path = item.path
                )
                android.net.Uri.parse(streamUrl)
            } else null

            if (isCurrentPage) {
                when {
                    item.isVideo -> {
                        VideoPlayerScreen(
                            fileItem = item,
                            localPlaybackFile = localFile,
                            playbackUri = uri,
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            onClose = onClose
                        )
                    }
                    item.isAudio -> {
                        AudioPlayerSheet(
                            fileItem = item,
                            localPlaybackFile = localFile,
                            playbackUri = uri,
                            onDismiss = onClose
                        )
                    }
                    else -> {
                        ImageViewerScreen(
                            fileItem = item,
                            localPlaybackFile = localFile,
                            playbackUri = uri,
                            onClose = onClose
                        )
                    }
                }
            }
        }
    }
}
