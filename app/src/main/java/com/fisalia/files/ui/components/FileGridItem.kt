package com.fisalia.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fisalia.files.data.local.ThumbnailHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.ui.theme.GoogleBlueContainer
import com.fisalia.files.ui.theme.GoogleBluePrimary
import com.fisalia.files.ui.theme.GoogleCyan
import com.fisalia.files.ui.theme.GoogleCyanContainer
import com.fisalia.files.ui.theme.GoogleGreen
import com.fisalia.files.ui.theme.GoogleGreenContainer
import com.fisalia.files.ui.theme.GoogleOrange
import com.fisalia.files.ui.theme.GoogleOrangeContainer
import com.fisalia.files.ui.theme.GooglePurple
import com.fisalia.files.ui.theme.GooglePurpleContainer
import com.fisalia.files.ui.theme.GoogleRed
import com.fisalia.files.ui.theme.GoogleRedContainer
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    fileItem: FileItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val containerColor = remember(isSelected, surfaceColor, primaryColor) {
        if (isSelected) primaryColor.copy(alpha = 0.14f) else surfaceColor
    }

    val borderModifier = remember(isSelected, outlineVariant) {
        if (isSelected) {
            Modifier.border(1.5.dp, GoogleBluePrimary.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
        } else {
            Modifier.border(1.dp, outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        val thumbnailModel by androidx.compose.runtime.produceState<Any?>(
            initialValue = null,
            key1 = fileItem.path,
            key2 = "${fileItem.protocol}:${fileItem.connectionId}:${fileItem.size}:${fileItem.lastModified}"
        ) {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (fileItem.protocol == ProtocolType.LOCAL) {
                    when {
                        fileItem.isImage -> File(fileItem.path)
                        fileItem.isVideo -> ThumbnailHelper.getVideoThumbnail(context, fileItem.path) ?: File(fileItem.path)
                        fileItem.isAudio -> ThumbnailHelper.getAudioCover(context, fileItem.path)
                        fileItem.extension.lowercase() == "pdf" -> ThumbnailHelper.getPdfThumbnail(fileItem.path)
                        fileItem.isApk -> ThumbnailHelper.getApkIcon(context, fileItem.path)
                        else -> null
                    }
                } else {
                    when {
                        fileItem.isImage && !fileItem.connectionId.isNullOrBlank() -> {
                            com.fisalia.files.data.network.MediaStreamingServer.getInstance().getThumbnailUrl(
                                connectionId = fileItem.connectionId,
                                path = fileItem.path,
                                size = fileItem.size,
                                lastModified = fileItem.lastModified
                            )
                        }
                        else -> null
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.84f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailModel != null) {
                val smbThumbnailKey = if (fileItem.protocol == ProtocolType.SMB) {
                    "smb-thumbnail:${fileItem.connectionId}:${fileItem.path}:${fileItem.size}:${fileItem.lastModified}"
                } else {
                    null
                }
                val imageRequest = remember(context, thumbnailModel, smbThumbnailKey) {
                    val builder = ImageRequest.Builder(context)
                        .data(thumbnailModel)
                        .size(320, 320)
                        .crossfade(false)
                    if (smbThumbnailKey != null) {
                        builder.memoryCacheKey("$smbThumbnailKey:320")
                            .diskCacheKey(smbThumbnailKey)
                    }
                    builder.build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = fileItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
                val (icon, tint, bg) = remember(fileItem, surfaceVariant, onSurfaceVariant) {
                    when {
                        fileItem.isDirectory -> Triple(Icons.Default.Folder, GoogleBluePrimary, GoogleBlueContainer)
                        fileItem.isImage -> Triple(Icons.Default.Image, GoogleCyan, GoogleCyanContainer)
                        fileItem.isVideo -> Triple(Icons.Default.Videocam, GoogleRed, GoogleRedContainer)
                        fileItem.isAudio -> Triple(Icons.Default.AudioFile, GoogleOrange, GoogleOrangeContainer)
                        fileItem.isDocument -> Triple(Icons.Default.Description, GooglePurple, GooglePurpleContainer)
                        fileItem.isApk -> Triple(Icons.Default.Android, GoogleGreen, GoogleGreenContainer)
                        fileItem.isArchive -> Triple(Icons.Default.FolderZip, GoogleOrange, GoogleOrangeContainer)
                        else -> Triple(Icons.AutoMirrored.Filled.InsertDriveFile, onSurfaceVariant, surfaceVariant)
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(bg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            startY = 0f
                        )
                    )
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 8.dp, bottom = 10.dp)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = remember(fileItem) {
                    if (fileItem.isDirectory) {
                        if (fileItem.childCount >= 0) "${fileItem.childCount} item" else "Folder"
                    } else {
                        fileItem.path.trimEnd('/', '\\')
                            .substringBeforeLast('/', "")
                            .substringAfterLast('/')
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (fileItem.protocol != ProtocolType.LOCAL && fileItem.protocol != ProtocolType.SMB) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = fileItem.protocol.name,
                        color = Color.White,
                        fontSize = 9.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

        }
    }
}
