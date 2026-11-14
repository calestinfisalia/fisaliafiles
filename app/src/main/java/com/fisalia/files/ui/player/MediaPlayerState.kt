package com.fisalia.files.ui.player

import android.net.Uri
import com.fisalia.files.data.model.FileItem
import java.io.File

sealed class ActiveMedia {
    abstract val fileItem: FileItem
    abstract val playlist: List<FileItem>
    abstract val currentIndex: Int
    abstract val localPlaybackFile: File?
    abstract val playbackUri: Uri?

    data class Video(
        override val fileItem: FileItem,
        override val playlist: List<FileItem> = listOf(fileItem),
        override val currentIndex: Int = 0,
        override val localPlaybackFile: File? = null,
        override val playbackUri: Uri? = null
    ) : ActiveMedia()

    data class Audio(
        override val fileItem: FileItem,
        override val playlist: List<FileItem> = listOf(fileItem),
        override val currentIndex: Int = 0,
        override val localPlaybackFile: File? = null,
        override val playbackUri: Uri? = null
    ) : ActiveMedia()

    data class Image(
        override val fileItem: FileItem,
        override val playlist: List<FileItem> = listOf(fileItem),
        override val currentIndex: Int = 0,
        override val localPlaybackFile: File? = null,
        override val playbackUri: Uri? = null
    ) : ActiveMedia()
}

fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
