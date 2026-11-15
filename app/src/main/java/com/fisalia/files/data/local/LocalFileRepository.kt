package com.fisalia.files.data.local

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.fisalia.files.R
import com.fisalia.files.data.model.FileCategory
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.UserMessageException
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.StorageItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class LocalFileRepository(private val context: Context) {

    fun getStorageDevices(): List<StorageItem> {
        val list = mutableListOf<StorageItem>()
        val internalFile = Environment.getExternalStorageDirectory()
        val internalTotal = internalFile.totalSpace
        val internalFree = internalFile.freeSpace

        list.add(
            StorageItem(
                name = "Internal Storage",
                path = internalFile.absolutePath,
                totalBytes = internalTotal,
                freeBytes = internalFree,
                isRemovable = false,
                nameRes = R.string.internal_storage
            )
        )

        // Check for SD Card or USB OTG
        val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
        for (dir in externalDirs) {
            if (dir != null && Environment.isExternalStorageRemovable(dir)) {
                // Find root of SD card
                val rootPath = dir.absolutePath.substringBefore("/Android")
                val rootFile = File(rootPath)
                if (rootFile.exists() && rootFile.canRead()) {
                    list.add(
                        StorageItem(
                            name = "SD Card",
                            path = rootPath,
                            totalBytes = rootFile.totalSpace,
                            freeBytes = rootFile.freeSpace,
                            isRemovable = true,
                            nameRes = R.string.sd_card
                        )
                    )
                    break
                }
            }
        }

        return list
    }

    suspend fun listFilesInDirectory(path: String, showHidden: Boolean = false): List<FileItem> = withContext(Dispatchers.IO) {
        val directory = File(path)
        if (!directory.exists() || !directory.isDirectory) return@withContext emptyList()

        val files = directory.listFiles() ?: return@withContext emptyList()
        files
            .filter { showHidden || !it.isHidden }
            .map { file ->
                val isDir = file.isDirectory
                val childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
                val ext = if (!isDir) file.extension else ""
                val mime = if (!isDir) FileOperationsHelper.getMimeType(file) else null

                FileItem(
                    id = file.absolutePath,
                    name = file.name,
                    path = file.absolutePath,
                    size = if (isDir) 0L else file.length(),
                    isDirectory = isDir,
                    lastModified = file.lastModified(),
                    mimeType = mime,
                    extension = ext,
                    protocol = ProtocolType.LOCAL,
                    childCount = childCount,
                    isHidden = file.isHidden
                )
            }
            .sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )
    }

    suspend fun getCategoryTotalSizes(): Map<FileCategory, Long> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<FileCategory, Long>()

        // 1. Downloads
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadSize = downloadDir.listFiles()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
            map[FileCategory.DOWNLOADS] = downloadSize
        } catch (_: Throwable) {
            map[FileCategory.DOWNLOADS] = 0L
        }

        // 2. Images
        map[FileCategory.IMAGES] = queryMediaStoreTotalSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

        // 3. Videos
        map[FileCategory.VIDEOS] = queryMediaStoreTotalSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        // 4. Audio
        map[FileCategory.AUDIO] = queryMediaStoreTotalSize(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)

        // 5. Documents & Other
        val docExts = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub")
        map[FileCategory.DOCUMENTS] = queryMediaStoreFilesSizeByExtensions(docExts)

        // 6. Apps (APKs)
        val apkExts = setOf("apk")
        map[FileCategory.APPS] = queryMediaStoreFilesSizeByExtensions(apkExts)

        map
    }

    private fun queryMediaStoreTotalSize(uri: android.net.Uri): Long {
        var total = 0L
        try {
            val projection = arrayOf(MediaStore.MediaColumns.SIZE)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                if (sizeCol != -1) {
                    while (cursor.moveToNext()) {
                        total += cursor.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Throwable) {}
        return total
    }

    private fun queryMediaStoreFilesSizeByExtensions(extensions: Set<String>): Long {
        var total = 0L
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                while (cursor.moveToNext()) {
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val ext = path.substringAfterLast('.', "").lowercase()
                    if (extensions.contains(ext) && sizeCol != -1) {
                        total += cursor.getLong(sizeCol)
                    }
                }
            }
        } catch (_: Throwable) {}
        return total
    }

    suspend fun getCategoryFiles(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val result = mutableListOf<FileItem>()

        when (category) {
            FileCategory.DOWNLOADS -> {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                return@withContext listFilesInDirectory(downloadDir.absolutePath)
            }
            FileCategory.IMAGES -> queryMediaStore(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, result)
            FileCategory.VIDEOS -> queryMediaStore(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, result)
            FileCategory.AUDIO -> queryMediaStore(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, result)
            FileCategory.DOCUMENTS -> {
                queryMediaStoreFilesByExtensions(setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub"), result)
                if (result.isEmpty()) scanByExtensions(root, setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub"), result)
            }
            FileCategory.APPS -> {
                queryMediaStoreFilesByExtensions(setOf("apk"), result)
                if (result.isEmpty()) scanByExtensions(root, setOf("apk"), result)
            }
            FileCategory.ARCHIVES -> {
                val extensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "tbz2", "txz")
                queryMediaStoreFilesByExtensions(extensions, result)
                if (result.isEmpty()) scanByExtensions(root, extensions, result)
            }
        }

        result.sortedByDescending { it.lastModified }
    }

    private fun queryMediaStore(uri: android.net.Uri, list: MutableList<FileItem>) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists()) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol)

                    list.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            size = size,
                            isDirectory = false,
                            lastModified = date,
                            mimeType = mime,
                            extension = file.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun queryMediaStoreFilesByExtensions(extensions: Set<String>, list: MutableList<FileItem>) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = extensions.joinToString(" OR ") { "${MediaStore.MediaColumns.DATA} LIKE '%.${it}'" }
        try {
            context.contentResolver.query(uri, projection, selection, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol) ?: FileOperationsHelper.getMimeType(file)

                    list.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            size = size,
                            isDirectory = false,
                            lastModified = date,
                            mimeType = mime,
                            extension = file.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    )
                }
            }
        } catch (_: Exception) {}
    }

    private fun scanByExtensions(dir: File, extensions: Set<String>, list: MutableList<FileItem>, maxDepth: Int = 4, currentDepth: Int = 0) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.canRead()) return
        val files = dir.listFiles() ?: return

        for (f in files) {
            if (f.isDirectory) {
                if (!f.name.startsWith(".") && f.name != "Android") {
                    scanByExtensions(f, extensions, list, maxDepth, currentDepth + 1)
                }
            } else if (f.extension.lowercase() in extensions) {
                list.add(
                    FileItem(
                        id = f.absolutePath,
                        name = f.name,
                        path = f.absolutePath,
                        size = f.length(),
                        isDirectory = false,
                        lastModified = f.lastModified(),
                        mimeType = FileOperationsHelper.getMimeType(f),
                        extension = f.extension,
                        protocol = ProtocolType.LOCAL
                    )
                )
            }
        }
    }

    suspend fun getRecentFiles(limit: Int = 12): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE} OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
        try {
            context.contentResolver.query(uri, projection, selection, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext() && list.size < limit) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol) ?: FileOperationsHelper.getMimeType(file)

                    list.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            size = size,
                            isDirectory = false,
                            lastModified = date,
                            mimeType = mime,
                            extension = file.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun searchFiles(query: String, startPath: String = Environment.getExternalStorageDirectory().absolutePath): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()
        val startDir = File(startPath)
        val lowerQuery = query.lowercase()

        fun searchRecursive(dir: File, depth: Int = 0) {
            if (depth > 5 || !dir.exists() || !dir.canRead() || results.size >= 100) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.name.lowercase().contains(lowerQuery)) {
                    val isDir = f.isDirectory
                    results.add(
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            size = if (isDir) 0L else f.length(),
                            isDirectory = isDir,
                            lastModified = f.lastModified(),
                            mimeType = if (!isDir) FileOperationsHelper.getMimeType(f) else null,
                            extension = f.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    )
                }
                if (f.isDirectory && !f.name.startsWith(".") && f.name != "Android") {
                    searchRecursive(f, depth + 1)
                }
            }
        }

        searchRecursive(startDir)
        results
    }

    suspend fun getJunkFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val processedPaths = mutableSetOf<String>()

        // 1. App cache directories
        val cacheDirs = listOfNotNull(
            context.cacheDir,
            context.externalCacheDir
        )
        for (cache in cacheDirs) {
            cache.walkTopDown().maxDepth(3).filter { it.isFile }.forEach { f ->
                if (processedPaths.add(f.absolutePath)) {
                    list.add(
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            size = f.length(),
                            isDirectory = false,
                            lastModified = f.lastModified(),
                            mimeType = FileOperationsHelper.getMimeType(f),
                            extension = f.extension
                        )
                    )
                }
            }
        }

        // 2. Temp and log files in storage
        val root = Environment.getExternalStorageDirectory()
        val tempExtensions = setOf("tmp", "temp", "log", "bak", "crdownload", "part", "dmp")

        fun scanTempFiles(dir: File, depth: Int = 0) {
            if (depth > 3 || !dir.exists() || !dir.canRead() || list.size >= 100) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isFile) {
                    val ext = f.extension.lowercase()
                    if (ext in tempExtensions || f.name.startsWith("cache_") || f.name.contains(".tmp.")) {
                        if (processedPaths.add(f.absolutePath)) {
                            list.add(
                                FileItem(
                                    id = f.absolutePath,
                                    name = f.name,
                                    path = f.absolutePath,
                                    size = f.length(),
                                    isDirectory = false,
                                    lastModified = f.lastModified(),
                                    mimeType = FileOperationsHelper.getMimeType(f),
                                    extension = f.extension
                                )
                            )
                        }
                    }
                } else if (f.isDirectory && !f.name.startsWith(".") && f.name != "Android") {
                    scanTempFiles(f, depth + 1)
                }
            }
        }

        scanTempFiles(root)
        list
    }

    suspend fun getLargeFiles(thresholdBytes: Long = 30 * 1024 * 1024L): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = "${MediaStore.MediaColumns.SIZE} >= ?"
        val selectionArgs = arrayOf(thresholdBytes.toString())

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, "${MediaStore.MediaColumns.SIZE} DESC")?.use { cursor ->
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext() && list.size < 100) {
                    val path = cursor.getString(dataCol) ?: continue
                    val file = File(path)
                    if (!file.exists() || file.isDirectory) continue

                    val name = cursor.getString(nameCol) ?: file.name
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val mime = cursor.getString(mimeCol) ?: FileOperationsHelper.getMimeType(file)

                    list.add(
                        FileItem(
                            id = path,
                            name = name,
                            path = path,
                            size = size,
                            isDirectory = false,
                            lastModified = date,
                            mimeType = mime,
                            extension = file.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        if (list.isEmpty()) {
            val root = Environment.getExternalStorageDirectory()
            fun scanLarge(dir: File, depth: Int = 0) {
                if (depth > 4 || !dir.exists() || !dir.canRead() || list.size >= 50) return
                val files = dir.listFiles() ?: return
                for (f in files) {
                    if (f.isFile && f.length() >= thresholdBytes) {
                        list.add(
                            FileItem(
                                id = f.absolutePath,
                                name = f.name,
                                path = f.absolutePath,
                                size = f.length(),
                                isDirectory = false,
                                lastModified = f.lastModified(),
                                mimeType = FileOperationsHelper.getMimeType(f),
                                extension = f.extension
                            )
                        )
                    } else if (f.isDirectory && !f.name.startsWith(".") && f.name != "Android") {
                        scanLarge(f, depth + 1)
                    }
                }
            }
            scanLarge(root)
        }

        list.sortedByDescending { it.size }
    }

    suspend fun scanLargeStorage(
        thresholdBytes: Long = 30 * 1024 * 1024L,
        onProgress: (Int) -> Unit = {}
    ): LargeStorageUsageResult = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        if (!root.exists() || !root.canRead()) {
            throw UserMessageException(R.string.error_storage_unavailable_for_scan)
        }

        val scanContext = currentCoroutineContext()
        val visitedDirectories = mutableSetOf<String>()
        val largeFiles = mutableListOf<FileItem>()
        val largeFolders = mutableListOf<FileItem>()
        var scannedEntries = 0

        suspend fun scanDirectory(directory: File, depth: Int): Long {
            scanContext.ensureActive()
            val isPrivateAppData = directory.name == "data" && directory.parentFile?.name == "Android"
            if (depth > 32 || !directory.exists() || !directory.canRead() || isPrivateAppData) {
                return 0L
            }
            if (!visitedDirectories.add(directory.absolutePath)) return 0L

            val children = directory.listFiles() ?: return 0L
            var directorySize = 0L
            for (child in children) {
                scanContext.ensureActive()
                if (child.isFile) {
                    val size = child.length()
                    directorySize += size
                    if (size >= thresholdBytes) {
                        largeFiles.add(
                            FileItem(
                                id = child.absolutePath,
                                name = child.name,
                                path = child.absolutePath,
                                size = size,
                                lastModified = child.lastModified(),
                                mimeType = FileOperationsHelper.getMimeType(child),
                                extension = child.extension,
                                protocol = ProtocolType.LOCAL
                            )
                        )
                    }
                } else if (child.isDirectory) {
                    val folderSize = scanDirectory(child, depth + 1)
                    directorySize += folderSize
                    if (folderSize >= thresholdBytes) {
                        largeFolders.add(
                            FileItem(
                                id = child.absolutePath,
                                name = child.name,
                                path = child.absolutePath,
                                size = folderSize,
                                isDirectory = true,
                                lastModified = child.lastModified(),
                                protocol = ProtocolType.LOCAL
                            )
                        )
                    }
                }

                scannedEntries++
                if (scannedEntries % 128 == 0) onProgress(scannedEntries)
            }
            return directorySize
        }

        scanDirectory(root, 0)
        onProgress(scannedEntries)
        LargeStorageUsageResult(
            files = largeFiles.sortedByDescending { it.size }.take(100),
            folders = largeFolders.sortedByDescending { it.size }.take(100),
            scannedEntries = scannedEntries
        )
    }

    suspend fun getDuplicateFiles(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val allFiles = mutableListOf<File>()
        val root = Environment.getExternalStorageDirectory()

        fun collectFiles(dir: File, depth: Int = 0) {
            if (depth > 4 || !dir.exists() || !dir.canRead() || allFiles.size >= 800) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isFile && f.length() > 0) {
                    allFiles.add(f)
                } else if (f.isDirectory && !f.name.startsWith(".") && f.name != "Android") {
                    collectFiles(f, depth + 1)
                }
            }
        }

        collectFiles(root)

        val bySize = allFiles.groupBy { it.length() }.filter { it.value.size > 1 }
        val groups = mutableListOf<DuplicateGroup>()

        for ((_, sizeMatches) in bySize) {
            val byHash = sizeMatches.groupBy { file ->
                val ext = file.extension.lowercase()
                val hash = getFileHeaderHash(file)
                "$ext:$hash"
            }

            for ((_, duplicateFiles) in byHash) {
                if (duplicateFiles.size > 1) {
                    val fileItems = duplicateFiles.map { f ->
                        FileItem(
                            id = f.absolutePath,
                            name = f.name,
                            path = f.absolutePath,
                            size = f.length(),
                            isDirectory = false,
                            lastModified = f.lastModified(),
                            mimeType = FileOperationsHelper.getMimeType(f),
                            extension = f.extension,
                            protocol = ProtocolType.LOCAL
                        )
                    }
                    val original = fileItems.first()
                    val copies = fileItems.drop(1)
                    groups.add(DuplicateGroup(original, copies))
                }
            }
        }

        groups.sortedByDescending { it.totalDuplicateBytes }
    }

    private fun getFileHeaderHash(file: File): String {
        return try {
            val bytes = ByteArray(2048)
            file.inputStream().use { stream ->
                val read = stream.read(bytes)
                if (read > 0) {
                    java.util.Arrays.hashCode(bytes.copyOf(read)).toString()
                } else ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}

data class DuplicateGroup(
    val original: FileItem,
    val duplicates: List<FileItem>
) {
    val totalDuplicateBytes: Long get() = duplicates.sumOf { it.size }
}

data class LargeStorageUsageResult(
    val files: List<FileItem>,
    val folders: List<FileItem>,
    val scannedEntries: Int
)
