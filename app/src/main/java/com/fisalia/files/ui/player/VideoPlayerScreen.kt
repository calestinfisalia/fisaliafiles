@file:kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.fisalia.files.ui.player

import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.ui.theme.GoogleBluePrimary
import kotlinx.coroutines.delay
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    fileItem: FileItem,
    localPlaybackFile: File? = null,
    playbackUri: Uri? = null,
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveUri = playbackUri ?: Uri.fromFile(localPlaybackFile ?: File(fileItem.path))
    val targetFile = localPlaybackFile ?: File(fileItem.path)
    var subtitlesEnabled by remember(fileItem.id) { mutableStateOf(true) }
    var savedPosition by rememberSaveable(fileItem.id) { mutableLongStateOf(0L) }

    fun toggleFullscreen() {
        onToggleFullscreen()
    }

    val exoPlayer = remember(fileItem.id, effectiveUri) {
        val trackSelector = androidx.media3.exoplayer.trackselection.DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setSelectUndeterminedTextLanguage(true)
                    .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
            )
        }
        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true)
        }
        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                val externalSub = if (playbackUri == null && targetFile.exists()) {
                    val baseName = targetFile.nameWithoutExtension
                    val parentDir = targetFile.parentFile
                    parentDir?.listFiles()?.firstOrNull {
                        it.nameWithoutExtension.equals(baseName, ignoreCase = true) &&
                        it.extension.lowercase() in listOf("srt", "ass", "ssa", "vtt")
                    }
                } else null

                val mediaItem = if (externalSub != null) {
                    val mimeType = when (externalSub.extension.lowercase()) {
                        "ass", "ssa" -> androidx.media3.common.MimeTypes.TEXT_SSA
                        "vtt" -> androidx.media3.common.MimeTypes.TEXT_VTT
                        else -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                    }
                    val subConfig = MediaItem.SubtitleConfiguration.Builder(Uri.fromFile(externalSub))
                        .setMimeType(mimeType)
                        .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                        .build()
                    MediaItem.Builder()
                        .setUri(effectiveUri)
                        .setSubtitleConfigurations(listOf(subConfig))
                        .build()
                } else {
                    MediaItem.fromUri(effectiveUri)
                }

                setMediaItem(mediaItem)
                if (savedPosition > 0L) {
                    seekTo(savedPosition)
                }
                prepare()
                playWhenReady = true
            }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var currentPosition by remember { mutableLongStateOf(savedPosition) }
    var totalDuration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }

    // Listener for ExoPlayer playback state
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                val textGroups = tracks.groups.filter { it.type == androidx.media3.common.C.TRACK_TYPE_TEXT }
                if (textGroups.isNotEmpty() && !textGroups.any { it.isSelected }) {
                    val firstGroup = textGroups.first()
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            androidx.media3.common.TrackSelectionOverride(firstGroup.mediaTrackGroup, 0)
                        )
                        .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, false)
                        .build()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        totalDuration = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        showControls = true
                    }
                    else -> isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Position progress tracking
    LaunchedEffect(isPlaying, showControls) {
        if (showControls && isPlaying) {
            currentPosition = exoPlayer.currentPosition
            savedPosition = currentPosition
            totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            while (isPlaying && showControls) {
                delay(500)
                currentPosition = exoPlayer.currentPosition
                savedPosition = currentPosition
                totalDuration = exoPlayer.duration.coerceAtLeast(0L)
            }
        }
    }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3500)
            showControls = false
        }
    }

    BackHandler {
        onClose()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    subtitleView?.apply {
                        setViewType(SubtitleView.VIEW_TYPE_CANVAS)
                        setStyle(
                            CaptionStyleCompat(
                                AndroidColor.WHITE,
                                AndroidColor.TRANSPARENT,
                                AndroidColor.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                AndroidColor.BLACK,
                                Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            )
                        )
                        setApplyEmbeddedStyles(true)
                        setApplyEmbeddedFontSizes(true)
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.15f)
                        setBottomPaddingFraction(0.06f)
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Indicator
        if (isBuffering) {
            CircularProgressIndicator(
                color = GoogleBluePrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (isFullscreen) {
                            toggleFullscreen()
                        } else {
                            onClose()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = fileItem.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = {
                        subtitlesEnabled = !subtitlesEnabled
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !subtitlesEnabled)
                            .build()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = stringResource(R.string.player_subtitles),
                            tint = if (subtitlesEnabled) GoogleBluePrimary else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(onClick = {
                        FileOperationsHelper.openFile(context, File(fileItem.path))
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.action_open_with),
                            tint = Color.White
                        )
                    }
                }

                // Center Play / Pause & Skip buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L))
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = stringResource(R.string.player_rewind_10),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(GoogleBluePrimary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play),
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration))
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = stringResource(R.string.player_forward_10),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Timeline & Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Slider(
                        value = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { frac ->
                            val targetMs = (frac * totalDuration).toLong()
                            exoPlayer.seekTo(targetMs)
                            currentPosition = targetMs
                            savedPosition = targetMs
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = GoogleBluePrimary,
                            activeTrackColor = GoogleBluePrimary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                        .size(14.dp)
                                    .clip(CircleShape)
                                    .background(GoogleBluePrimary)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(4.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = GoogleBluePrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatDuration(currentPosition)} / ${formatDuration(totalDuration)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = { toggleFullscreen() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = stringResource(
                                    if (isFullscreen) R.string.player_exit_fullscreen
                                    else R.string.player_fullscreen
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
