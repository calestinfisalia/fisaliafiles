package com.fisalia.files.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
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
fun FileListItem(
    fileItem: FileItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onActionShare: () -> Unit,
    onActionOpenWith: () -> Unit,
    onActionRename: () -> Unit,
    onActionDelete: () -> Unit,
    onActionDetails: () -> Unit,
    onActionZip: () -> Unit,
    onActionExtract: () -> Unit = {},
    onActionCopy: () -> Unit,
    onActionMove: () -> Unit,
    isFavorite: Boolean = false,
    onActionFavorite: () -> Unit = {},
    onActionMoveToSafe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            FileLeadingThumbnail(fileItem = fileItem)

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val subText = if (fileItem.isDirectory) {
                        val folderDescription = if (fileItem.childCount >= 0) {
                            stringResource(R.string.folder_item_count, fileItem.childCount)
                        } else {
                            stringResource(R.string.folder_label)
                        }
                        "$folderDescription • ${FileOperationsHelper.formatDate(fileItem.lastModified)}"
                    } else {
                        "${FileOperationsHelper.formatFileSize(fileItem.size)} • ${FileOperationsHelper.formatDate(fileItem.lastModified)}"
                    }

                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (fileItem.protocol != ProtocolType.LOCAL && fileItem.protocol != ProtocolType.SMB) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoogleBluePrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = fileItem.protocol.name,
                                color = GoogleBluePrimary,
                                fontSize = 10.sp,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi lainnya",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!fileItem.isDirectory && fileItem.protocol == ProtocolType.LOCAL) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_open_with)) },
                                onClick = {
                                    showMenu = false
                                    onActionOpenWith()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_share)) },
                                onClick = {
                                    showMenu = false
                                    onActionShare()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                showMenu = false
                                onActionRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_copy_to)) },
                            onClick = {
                                showMenu = false
                                onActionCopy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_move_to)) },
                            onClick = {
                                showMenu = false
                                onActionMove()
                            }
                        )
                        if (fileItem.protocol == ProtocolType.LOCAL) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_zip)) },
                                onClick = {
                                    showMenu = false
                                    onActionZip()
                                }
                            )
                            if (fileItem.isArchive) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_extract_archive)) },
                                    onClick = {
                                        showMenu = false
                                        onActionExtract()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(if (isFavorite) R.string.action_unfavorite else R.string.action_favorite)) },
                                onClick = {
                                    showMenu = false
                                    onActionFavorite()
                                }
                            )
                            if (!fileItem.isDirectory) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_move_to_safe)) },
                                    onClick = {
                                        showMenu = false
                                        onActionMoveToSafe()
                                    }
                                )
                            }
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_info)) },
                            onClick = {
                                showMenu = false
                                onActionDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onActionDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FileLeadingThumbnail(fileItem: FileItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val thumbnailModel by androidx.compose.runtime.produceState<Any?>(
        initialValue = null,
        key1 = fileItem.path,
        key2 = fileItem.protocol
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

    if (thumbnailModel != null) {
        val smbThumbnailKey = if (fileItem.protocol == ProtocolType.SMB) {
            "smb-thumbnail:${fileItem.connectionId}:${fileItem.path}:${fileItem.size}:${fileItem.lastModified}"
        } else {
            null
        }
        val imageRequest = remember(context, thumbnailModel, smbThumbnailKey) {
            val builder = ImageRequest.Builder(context)
                .data(thumbnailModel)
                .size(128, 128)
                .crossfade(false)
            if (smbThumbnailKey != null) {
                builder.memoryCacheKey(smbThumbnailKey)
                    .diskCacheKey(smbThumbnailKey)
            }
            builder.build()
        }
        AsyncImage(
            model = imageRequest,
            contentDescription = fileItem.name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
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
                else -> Triple(Icons.Default.InsertDriveFile, onSurfaceVariant, surfaceVariant)
            }
        }

        Box(
            modifier = modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
