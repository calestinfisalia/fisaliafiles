package com.fisalia.files.ui.screens.clean

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fisalia.files.R
import com.fisalia.files.data.local.DuplicateGroup
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.ui.theme.GoogleBlueContainer
import com.fisalia.files.ui.theme.GoogleBluePrimary
import com.fisalia.files.ui.theme.GoogleGreen
import com.fisalia.files.ui.theme.GoogleGreenContainer
import com.fisalia.files.ui.theme.GoogleRed
import com.fisalia.files.ui.theme.GoogleRedContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanScreen(
    viewModel: CleanViewModel,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showReviewLargeFiles by remember { mutableStateOf(false) }
    var showReviewDuplicates by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (isActive) viewModel.ensureScanned()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.clean_header_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.clean_header_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.isScanning || state.isScanningDuplicates) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.scanError != null) {
                Text(
                    text = stringResource(R.string.clean_scan_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            CleanCard(
                title = stringResource(R.string.clean_junk_title),
                description = if (state.junkCleaned) stringResource(R.string.clean_success) else stringResource(R.string.clean_junk_desc),
                sizeText = if (state.junkCleaned) "0 B" else FileOperationsHelper.formatFileSize(state.totalJunkBytes),
                icon = Icons.Default.CleaningServices,
                iconTint = GoogleBluePrimary,
                bgTint = GoogleBlueContainer,
                buttonText = if (state.junkCleaned) stringResource(R.string.clean_already_clean) else stringResource(R.string.btn_clean_action),
                buttonEnabled = !state.isScanning && !state.junkCleaned && state.junkFiles.isNotEmpty(),
                onAction = { viewModel.cleanJunkFiles() }
            )

            CleanCard(
                title = stringResource(R.string.clean_large_files_title),
                description = stringResource(R.string.clean_large_files_desc),
                sizeText = FileOperationsHelper.formatFileSize(state.totalLargeBytes),
                icon = Icons.Default.VideoLibrary,
                iconTint = GoogleRed,
                bgTint = GoogleRedContainer,
                buttonText = stringResource(R.string.action_scan_storage),
                buttonEnabled = true,
                onAction = {
                    showReviewLargeFiles = true
                    viewModel.ensureLargeStorageScanned()
                }
            )

            CleanCard(
                title = stringResource(R.string.clean_duplicates_title),
                description = stringResource(R.string.clean_duplicates_desc),
                sizeText = FileOperationsHelper.formatFileSize(state.totalDuplicateBytes),
                icon = Icons.Default.FileCopy,
                iconTint = GoogleGreen,
                bgTint = GoogleGreenContainer,
                buttonText = when {
                    state.isScanningDuplicates -> stringResource(R.string.action_scanning)
                    state.duplicateGroups.isNotEmpty() -> stringResource(R.string.btn_review_files)
                    else -> stringResource(R.string.btn_scan_duplicates)
                },
                buttonEnabled = !state.isScanning && !state.isScanningDuplicates,
                onAction = {
                    if (state.duplicateGroups.isNotEmpty()) {
                        showReviewDuplicates = true
                    } else {
                        viewModel.scanForDuplicates()
                    }
                }
            )
        }
    }

    if (showReviewLargeFiles) {
        LargeStorageScanScreen(
            files = state.scannedLargeFiles,
            folders = state.largeFolders,
            isScanning = state.isScanningLargeStorage,
            hasScanned = state.hasScannedLargeStorage,
            scannedEntryCount = state.scannedEntryCount,
            errorMessage = state.largeStorageScanError,
            onScan = { viewModel.scanLargeStorage() },
            onDismiss = { showReviewLargeFiles = false },
            onDeleteSelected = { selected ->
                viewModel.deleteLargeFiles(selected)
            }
        )
    }

    if (showReviewDuplicates) {
        ReviewDuplicatesBottomSheet(
            groups = state.duplicateGroups,
            onDismiss = { showReviewDuplicates = false },
            onDeleteDuplicates = { selected ->
                viewModel.deleteDuplicateFiles(selected)
            }
        )
    }
}

