package com.fisalia.files.data.model

import android.net.Uri

enum class ProtocolType {
    LOCAL,
    SMB,
    FTP,
    FTPS,
    SFTP,
    WEBDAV
}

data class FileItem(
    val id: String,
    val name: String,
    val path: String,
    val uri: Uri? = null,
    val size: Long = 0L,
    val isDirectory: Boolean = false,
    val lastModified: Long = 0L,
    val mimeType: String? = null,
    val extension: String = "",
    val protocol: ProtocolType = ProtocolType.LOCAL,
    val connectionId: String? = null,
    val childCount: Int = 0,
    val isHidden: Boolean = false
) {
    val isImage: Boolean get() = mimeType?.startsWith("image/") == true || extension.lowercase() in listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
    val isVideo: Boolean get() = mimeType?.startsWith("video/") == true || extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "webm", "flv", "3gp")
    val isAudio: Boolean get() = mimeType?.startsWith("audio/") == true || extension.lowercase() in listOf("mp3", "m4a", "wav", "flac", "aac", "ogg")
    val isApk: Boolean get() = extension.equals("apk", ignoreCase = true) || mimeType == "application/vnd.android.package-archive"
    val isZip: Boolean get() = extension.lowercase() in listOf("zip", "rar", "7z", "tar", "gz")
    val isDocument: Boolean get() = extension.lowercase() in listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub")
}
