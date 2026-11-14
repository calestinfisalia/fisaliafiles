package com.fisalia.files.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileCategory

data class CategoryItemUI(
    val category: FileCategory,
    val titleRes: Int,
    val icon: ImageVector
)

@Composable
fun CategoryGrid(
    onCategoryClick: (FileCategory) -> Unit,
    categorySizes: Map<FileCategory, Long> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val items = listOf(
        CategoryItemUI(FileCategory.DOWNLOADS, R.string.category_downloads, Icons.Default.Download),
        CategoryItemUI(FileCategory.IMAGES, R.string.category_images, Icons.Default.Image),
        CategoryItemUI(FileCategory.VIDEOS, R.string.category_videos, Icons.Default.Movie),
        CategoryItemUI(FileCategory.AUDIO, R.string.category_audio, Icons.Default.MusicNote),
        CategoryItemUI(FileCategory.DOCUMENTS, R.string.category_documents, Icons.Default.Description),
        CategoryItemUI(FileCategory.APPS, R.string.category_apps, Icons.Default.Apps)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.categories_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        val size = categorySizes[item.category]
                        val sizeText = if (size != null && size > 0L) {
                            FileOperationsHelper.formatFileSize(size)
                        } else {
                            ""
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .clickable { onCategoryClick(item.category) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.titleRes),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(item.titleRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (sizeText.isNotEmpty()) {
                                        Text(
                                            text = sizeText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
