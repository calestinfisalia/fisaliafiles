package com.fisalia.files.ui.screens.browse

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import com.fisalia.files.R
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.SortField
import com.fisalia.files.data.model.SortOption
import com.fisalia.files.ui.components.BreadcrumbBar
import com.fisalia.files.ui.components.CreateFolderDialog
import com.fisalia.files.ui.components.DeleteConfirmDialog
import com.fisalia.files.ui.components.FileDetailsBottomSheet
import com.fisalia.files.ui.components.FileGridItem
import com.fisalia.files.ui.components.FileListItem
import com.fisalia.files.ui.components.RenameDialog
import com.fisalia.files.ui.player.ActiveMedia
import com.fisalia.files.ui.player.ImageViewerScreen
import com.fisalia.files.ui.player.VideoPlayerScreen
import com.fisalia.files.ui.theme.GoogleBluePrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    state: BrowseUiState,
    viewModel: BrowseViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isGridView by viewModel.settingsPrefs.isGridView.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemForDetails by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var singleItemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSelectionMenu by remember { mutableStateOf(false) }

    fun prepareTransfer(items: List<FileItem>, isMove: Boolean) {
        if (items.isEmpty()) return
        viewModel.setClipboard(items, isCut = isMove)
        viewModel.clearSelection()
        android.widget.Toast.makeText(
            context,
            context.getString(
                if (isMove) R.string.toast_files_moved_to_clipboard else R.string.toast_files_copied_to_clipboard,
                items.size
            ),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    fun selectedFiles(): List<FileItem> =
        state.files.filter { it.id in state.selectedItemIds }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            if (state.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.items_selected, state.selectedItemIds.size), fontWeight = FontWeight.SemiBold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear_selection))
                        }
                    },
                    actions = {
                        IconButton(onClick = { prepareTransfer(selectedFiles(), true) }) {
                            Icon(Icons.Default.SyncAlt, contentDescription = stringResource(R.string.action_move_to))
                        }
                        Box {
                            IconButton(onClick = { showSelectionMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.action_more))
                            }
                            DropdownMenu(
                                expanded = showSelectionMenu,
                                onDismissRequest = { showSelectionMenu = false }
                            ) {
                                val selectedItem = selectedFiles().singleOrNull()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_select_all)) },
                                    onClick = {
                                        showSelectionMenu = false
                                        viewModel.selectAll()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_move_to)) },
                                    onClick = {
                                        showSelectionMenu = false
                                        prepareTransfer(selectedFiles(), true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_copy_to)) },
                                    onClick = {
                                        showSelectionMenu = false
                                        prepareTransfer(selectedFiles(), false)
                                    }
                                )
                                if (selectedItem != null) {
                                    if (selectedItem.protocol == ProtocolType.LOCAL && !selectedItem.isDirectory) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.action_open_with)) },
                                            onClick = {
                                                showSelectionMenu = false
                                                viewModel.clearSelection()
                                                FileOperationsHelper.openFile(context, File(selectedItem.path))
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_rename)) },
                                        onClick = {
                                            showSelectionMenu = false
                                            itemToRename = selectedItem
                                            viewModel.clearSelection()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_info)) },
                                        onClick = {
                                            showSelectionMenu = false
                                            itemForDetails = selectedItem
                                            viewModel.clearSelection()
                                        }
                                    )
                                    if (selectedItem.protocol == ProtocolType.LOCAL) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(if (viewModel.isFavorite(selectedItem)) R.string.action_unfavorite else R.string.action_favorite)) },
                                            onClick = {
                                                showSelectionMenu = false
                                                viewModel.toggleFavorite(selectedItem)
                                                viewModel.clearSelection()
                                            }
                                        )
                                        if (!selectedItem.isDirectory) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.action_move_to_safe)) },
                                                onClick = {
                                                    showSelectionMenu = false
                                                    viewModel.clearSelection()
                                                    viewModel.moveToSafeFolder(
                                                        selectedItem,
                                                        onSuccess = {
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                context.getString(R.string.toast_moved_to_safe),
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        onError = { error ->
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                error,
                                                                android.widget.Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                if (selectedItem?.protocol == ProtocolType.LOCAL && selectedItem.isArchive) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.action_extract_archive)) },
                                        onClick = {
                                            showSelectionMenu = false
                                            viewModel.clearSelection()
                                            viewModel.extractArchive(selectedItem) { destination ->
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.archive_extracted_to, destination.name),
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        val titleText = if (state.activeCategory != null) {
                            stringResource(state.activeCategory.titleRes)
                        } else {
                            state.rootName
                        }
                        Text(
                            text = titleText,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.settingsPrefs.setIsGridView(!isGridView) }) {
                            Icon(
                                imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                contentDescription = stringResource(if (isGridView) R.string.view_mode_list else R.string.view_mode_grid)
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.action_sort))
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_name_asc)) },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOption(SortOption(SortField.NAME, true))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_name_desc)) },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOption(SortOption(SortField.NAME, false))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_date_desc)) },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOption(SortOption(SortField.DATE, false))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.sort_size_desc)) },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOption(SortOption(SortField.SIZE, false))
                                    }
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (state.isSelectionMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val selectedFiles = state.files.filter { state.selectedItemIds.contains(it.id) && it.protocol == ProtocolType.LOCAL }
                            FileOperationsHelper.shareFiles(context, selectedFiles.map { File(it.path) })
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
                            }
                        }

                        IconButton(onClick = {
                            showDeleteConfirmDialog = true
                        }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        if (state.activeConnection == null) {
                            IconButton(onClick = {
                                viewModel.zipSelectedItems(
                                    context.getString(
                                        R.string.archive_name_format,
                                        System.currentTimeMillis()
                                    )
                                )
                            }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.action_zip))
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.clipboard != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .padding(start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = stringResource(R.string.action_paste),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.action_paste_count, state.clipboard.files.size),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.pasteClipboard() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = stringResource(R.string.action_paste),
                                        tint = GoogleBluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearClipboard() },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.btn_cancel),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (state.activeCategory == null) {
                        FloatingActionButton(
                            onClick = { showCreateFolderDialog = true },
                            containerColor = GoogleBluePrimary
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = stringResource(R.string.action_new_folder), tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumb Bar when navigating directory paths
            if (state.activeCategory == null && (state.currentPath.isNotBlank() || state.activeConnection != null)) {
                BreadcrumbBar(
                    rootName = state.rootName,
                    currentPath = state.currentPath,
                    basePath = state.basePath,
                    onPathSelected = { selectedPath ->
                        viewModel.navigateTo(selectedPath)
                    }
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoogleBluePrimary)
                }
            } else if (state.files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.empty_folder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val handleItemClick: (FileItem) -> Unit = { item ->
                    if (state.isSelectionMode) {
                        viewModel.toggleItemSelection(item.id)
                    } else if (item.isDirectory) {
                        viewModel.navigateTo(item.path)
                    } else if (item.protocol == ProtocolType.LOCAL) {
                        when {
                            item.isVideo || item.isAudio || item.isImage -> viewModel.openMediaItem(item, state.files)
                            else -> FileOperationsHelper.openFile(context, File(item.path))
                        }
                    } else {
                        // Remote file (SMB, FTP, FTPS, etc.)
                        when {
                            item.isVideo || item.isAudio || item.isImage -> {
                                viewModel.openMediaItem(item, state.files)
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
                }

                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 105.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.files,
                            key = { it.id },
                            contentType = { if (it.isDirectory) "directory" else "file" }
                        ) { item ->
                            val isSelected = state.selectedItemIds.contains(item.id)
                            FileGridItem(
                                fileItem = item,
                                isSelectionMode = state.isSelectionMode,
                                isSelected = isSelected,
                                onItemClick = { handleItemClick(item) },
                                onItemLongClick = {
                                    viewModel.toggleItemSelection(item.id)
                                },
                                onCheckedChange = {
                                    viewModel.toggleItemSelection(item.id)
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = state.files,
                            key = { it.id },
                            contentType = { if (it.isDirectory) "directory" else "file" }
                        ) { item ->
                            val isSelected = state.selectedItemIds.contains(item.id)
                            FileListItem(
                                fileItem = item,
                                isSelectionMode = state.isSelectionMode,
                                isSelected = isSelected,
                                onItemClick = { handleItemClick(item) },
                                onItemLongClick = {
                                    viewModel.toggleItemSelection(item.id)
                                },
                                onCheckedChange = {
                                    viewModel.toggleItemSelection(item.id)
                                },
                                onActionShare = {
                                    FileOperationsHelper.shareFiles(context, listOf(File(item.path)))
                                },
                                onActionOpenWith = {
                                    FileOperationsHelper.openFile(context, File(item.path))
                                },
                                onActionRename = {
                                    itemToRename = item
                                },
                                onActionDelete = {
                                    singleItemToDelete = item
                                },
                                onActionDetails = {
                                    itemForDetails = item
                                },
                                onActionZip = {
                                    viewModel.toggleItemSelection(item.id)
                                    viewModel.zipSelectedItems(item.name.substringBeforeLast('.'))
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
                                onActionCopy = { prepareTransfer(listOf(item), false) },
                                onActionMove = { prepareTransfer(listOf(item), true) },
                                isFavorite = viewModel.isFavorite(item),
                                onActionFavorite = {
                                    viewModel.toggleFavorite(item)
                                },
                                onActionMoveToSafe = {
                                    viewModel.moveToSafeFolder(
                                        item,
                                        onSuccess = {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.toast_moved_to_safe),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = { err ->
                                            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { folderName ->
                showCreateFolderDialog = false
                viewModel.createFolder(folderName)
            }
        )
    }

    itemToRename?.let { item ->
        RenameDialog(
            currentName = item.name,
            onDismiss = { itemToRename = null },
            onConfirm = { newName ->
                itemToRename = null
                viewModel.renameFile(item, newName)
            }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            itemCount = state.selectedItemIds.size,
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteSelectedItems()
            }
        )
    }

    singleItemToDelete?.let { item ->
        DeleteConfirmDialog(
            itemCount = 1,
            singleFileName = item.name,
            onDismiss = { singleItemToDelete = null },
            onConfirm = {
                singleItemToDelete = null
                viewModel.deleteSingleItem(item)
            }
        )
    }

    itemForDetails?.let { item ->
        FileDetailsBottomSheet(
            fileItem = item,
            onDismiss = { itemForDetails = null }
        )
    }

    // Error Toast
    androidx.compose.runtime.LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
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
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = GoogleBluePrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_remote_file),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    state.remoteMediaProgressText?.let { progressText ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = progressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoogleBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
