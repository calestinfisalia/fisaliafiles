package com.fisalia.files.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fisalia.files.R
import com.fisalia.files.data.local.ThumbnailHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.ui.theme.GoogleBluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RecentsCarousel(
    recentFiles: List<FileItem>,
    onItemClick: (FileItem) -> Unit,
    onSeeAllClick: () -> Unit = {},
    onActionShare: (FileItem) -> Unit = {},
    onActionDelete: (FileItem) -> Unit = {},
    onActionDetails: (FileItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (recentFiles.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.recents_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = onSeeAllClick) {
                Text(
                    text = stringResource(R.string.recents_see_all),
                    color = GoogleBluePrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = recentFiles,
                key = { it.id }
            ) { item ->
                RecentFileCard(
                    fileItem = item,
                    onClick = { onItemClick(item) },
                    onActionShare = { onActionShare(item) },
                    onActionDelete = { onActionDelete(item) },
                    onActionDetails = { onActionDetails(item) }
                )
            }
        }
    }
}

@Composable
private fun RecentFileCard(
    fileItem: FileItem,
    onClick: () -> Unit,
    onActionShare: () -> Unit = {},
    onActionDelete: () -> Unit = {},
    onActionDetails: () -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = fileItem.path) {
        value = withContext(Dispatchers.IO) {
            when {
                fileItem.isImage -> {
                    try {
                        val opts = android.graphics.BitmapFactory.Options().apply { inSampleSize = 2 }
                        android.graphics.BitmapFactory.decodeFile(fileItem.path, opts)
                    } catch (_: Throwable) { null }
                }
                fileItem.isVideo -> ThumbnailHelper.getVideoThumbnail(context, fileItem.path)
                fileItem.isAudio -> ThumbnailHelper.getAudioCover(context, fileItem.path)
                else -> null
            }
        }
    }

    val folderName = remember(fileItem.path) {
        try {
            File(fileItem.path).parentFile?.name ?: if (fileItem.isVideo) "Video" else "Image"
        } catch (_: Throwable) {
            if (fileItem.isVideo) "Video" else "Image"
        }
    }

    Card(
        modifier = Modifier
            .size(width = 150.dp, height = 180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = fileItem.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (fileItem.isVideo) Icons.Default.PlayArrow else Icons.Default.Image,
                        contentDescription = null,
                        tint = GoogleBluePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            // Top-right 3-dots Menu Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { showMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open)) },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            showMenu = false
                            onActionShare()
                        }
                    )
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

            // Dark gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Text Info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = fileItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = folderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
