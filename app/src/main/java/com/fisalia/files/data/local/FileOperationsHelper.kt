package com.fisalia.files.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.fisalia.files.R
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.UserMessageException
import com.github.junrar.Archive
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileOperationsHelper {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val formatted = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", formatted, units[digitGroups])
    }

    fun formatDate(timestamp: Long): String {
        if (timestamp <= 0) return "-"
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return getMimeTypeFromExtension(extension)
    }

    fun getMimeTypeFromExtension(extension: String): String {
        val ext = extension.lowercase().removePrefix(".")
        return when (ext) {
            "mkv" -> "video/x-matroska"
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "avi" -> "video/x-msvideo"
            "mov" -> "video/quicktime"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            "ogg" -> "audio/ogg"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "apk" -> "application/vnd.android.package-archive"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
        }
    }

    fun openFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val mimeType = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to open with generic chooser
            val chooser = Intent.createChooser(intent, context.getString(R.string.action_open_with))
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (f in files) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", f)
            uris.add(uri)
        }

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(files.first())
                putExtra(Intent.EXTRA_STREAM, uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "Bagikan file")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun createFolder(parentDir: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        val newFolder = File(parentDir, folderName)
        if (newFolder.exists()) {
            Result.failure(UserMessageException(R.string.error_folder_exists))
        } else if (newFolder.mkdirs()) {
            Result.success(newFolder)
        } else {
            Result.failure(UserMessageException(R.string.error_folder_create_failed))
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val parent = file.parentFile ?: return@withContext Result.failure(
            UserMessageException(R.string.error_parent_directory_missing)
        )
        val target = File(parent, newName)
        if (target.exists()) {
            Result.failure(UserMessageException(R.string.error_name_already_used, listOf(newName)))
        } else if (file.renameTo(target)) {
            Result.success(target)
        } else {
            try {
                if (file.isDirectory) {
                    file.copyRecursively(target, overwrite = true)
                    file.deleteRecursively()
                } else {
                    file.copyTo(target, overwrite = true)
                    file.delete()
                }
                Result.success(target)
            } catch (e: Exception) {
                Result.failure(UserMessageException(R.string.error_rename_failed, cause = e))
            }
        }
    }

    suspend fun delete(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copy(source: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(destinationDir, source.name)
            if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true)
            } else {
                source.copyTo(target, overwrite = true)
            }
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun move(source: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val target = File(destinationDir, source.name)
            if (source.renameTo(target)) {
                Result.success(target)
            } else {
                // If moving across partitions, copy and delete
                if (source.isDirectory) {
                    source.copyRecursively(target, overwrite = true)
                } else {
                    source.copyTo(target, overwrite = true)
                }
                source.deleteRecursively()
                Result.success(target)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun zipFiles(sourceFiles: List<File>, zipFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (file in sourceFiles) {
                    addFileToZip(file, file.name, zos)
                }
            }
            Result.success(zipFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addFileToZip(file: File, entryPath: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            for (child in children) {
                addFileToZip(child, "$entryPath/${child.name}", zos)
            }
        } else {
            val entry = ZipEntry(entryPath)
            zos.putNextEntry(entry)
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun extractArchive(archiveFile: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        var extractionDir: File? = null
        try {
            if (!archiveFile.isFile) {
                throw UserMessageException(R.string.error_archive_file_missing)
            }
            if (archiveFile.extension.lowercase() !in supportedArchiveExtensions) {
                throw UserMessageException(
                    R.string.error_archive_format_unsupported,
                    listOf(archiveFile.extension)
                )
            }
            if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                throw UserMessageException(R.string.error_archive_destination_create_failed)
            }

            extractionDir = createExtractionDirectory(archiveFile, destinationDir)
            when {
                archiveFile.name.endsWith(".tar.gz", ignoreCase = true) ||
                    archiveFile.name.endsWith(".tgz", ignoreCase = true) ->
                    extractTar(GzipCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))), extractionDir)
                archiveFile.name.endsWith(".tar.bz2", ignoreCase = true) ||
                    archiveFile.name.endsWith(".tbz2", ignoreCase = true) ->
                    extractTar(BZip2CompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))), extractionDir)
                archiveFile.name.endsWith(".tar.xz", ignoreCase = true) ||
                    archiveFile.name.endsWith(".txz", ignoreCase = true) ->
                    extractTar(XZCompressorInputStream(BufferedInputStream(FileInputStream(archiveFile))), extractionDir)
                archiveFile.extension.equals("zip", ignoreCase = true) ->
                    extractZip(archiveFile, extractionDir)
                archiveFile.extension.equals("rar", ignoreCase = true) ->
                    extractRar(archiveFile, extractionDir)
                archiveFile.extension.equals("7z", ignoreCase = true) ->
                    extractSevenZip(archiveFile, extractionDir)
                archiveFile.extension.equals("tar", ignoreCase = true) ->
                    extractTar(BufferedInputStream(FileInputStream(archiveFile)), extractionDir)
                archiveFile.extension.equals("gz", ignoreCase = true) ->
                    extractCompressedFile(archiveFile, extractionDir, ".gz") {
                        GzipCompressorInputStream(BufferedInputStream(it))
                    }
                archiveFile.extension.equals("bz2", ignoreCase = true) ->
                    extractCompressedFile(archiveFile, extractionDir, ".bz2") {
                        BZip2CompressorInputStream(BufferedInputStream(it))
                    }
                archiveFile.extension.equals("xz", ignoreCase = true) ->
                    extractCompressedFile(archiveFile, extractionDir, ".xz") {
                        XZCompressorInputStream(BufferedInputStream(it))
                    }
            }
            Result.success(extractionDir)
        } catch (e: Exception) {
            extractionDir?.deleteRecursively()
            Result.failure(e)
        }
    }

    private val supportedArchiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "tgz", "tbz2", "txz")

    private fun createExtractionDirectory(archiveFile: File, parent: File): File {
        val lowerName = archiveFile.name.lowercase()
        val suffix = listOf(".tar.gz", ".tar.bz2", ".tar.xz", ".tbz2", ".tgz", ".txz", ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz")
            .first { lowerName.endsWith(it) }
        val baseName = archiveFile.name.dropLast(suffix.length).ifBlank { "Extracted" }
        var candidate = File(parent, baseName)
        var index = 2
        while (candidate.exists()) {
            candidate = File(parent, "$baseName ($index)")
            index++
        }
        if (!candidate.mkdirs()) throw UserMessageException(R.string.error_archive_extraction_folder_create_failed)
        return candidate
    }

    private fun extractZip(archiveFile: File, root: File) {
        ZipInputStream(BufferedInputStream(FileInputStream(archiveFile))).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val output = resolveArchiveEntry(root, entry.name)
                if (entry.isDirectory) {
                    ensureDirectory(output)
                } else {
                    ensureDirectory(output.parentFile ?: root)
                    output.outputStream().use { input.copyTo(it) }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
    }

    private fun extractTar(input: InputStream, root: File) {
        TarArchiveInputStream(input).use { archive ->
            var entry = archive.nextTarEntry
            while (entry != null) {
                if (entry.isSymbolicLink || entry.isLink) {
                    throw UserMessageException(R.string.error_archive_links_unsupported)
                }
                val output = resolveArchiveEntry(root, entry.name)
                if (entry.isDirectory) {
                    ensureDirectory(output)
                } else {
                    ensureDirectory(output.parentFile ?: root)
                    output.outputStream().use { archive.copyTo(it) }
                }
                entry = archive.nextTarEntry
            }
        }
    }

    private fun extractSevenZip(archiveFile: File, root: File) {
        SevenZFile.builder().setFile(archiveFile).get().use { archive ->
            var entry = archive.nextEntry
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (entry != null) {
                if (!entry.isAntiItem) {
                    val output = resolveArchiveEntry(root, entry.name)
                    if (entry.isDirectory) {
                        ensureDirectory(output)
                    } else {
                        ensureDirectory(output.parentFile ?: root)
                        output.outputStream().use { destination ->
                            while (true) {
                                val count = archive.read(buffer)
                                if (count < 0) break
                                destination.write(buffer, 0, count)
                            }
                        }
                    }
                }
                entry = archive.nextEntry
            }
        }
    }

    private fun extractRar(archiveFile: File, root: File) {
        Archive(archiveFile).use { archive ->
            for (entry in archive.fileHeaders) {
                val output = resolveArchiveEntry(root, entry.fileNameString)
                if (entry.isDirectory) {
                    ensureDirectory(output)
                } else {
                    ensureDirectory(output.parentFile ?: root)
                    archive.getInputStream(entry).use { input ->
                        output.outputStream().use { input.copyTo(it) }
                    }
                }
            }
        }
    }

    private fun extractCompressedFile(
        archiveFile: File,
        root: File,
        suffix: String,
        openStream: (FileInputStream) -> InputStream
    ) {
        val outputName = archiveFile.name.dropLast(suffix.length).ifBlank { "Extracted" }
        val output = resolveArchiveEntry(root, outputName)
        openStream(FileInputStream(archiveFile)).use { input ->
            output.outputStream().use { input.copyTo(it) }
        }
    }

    private fun resolveArchiveEntry(root: File, entryName: String): File {
        val rootPath = root.canonicalFile
        val target = File(rootPath, entryName).canonicalFile
        if (target == rootPath || !target.path.startsWith(rootPath.path + File.separator)) {
            throw UserMessageException(R.string.error_archive_entry_path_invalid)
        }
        return target
    }

    private fun ensureDirectory(directory: File) {
        if (!directory.exists() && !directory.mkdirs()) {
            throw UserMessageException(R.string.error_archive_directory_create_failed)
        }
        if (!directory.isDirectory) {
            throw UserMessageException(R.string.error_archive_entry_conflicts)
        }
    }
}
