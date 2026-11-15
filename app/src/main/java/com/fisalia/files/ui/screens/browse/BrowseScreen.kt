package com.fisalia.files.ui.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.StorageItem
import com.fisalia.files.ui.components.CategoryGrid
import com.fisalia.files.ui.components.DeleteConfirmDialog
import com.fisalia.files.ui.components.FileDetailsBottomSheet
import com.fisalia.files.ui.components.FileListItem
import com.fisalia.files.ui.components.GoogleFilesTopBar
import com.fisalia.files.ui.components.RecentsCarousel
import com.fisalia.files.ui.components.RenameDialog
import com.fisalia.files.ui.components.SafeFolderPinDialog
import com.fisalia.files.ui.components.StorageGridCard
import com.fisalia.files.ui.player.ActiveMedia
import com.fisalia.files.ui.theme.GoogleBluePrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onOpenDrawer: () -> Unit = {},
    onOpenNetworkStorage: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val showHomeRecents by viewModel.settingsPrefs.showHomeRecents.collectAsState()
    val showHomeCategories by viewModel.settingsPrefs.showHomeCategories.collectAsState()
    val showHomeCollections by viewModel.settingsPrefs.showHomeCollections.collectAsState()
    val showHomeStorage by viewModel.settingsPrefs.showHomeStorage.collectAsState()
    val context = LocalContext.current
    var showSafeFolderPinDialog by remember { mutableStateOf(false) }
    var isSettingSafePin by remember { mutableStateOf(false) }
    var showCollectionsMenu by remember { mutableStateOf(false) }

    // Dialog & bottom sheet states for search results & recents
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var singleItemToDelete by remember { mutableStateOf<FileItem?>(null) }

    val isInsideDirectory = state.activeConnection == null &&
        (state.currentPath.isNotBlank() || state.activeCategory != null || state.isFavorites || state.isSafeFolder)

    androidx.compose.runtime.LaunchedEffect(isInsideDirectory) {
        if (!isInsideDirectory) {
            viewModel.loadStorageDevices()
            viewModel.loadRecentFiles()
        }
    }

    BackHandler(enabled = isInsideDirectory || state.isSearchActive) {
        viewModel.navigateBack()
    }

    if (isInsideDirectory && !state.isSearchActive) {
        ExplorerScreen(
            state = state,
            viewModel = viewModel,
            onBackClick = { viewModel.navigateBack() },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 4.dp)
            ) {
                // Google Files Top Search Bar with Hamburger Menu
                GoogleFilesTopBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearchTriggered = { viewModel.searchFiles(it) },
                    isSearchActive = state.isSearchActive,
                    onSearchActiveChange = { viewModel.setSearchActive(it) },
                    showBackButton = state.isSearchActive,
                    onBackClick = { viewModel.closeSearch() },
                    onMenuClick = onOpenDrawer,
                    onSettingsClick = onOpenSettings
                )

                if (state.isSearchActive) {
                    // Search Results
                    if (state.searchResults.isEmpty() && state.searchQuery.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.no_search_results, state.searchQuery),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                items = state.searchResults,
                                key = { it.id },
                                contentType = { if (it.isDirectory) "folder" else "file" }
                            ) { item ->
                                FileListItem(
                                    fileItem = item,
                                    isSelectionMode = false,
                                    isSelected = false,
                                    onItemClick = {
                                        if (item.isDirectory) {
                                            viewModel.closeSearch()
                                            viewModel.openStorageDevice(state.storageDevices.first())
                                            viewModel.navigateTo(item.path)
                                        } else if (item.protocol == ProtocolType.LOCAL) {
                                            when {
                                                item.isVideo || item.isAudio || item.isImage -> viewModel.openMediaItem(item, state.searchResults)
                                                else -> FileOperationsHelper.openFile(context, File(item.path))
                                            }
                                        } else {
                                            val streamUrl = com.fisalia.files.data.network.MediaStreamingServer.getInstance().getStreamUrl(
                                                connectionId = item.connectionId ?: "",
                                                path = item.path
                                            )
                                            when {
                                                item.isVideo -> {
                                                    viewModel.openMedia(ActiveMedia.Video(item, playbackUri = android.net.Uri.parse(streamUrl)))
                                                }
                                                item.isAudio -> {
                                                    viewModel.openMedia(ActiveMedia.Audio(item, playbackUri = android.net.Uri.parse(streamUrl)))
                                                }
                                                item.isImage -> {
                                                    viewModel.openMedia(ActiveMedia.Image(item, playbackUri = android.net.Uri.parse(streamUrl)))
                                                }
                                                else -> {
                                                    viewModel.prepareRemoteFileForPlayback(
                                                        fileItem = item,
                                                        onReady = { localFile ->
                                                            FileOperationsHelper.openFile(context, localFile)
                                                        },
                                                        onError = { err ->
                                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onItemLongClick = {},
                                    onCheckedChange = {},
                                    onActionShare = { FileOperationsHelper.shareFiles(context, listOf(File(item.path))) },
                                    onActionOpenWith = { FileOperationsHelper.openFile(context, File(item.path)) },
                                    onActionRename = { itemToRename = item },
                                    onActionDelete = {
                                        singleItemToDelete = item
                                        showDeleteConfirmDialog = true
                                    },
                                    onActionDetails = { itemForDetails = item },
                                    onActionZip = {
                                        viewModel.zipSelectedItems(
                                            context.getString(
                                                R.string.archive_name_format,
                                                File(item.name).nameWithoutExtension
                                            )
                                        )
                                    },
                                    onActionExtract = {
                                        viewModel.extractArchive(item) { destination ->
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.archive_extracted_to, destination.name),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onActionCopy = {
                                        viewModel.setClipboard(listOf(item), isCut = false)
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_copied_to_clipboard),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onActionMove = {
                                        viewModel.setClipboard(listOf(item), isCut = true)
                                        android.widget.Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_moved_to_clipboard),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Google Files Main Dashboard Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 90.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Recents Carousel (Baru-baru ini)
                        if (showHomeRecents && state.recentFiles.isNotEmpty()) {
                            RecentsCarousel(
                                recentFiles = state.recentFiles,
                                onItemClick = { item ->
                                    viewModel.openMediaItem(item, state.recentFiles)
                                },
                                onSeeAllClick = {
                                    viewModel.openAllRecents()
                                },
                                onActionShare = { item ->
                                    FileOperationsHelper.shareFiles(context, listOf(File(item.path)))
                                },
                                onActionDelete = { item ->
                                    singleItemToDelete = item
                                    showDeleteConfirmDialog = true
                                },
                                onActionDetails = { item ->
                                    itemForDetails = item
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Categories Grid (Google Files iconic tiles with calculated sizes)
                        if (showHomeCategories) {
                            CategoryGrid(
                                onCategoryClick = { category ->
                                    viewModel.openCategory(category)
                                },
                                categorySizes = state.categorySizes
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (showHomeCollections) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.collections_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Box {
                                    IconButton(onClick = { showCollectionsMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.more_options),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showCollectionsMenu,
                                        onDismissRequest = { showCollectionsMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_customize_home)) },
                                            onClick = {
                                                showCollectionsMenu = false
                                                onOpenSettings()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.safe_folder_set_pin)) },
                                            onClick = {
                                                showCollectionsMenu = false
                                                isSettingSafePin = true
                                                showSafeFolderPinDialog = true
                                            }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CollectionCard(
                                    title = stringResource(R.string.favorites),
                                    icon = Icons.Default.StarOutline,
                                    iconColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    onClick = { viewModel.openFavorites() }
                                )

                                CollectionCard(
                                    title = stringResource(R.string.safe_folder),
                                    icon = Icons.Default.Lock,
                                    iconColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val savedPin = viewModel.settingsPrefs.getSafeFolderPin()
                                        isSettingSafePin = savedPin == null
                                        showSafeFolderPinDialog = true
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (showHomeStorage) {
                            Text(
                                text = stringResource(R.string.storage_devices_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            val primaryStorage = state.storageDevices.firstOrNull()
                            val sdCardStorage = state.storageDevices.getOrNull(1)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val internalFree = if (primaryStorage != null && primaryStorage.freeBytes > 0L) {
                                        stringResource(
                                            R.string.storage_free,
                                            FileOperationsHelper.formatFileSize(primaryStorage.freeBytes)
                                        )
                                    } else {
                                        stringResource(R.string.storage_free_generic)
                                    }

                                    StorageGridCard(
                                        title = stringResource(R.string.internal_storage),
                                        subtitle = internalFree,
                                        icon = Icons.Default.PhoneAndroid,
                                        onClick = {
                                            if (primaryStorage != null) {
                                                viewModel.openStorageDevice(primaryStorage)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (sdCardStorage != null) {
                                        val sdFree = if (sdCardStorage.freeBytes > 0L) {
                                            stringResource(
                                                R.string.storage_free,
                                                FileOperationsHelper.formatFileSize(sdCardStorage.freeBytes)
                                            )
                                        } else {
                                            stringResource(R.string.storage_free_generic)
                                        }
                                        StorageGridCard(
                                            title = stringResource(R.string.sd_card),
                                            subtitle = sdFree,
                                            icon = Icons.Default.SdCard,
                                            onClick = { viewModel.openStorageDevice(sdCardStorage) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        StorageGridCard(
                                            title = stringResource(R.string.drawer_network),
                                            subtitle = stringResource(R.string.drawer_network_sub),
                                            icon = Icons.Default.Dns,
                                            onClick = onOpenNetworkStorage,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (sdCardStorage != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        StorageGridCard(
                                            title = stringResource(R.string.drawer_network),
                                            subtitle = stringResource(R.string.drawer_network_sub),
                                            icon = Icons.Default.Dns,
                                            onClick = onOpenNetworkStorage,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    // Remote media preparation loader overlay
    if (state.isPreparingRemoteMedia) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = GoogleBluePrimary)
                    Text(
                        text = stringResource(R.string.loading_remote_file),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Built-in Dialogs
    if (showSafeFolderPinDialog) {
        SafeFolderPinDialog(
            isSettingPin = isSettingSafePin,
            onDismiss = { showSafeFolderPinDialog = false },
            onConfirm = { pin ->
                val savedPin = viewModel.settingsPrefs.getSafeFolderPin()
                if (isSettingSafePin) {
                    viewModel.settingsPrefs.setSafeFolderPin(pin)
                    showSafeFolderPinDialog = false
                    viewModel.openSafeFolder()
                } else if (pin == savedPin) {
                    showSafeFolderPinDialog = false
                    viewModel.openSafeFolder()
                } else {
                    android.widget.Toast.makeText(context, context.getString(R.string.safe_folder_pin_incorrect), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    itemToRename?.let { item ->
        RenameDialog(
            currentName = item.name,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                viewModel.renameFile(item, newName)
                itemToRename = null
            }
        )
    }

    itemForDetails?.let { item ->
        FileDetailsBottomSheet(
            fileItem = item,
            onDismiss = { itemForDetails = null }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            itemCount = if (singleItemToDelete != null) 1 else 0,
            onDismiss = {
                showDeleteConfirmDialog = false
                singleItemToDelete = null
            },
            onConfirm = {
                singleItemToDelete?.let { viewModel.deleteSingleItem(it) }
                showDeleteConfirmDialog = false
                singleItemToDelete = null
            }
        )
    }
}

@Composable
private fun CollectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
