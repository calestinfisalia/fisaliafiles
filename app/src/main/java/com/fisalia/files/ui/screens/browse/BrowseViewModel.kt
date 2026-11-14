package com.fisalia.files.ui.screens.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fisalia.files.data.local.ConnectionPreferences
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.local.LocalFileRepository
import com.fisalia.files.data.local.SettingsPreferences
import com.fisalia.files.data.model.FileCategory
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.SortField
import com.fisalia.files.data.model.SortOption
import com.fisalia.files.data.model.StorageItem
import com.fisalia.files.data.network.NetworkClientManager
import com.fisalia.files.R
import com.fisalia.files.ui.player.ActiveMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class ClipboardItem(
    val files: List<FileItem>,
    val isCut: Boolean,
    val sourceConnection: NetworkConnection? = null
)

data class BrowseUiState(
    val storageDevices: List<StorageItem> = emptyList(),
    val networkStorageDevices: List<StorageItem> = emptyList(),
    val currentPath: String = "",
    val basePath: String = "",
    val rootName: String = "",
    val activeCategory: FileCategory? = null,
    val activeConnection: NetworkConnection? = null,
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val searchResults: List<FileItem> = emptyList(),
    val selectedItemIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val sortOption: SortOption = SortOption(),
    val clipboard: ClipboardItem? = null,
    val errorMessage: String? = null,
    val isPreparingRemoteMedia: Boolean = false,
    val remoteMediaProgressText: String? = null,
    val isFavorites: Boolean = false,
    val isSafeFolder: Boolean = false,
    val recentFiles: List<FileItem> = emptyList(),
    val categorySizes: Map<FileCategory, Long> = emptyMap()
)

class BrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepo = LocalFileRepository(application)
    private val networkManager = NetworkClientManager()
    private val connectionPrefs = ConnectionPreferences(application)
    val settingsPrefs = SettingsPreferences.getInstance(application)

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private val _activeMedia = MutableStateFlow<ActiveMedia?>(null)
    val activeMedia: StateFlow<ActiveMedia?> = _activeMedia.asStateFlow()

    fun openMedia(media: ActiveMedia) {
        _activeMedia.value = media
    }

    fun openMediaItem(item: FileItem, playlist: List<FileItem> = emptyList()) {
        val mediaPlaylist = if (playlist.isNotEmpty()) {
            playlist.filter { it.isImage || it.isVideo || it.isAudio }
        } else {
            listOf(item)
        }
        val idx = mediaPlaylist.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
        val target = if (mediaPlaylist.isNotEmpty()) mediaPlaylist[idx] else item

        val playbackUri = if (target.protocol != ProtocolType.LOCAL && !target.connectionId.isNullOrBlank()) {
            val streamUrl = com.fisalia.files.data.network.MediaStreamingServer.getInstance().getStreamUrl(
                connectionId = target.connectionId,
                path = target.path
            )
            android.net.Uri.parse(streamUrl)
        } else null

        val media = when {
            target.isVideo -> ActiveMedia.Video(target, mediaPlaylist, idx, playbackUri = playbackUri)
            target.isAudio -> ActiveMedia.Audio(target, mediaPlaylist, idx, playbackUri = playbackUri)
            else -> ActiveMedia.Image(target, mediaPlaylist, idx, playbackUri = playbackUri)
        }
        _activeMedia.value = media
    }

    fun closeMedia() {
        _activeMedia.value = null
    }

    fun resetToHome() {
        _uiState.update {
            it.copy(
                currentPath = "",
                basePath = "",
                rootName = "",
                activeCategory = null,
                activeConnection = null,
                searchQuery = "",
                isSearchActive = false,
                searchResults = emptyList(),
                selectedItemIds = emptySet(),
                isSelectionMode = false,
                isFavorites = false,
                isSafeFolder = false
            )
        }
        loadStorageDevices()
    }

    init {
        loadStorageDevices()
        loadRecentFiles()
        viewModelScope.launch {
            settingsPrefs.showHiddenFiles.collect {
                if (_uiState.value.currentPath.isNotBlank() && _uiState.value.activeConnection == null) {
                    loadDirectory(_uiState.value.currentPath)
                }
            }
        }
    }

    fun loadRecentFiles() {
        viewModelScope.launch {
            val recents = localRepo.getRecentFiles()
            _uiState.update { it.copy(recentFiles = recents) }
        }
    }

    fun loadCategorySizes() {
        viewModelScope.launch {
            val sizes = localRepo.getCategoryTotalSizes()
            _uiState.update { it.copy(categorySizes = sizes) }
        }
    }

    fun openAllRecents() {
        viewModelScope.launch {
            val recents = localRepo.getRecentFiles(50)
            _uiState.update {
                it.copy(
                    activeCategory = null,
                    isFavorites = false,
                    isSafeFolder = false,
                    currentPath = "",
                    basePath = "",
                    rootName = "Recents",
                    files = recents,
                    selectedItemIds = emptySet(),
                    isSelectionMode = false
                )
            }
        }
    }

    fun loadStorageDevices() {
        viewModelScope.launch {
            loadCategorySizes()
            val devices = localRepo.getStorageDevices()
            val savedConns = connectionPrefs.getConnections()
            val initialNetworkDevices = savedConns.map { conn ->
                StorageItem(
                    name = conn.name,
                    path = conn.shareOrBasePath,
                    totalBytes = 0L,
                    freeBytes = 0L,
                    networkConnection = conn
                )
            }
            _uiState.update {
                it.copy(
                    storageDevices = devices,
                    networkStorageDevices = initialNetworkDevices
                )
            }

            // Asynchronously fetch capacity for network storage devices (e.g. SMB)
            savedConns.forEach { conn ->
                launch {
                    val capacity = networkManager.getStorageCapacity(conn)
                    if (capacity != null) {
                        _uiState.update { current ->
                            val updatedList = current.networkStorageDevices.map { item ->
                                if (item.networkConnection?.id == conn.id) {
                                    item.copy(totalBytes = capacity.first, freeBytes = capacity.second)
                                } else {
                                    item
                                }
                            }
                            current.copy(networkStorageDevices = updatedList)
                        }
                    }
                }
            }
        }
    }

    fun openStorageDevice(device: StorageItem) {
        _uiState.update {
            it.copy(
                basePath = device.path,
                currentPath = device.path,
                rootName = device.name,
                activeCategory = null,
                activeConnection = null,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
        loadDirectory(device.path)
    }

    fun openCategory(category: FileCategory) {
        _uiState.update {
            it.copy(
                activeCategory = category,
                activeConnection = null,
                rootName = category.title,
                currentPath = "",
                basePath = "",
                isLoading = true,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
        viewModelScope.launch {
            val items = localRepo.getCategoryFiles(category)
            _uiState.update {
                it.copy(
                    files = sortFiles(items, it.sortOption),
                    isLoading = false
                )
            }
        }
    }

    fun openNetworkConnection(connection: NetworkConnection) {
        _uiState.update {
            it.copy(
                activeConnection = connection,
                activeCategory = null,
                rootName = connection.name,
                basePath = "",
                currentPath = "",
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
        loadNetworkDirectory(connection, "")
    }

    fun navigateTo(path: String) {
        val state = _uiState.value
        if (state.activeConnection != null) {
            _uiState.update { it.copy(currentPath = path, selectedItemIds = emptySet(), isSelectionMode = false) }
            loadNetworkDirectory(state.activeConnection, path)
        } else {
            _uiState.update { it.copy(currentPath = path, selectedItemIds = emptySet(), isSelectionMode = false) }
            loadDirectory(path)
        }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        if (state.isSelectionMode) {
            clearSelection()
            return true
        }
        if (state.isSearchActive) {
            closeSearch()
            return true
        }

        if (state.isFavorites) {
            _uiState.update { it.copy(isFavorites = false, currentPath = "", basePath = "", files = emptyList()) }
            return true
        }

        if (state.isSafeFolder) {
            _uiState.update { it.copy(isSafeFolder = false, currentPath = "", basePath = "", files = emptyList()) }
            return true
        }

        if (state.activeConnection != null) {
            if (state.currentPath.isBlank() || state.currentPath == "/") {
                // Exit remote explorer back to main screen
                _uiState.update { it.copy(activeConnection = null, files = emptyList(), currentPath = "") }
                return true
            } else {
                val parent = state.currentPath.substringBeforeLast('/', "").ifEmpty { "" }
                navigateTo(parent)
                return true
            }
        }

        if (state.activeCategory != null) {
            _uiState.update { it.copy(activeCategory = null, files = emptyList()) }
            return true
        }

        if (state.currentPath.isNotBlank() && state.currentPath != state.basePath) {
            val parentFile = File(state.currentPath).parentFile
            if (parentFile != null && parentFile.absolutePath.startsWith(state.basePath)) {
                navigateTo(parentFile.absolutePath)
                return true
            } else {
                // Reached storage root
                _uiState.update { it.copy(currentPath = "", basePath = "", files = emptyList()) }
                return true
            }
        } else if (state.currentPath == state.basePath && state.currentPath.isNotBlank()) {
            _uiState.update { it.copy(currentPath = "", basePath = "", files = emptyList()) }
            return true
        }

        return false
    }

    private fun loadDirectory(path: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val showHidden = settingsPrefs.showHiddenFiles.value
            val files = localRepo.listFilesInDirectory(path, showHidden = showHidden)
            _uiState.update {
                it.copy(
                    files = sortFiles(files, it.sortOption),
                    isLoading = false
                )
            }
        }
    }

    private fun loadNetworkDirectory(connection: NetworkConnection, relativePath: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = networkManager.listFiles(connection, relativePath)
            result.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        files = sortFiles(items, it.sortOption),
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat jaringan: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isNotBlank()) {
            searchFiles(query)
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "", searchResults = emptyList()) }
    }

    fun searchFiles(query: String) {
        viewModelScope.launch {
            val results = localRepo.searchFiles(query)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun toggleItemSelection(id: String) {
        _uiState.update { state ->
            val updated = state.selectedItemIds.toMutableSet()
            if (updated.contains(id)) {
                updated.remove(id)
            } else {
                updated.add(id)
            }
            state.copy(
                selectedItemIds = updated,
                isSelectionMode = updated.isNotEmpty()
            )
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allIds = state.files.map { it.id }.toSet()
            state.copy(selectedItemIds = allIds, isSelectionMode = true)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet(), isSelectionMode = false) }
    }

    fun refreshCurrentView() {
        val state = _uiState.value
        when {
            state.activeConnection != null -> loadNetworkDirectory(state.activeConnection, state.currentPath)
            state.activeCategory != null -> openCategory(state.activeCategory)
            state.isFavorites -> openFavorites()
            state.isSafeFolder -> openSafeFolder()
            state.currentPath.isNotBlank() -> loadDirectory(state.currentPath)
            else -> loadStorageDevices()
        }
    }

    fun createFolder(folderName: String) {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.activeConnection != null) {
                val rel = if (state.currentPath.isBlank()) folderName else "${state.currentPath}/$folderName"
                networkManager.createFolder(state.activeConnection, rel).onSuccess {
                    loadNetworkDirectory(state.activeConnection, state.currentPath)
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            } else {
                val dir = File(state.currentPath)
                FileOperationsHelper.createFolder(dir, folderName).onSuccess {
                    refreshCurrentView()
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
        }
    }

    fun renameFile(item: FileItem, newName: String) {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.activeConnection != null) {
                val parentPath = item.path.substringBeforeLast('/', "").ifEmpty { "" }
                val newRelPath = if (parentPath.isBlank()) newName else "$parentPath/$newName"
                networkManager.rename(state.activeConnection, item.path, newRelPath).onSuccess {
                    loadNetworkDirectory(state.activeConnection, state.currentPath)
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            } else {
                FileOperationsHelper.rename(File(item.path), newName).onSuccess {
                    refreshCurrentView()
                }.onFailure { err ->
                    _uiState.update { it.copy(errorMessage = err.message) }
                }
            }
        }
    }

    fun deleteSelectedItems() {
        val state = _uiState.value
        val itemsToDelete = state.files.filter { state.selectedItemIds.contains(it.id) }
        viewModelScope.launch {
            if (state.activeConnection != null) {
                for (item in itemsToDelete) {
                    networkManager.delete(state.activeConnection, item.path, item.isDirectory)
                }
                clearSelection()
                loadNetworkDirectory(state.activeConnection, state.currentPath)
            } else {
                for (item in itemsToDelete) {
                    FileOperationsHelper.delete(File(item.path))
                }
                clearSelection()
                refreshCurrentView()
                loadStorageDevices()
            }
        }
    }

    fun deleteSingleItem(item: FileItem) {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.activeConnection != null) {
                networkManager.delete(state.activeConnection, item.path, item.isDirectory)
                loadNetworkDirectory(state.activeConnection, state.currentPath)
            } else {
                FileOperationsHelper.delete(File(item.path))
                refreshCurrentView()
                loadStorageDevices()
            }
        }
    }

    fun setClipboard(items: List<FileItem>, isCut: Boolean) {
        val currentConn = _uiState.value.activeConnection
        _uiState.update { it.copy(clipboard = ClipboardItem(files = items, isCut = isCut, sourceConnection = currentConn)) }
    }

    fun setClipboardSingle(item: FileItem, isCut: Boolean) {
        setClipboard(listOf(item), isCut)
    }

    fun clearClipboard() {
        _uiState.update { it.copy(clipboard = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun pasteClipboard() {
        val state = _uiState.value
        val clip = state.clipboard ?: return
        if (clip.files.isEmpty()) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val destConnection = state.activeConnection
            val srcConnection = clip.sourceConnection
            
            // Resolve local target path if currently empty
            val resolvedTargetPath = if (destConnection == null && state.currentPath.isBlank()) {
                localRepo.getStorageDevices().firstOrNull()?.path ?: android.os.Environment.getExternalStorageDirectory().absolutePath
            } else {
                state.currentPath
            }

            try {
                when {
                    // Scenario 1: Local -> Local
                    srcConnection == null && destConnection == null -> {
                        val targetDir = File(resolvedTargetPath)
                        if (!targetDir.exists()) targetDir.mkdirs()
                        for (item in clip.files) {
                            val src = File(item.path)
                            if (clip.isCut) {
                                FileOperationsHelper.move(src, targetDir).getOrThrow()
                            } else {
                                FileOperationsHelper.copy(src, targetDir).getOrThrow()
                            }
                        }
                        if (clip.isCut) clearClipboard()
                        refreshCurrentView()
                        loadStorageDevices()
                    }

                    // Scenario 2: Local -> Remote (NAS)
                    srcConnection == null && destConnection != null -> {
                        val cleanTarget = resolvedTargetPath.trim('/')
                        for (item in clip.files) {
                            val srcFile = File(item.path)
                            val remoteDest = if (cleanTarget.isBlank()) item.name else "$cleanTarget/${item.name}"
                            if (item.isDirectory) {
                                uploadDirectoryRecursively(destConnection, srcFile, remoteDest)
                            } else {
                                networkManager.uploadFile(destConnection, srcFile, remoteDest).getOrThrow()
                            }
                            if (clip.isCut) {
                                FileOperationsHelper.delete(srcFile).getOrThrow()
                            }
                        }
                        if (clip.isCut) clearClipboard()
                        loadNetworkDirectory(destConnection, resolvedTargetPath)
                    }

                    // Scenario 3: Remote (NAS) -> Local
                    srcConnection != null && destConnection == null -> {
                        val targetDir = File(resolvedTargetPath)
                        if (!targetDir.exists()) targetDir.mkdirs()
                        for (item in clip.files) {
                            val localTarget = File(targetDir, item.name)
                            if (item.isDirectory) {
                                downloadDirectoryRecursively(srcConnection, item.path, localTarget)
                            } else {
                                networkManager.downloadFile(srcConnection, item.path, localTarget).getOrThrow()
                            }
                            if (clip.isCut) {
                                networkManager.delete(srcConnection, item.path, item.isDirectory).getOrThrow()
                            }
                        }
                        if (clip.isCut) clearClipboard()
                        refreshCurrentView()
                        loadStorageDevices()
                    }

                    // Scenario 4: Remote -> Remote
                    srcConnection != null && destConnection != null -> {
                        val cacheDir = File(getApplication<Application>().cacheDir, "remote_transfer")
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val cleanTarget = resolvedTargetPath.trim('/')

                        for (item in clip.files) {
                            val tempLocal = File(cacheDir, item.name)
                            val remoteDest = if (cleanTarget.isBlank()) item.name else "$cleanTarget/${item.name}"
                            if (item.isDirectory) {
                                downloadDirectoryRecursively(srcConnection, item.path, tempLocal)
                                uploadDirectoryRecursively(destConnection, tempLocal, remoteDest)
                                tempLocal.deleteRecursively()
                            } else {
                                networkManager.downloadFile(srcConnection, item.path, tempLocal).getOrThrow()
                                networkManager.uploadFile(destConnection, tempLocal, remoteDest).getOrThrow()
                                tempLocal.delete()
                            }
                            if (clip.isCut) {
                                networkManager.delete(srcConnection, item.path, item.isDirectory).getOrThrow()
                            }
                        }
                        if (clip.isCut) clearClipboard()
                        loadNetworkDirectory(destConnection, resolvedTargetPath)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Gagal menempel file: ${e.localizedMessage}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun uploadDirectoryRecursively(connection: NetworkConnection, localDir: File, remoteDir: String) {
        networkManager.createFolder(connection, remoteDir).getOrThrow()
        val children = localDir.listFiles() ?: return
        for (child in children) {
            val childRemote = "$remoteDir/${child.name}"
            if (child.isDirectory) {
                uploadDirectoryRecursively(connection, child, childRemote)
            } else {
                networkManager.uploadFile(connection, child, childRemote).getOrThrow()
            }
        }
    }

    private suspend fun downloadDirectoryRecursively(connection: NetworkConnection, remoteDir: String, localDir: File) {
        if (!localDir.exists()) localDir.mkdirs()
        val listRes = networkManager.listFiles(connection, remoteDir)
        val items = listRes.getOrThrow()
        for (item in items) {
            val localChild = File(localDir, item.name)
            if (item.isDirectory) {
                downloadDirectoryRecursively(connection, item.path, localChild)
            } else {
                networkManager.downloadFile(connection, item.path, localChild).getOrThrow()
            }
        }
    }

    fun zipSelectedItems(zipName: String) {
        val state = _uiState.value
        val itemsToZip = state.files.filter { state.selectedItemIds.contains(it.id) }.map { File(it.path) }
        val targetZip = File(state.currentPath, "$zipName.zip")
        viewModelScope.launch {
            FileOperationsHelper.zipFiles(itemsToZip, targetZip).onSuccess {
                clearSelection()
                loadDirectory(state.currentPath)
            }
        }
    }

    fun sortFiles(files: List<FileItem>, option: SortOption): List<FileItem> {
        val comparator = when (option.field) {
            SortField.NAME -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            SortField.DATE -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortField.SIZE -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
            SortField.TYPE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.extension }
        }
        return if (option.isAscending) files.sortedWith(comparator) else files.sortedWith(comparator).reversed()
    }

    fun setSortOption(option: SortOption) {
        _uiState.update {
            it.copy(
                sortOption = option,
                files = sortFiles(it.files, option)
            )
        }
    }

    fun prepareRemoteFileForPlayback(
        fileItem: FileItem,
        onReady: (File) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val connection = _uiState.value.activeConnection
            ?: connectionPrefs.getConnectionById(fileItem.connectionId ?: "")
        if (connection == null) {
            onError("Koneksi jaringan tidak ditemukan")
            return
        }

        _uiState.update { it.copy(isPreparingRemoteMedia = true, remoteMediaProgressText = null) }
        viewModelScope.launch {
            val cacheDir = File(getApplication<Application>().cacheDir, "remote_media_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val safeName = fileItem.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(cacheDir, "${fileItem.protocol.name}_${fileItem.id.hashCode()}_$safeName")

            // If already cached and valid size
            if (targetFile.exists() && targetFile.length() > 0L && (fileItem.size <= 0L || targetFile.length() == fileItem.size)) {
                _uiState.update { it.copy(isPreparingRemoteMedia = false, remoteMediaProgressText = null) }
                onReady(targetFile)
                return@launch
            }

            val tempFile = File(cacheDir, "${targetFile.name}.tmp")
            if (tempFile.exists()) tempFile.delete()

            val result = networkManager.downloadFile(connection, fileItem.path, tempFile) { read, total ->
                if (total > 0L) {
                    val readStr = FileOperationsHelper.formatFileSize(read)
                    val totalStr = FileOperationsHelper.formatFileSize(total)
                    val pct = (read * 100 / total).toInt()
                    _uiState.update { it.copy(remoteMediaProgressText = "$readStr / $totalStr ($pct%)") }
                } else {
                    val readStr = FileOperationsHelper.formatFileSize(read)
                    _uiState.update { it.copy(remoteMediaProgressText = readStr) }
                }
            }
            _uiState.update { it.copy(isPreparingRemoteMedia = false, remoteMediaProgressText = null) }
            result.onSuccess { downloaded ->
                if (tempFile.renameTo(targetFile)) {
                    onReady(targetFile)
                } else {
                    onReady(downloaded)
                }
            }.onFailure { err ->
                if (tempFile.exists()) tempFile.delete()
                onError(err.localizedMessage ?: "Gagal mengunduh file media")
            }
        }
    }

    fun openFavorites() {
        val app = getApplication<Application>()
        val favPaths = settingsPrefs.getFavorites()
        val favFiles = favPaths.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) {
                val mime = FileOperationsHelper.getMimeType(file)
                FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = file.isDirectory,
                    mimeType = mime,
                    extension = file.extension,
                    protocol = ProtocolType.LOCAL,
                    childCount = if (file.isDirectory) file.list()?.size ?: 0 else 0
                )
            } else null
        }
        _uiState.update {
            it.copy(
                basePath = "favorites://",
                currentPath = "favorites://",
                rootName = app.getString(R.string.favorites),
                activeCategory = null,
                activeConnection = null,
                isFavorites = true,
                isSafeFolder = false,
                files = sortFiles(favFiles, it.sortOption),
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
    }

    fun toggleFavorite(fileItem: FileItem) {
        settingsPrefs.toggleFavorite(fileItem.path)
        if (_uiState.value.isFavorites) {
            openFavorites()
        }
    }

    fun isFavorite(fileItem: FileItem): Boolean {
        return settingsPrefs.getFavorites().contains(fileItem.path)
    }

    fun openSafeFolder() {
        val app = getApplication<Application>()
        val safeDir = File(app.filesDir, ".safe_folder").apply { mkdirs() }
        _uiState.update {
            it.copy(
                basePath = safeDir.absolutePath,
                currentPath = safeDir.absolutePath,
                rootName = app.getString(R.string.safe_folder),
                activeCategory = null,
                activeConnection = null,
                isFavorites = false,
                isSafeFolder = true,
                selectedItemIds = emptySet(),
                isSelectionMode = false
            )
        }
        loadDirectory(safeDir.absolutePath)
    }

    fun moveToSafeFolder(fileItem: FileItem, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val safeDir = File(app.filesDir, ".safe_folder").apply { mkdirs() }
            val srcFile = File(fileItem.path)
            if (!srcFile.exists()) {
                withContext(Dispatchers.Main) { onError("File tidak ditemukan") }
                return@launch
            }
            val destFile = File(safeDir, srcFile.name)
            val success = srcFile.renameTo(destFile)
            withContext(Dispatchers.Main) {
                if (success) {
                    loadDirectory(_uiState.value.currentPath)
                    onSuccess()
                } else {
                    onError("Gagal memindahkan ke Folder Aman")
                }
            }
        }
    }
}