@Composable
private fun CleanCard(
    title: String,
    description: String,
    sizeText: String,
    icon: ImageVector,
    iconTint: Color,
    bgTint: Color,
    buttonText: String,
    buttonEnabled: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onAction,
                    enabled = buttonEnabled,
                    modifier = Modifier.widthIn(min = 148.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleBluePrimary)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LargeStorageScanScreen(
    files: List<FileItem>,
    folders: List<FileItem>,
    isScanning: Boolean,
    hasScanned: Boolean,
    scannedEntryCount: Int,
    errorMessage: String?,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
    onDeleteSelected: (List<FileItem>) -> Unit
) {
    val selectedFiles = remember(files) { mutableStateListOf<FileItem>().apply { addAll(files) } }
    val totalSize = selectedFiles.sumOf { it.size }

    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.large_storage_scan_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onScan, enabled = !isScanning) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.action_scan_storage)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (files.isNotEmpty()) {
                Button(
                    onClick = {
                        onDeleteSelected(selectedFiles.toList())
                        onDismiss()
                    },
                    enabled = selectedFiles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.btn_delete_selected,
                            selectedFiles.size,
                            FileOperationsHelper.formatFileSize(totalSize)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
        Text(
            text = stringResource(R.string.large_storage_scan_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.large_storage_scan_progress, scannedEntryCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
            )
        }

        if (errorMessage != null) {
            Text(
                text = stringResource(R.string.large_storage_scan_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (hasScanned && files.isEmpty() && folders.isEmpty() && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                    .weight(1f, fill = true),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                    text = stringResource(R.string.large_storage_scan_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
            if (files.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.large_storage_files_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        if (selectedFiles.size == files.size) {
                            selectedFiles.clear()
                        } else {
                            selectedFiles.clear()
                            selectedFiles.addAll(files)
                        }
                    }) {
                        Text(
                            if (selectedFiles.size == files.size) {
                                stringResource(R.string.action_clear_selection)
                            } else {
                                stringResource(R.string.action_select_all)
                            }
                        )
                    }
                }
            }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                if (folders.isNotEmpty()) {
                    item(key = "large_folders_header") {
                        Text(
                            text = stringResource(R.string.large_storage_folders_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = folders,
                        key = { "folder:${it.id}" },
                        contentType = { "large_folder" }
                    ) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = GoogleBluePrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = folder.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = FileOperationsHelper.formatFileSize(folder.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (files.isNotEmpty()) {
                    item(key = "large_files_header") {
                        Text(
                            text = stringResource(R.string.large_storage_files_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                }
                    items(
                        items = files,
                    key = { "file:${it.id}" },
                        contentType = { "large_file" }
                    ) { item ->
                        val isChecked = selectedFiles.contains(item)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedFiles.remove(item) else selectedFiles.add(item)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) selectedFiles.add(item) else selectedFiles.remove(item)
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = item.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = FileOperationsHelper.formatFileSize(item.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDuplicatesBottomSheet(
    groups: List<DuplicateGroup>,
    onDismiss: () -> Unit,
    onDeleteDuplicates: (List<FileItem>) -> Unit
) {
    val selectedDuplicates = remember {
        mutableStateListOf<FileItem>().apply {
            groups.forEach { addAll(it.duplicates) }
        }
    }
    val totalSize = selectedDuplicates.sumOf { it.size }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.review_duplicates_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_duplicates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = groups,
                        key = { it.original.id },
                        contentType = { "duplicate_group" }
                    ) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = group.original.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GoogleGreenContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.original_file),
                                            color = GoogleGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = FileOperationsHelper.formatFileSize(group.original.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                group.duplicates.forEach { copy ->
                                    val isChecked = selectedDuplicates.contains(copy)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (isChecked) selectedDuplicates.remove(copy) else selectedDuplicates.add(copy)
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedDuplicates.add(copy) else selectedDuplicates.remove(copy)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(GoogleRedContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.duplicate_copy),
                                                color = GoogleRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = copy.path.substringAfterLast('/'),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDeleteDuplicates(selectedDuplicates.toList())
                        onDismiss()
                    },
                    enabled = selectedDuplicates.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoogleRed),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.btn_delete_duplicates,
                            selectedDuplicates.size,
                            FileOperationsHelper.formatFileSize(totalSize)
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
