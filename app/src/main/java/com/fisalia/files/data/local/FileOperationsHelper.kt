package com.fisalia.files.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.fisalia.files.R
import com.fisalia.files.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
            Result.failure(Exception("Folder dengan nama tersebut sudah ada."))
        } else if (newFolder.mkdirs()) {
            Result.success(newFolder)
        } else {
            Result.failure(Exception("Gagal membuat folder."))
        }
    }

    suspend fun rename(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        val parent = file.parentFile ?: return@withContext Result.failure(Exception("Tidak dapat menemukan direktori induk."))
        val target = File(parent, newName)
        if (target.exists()) {
            Result.failure(Exception("Nama '$newName' sudah digunakan."))
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
                Result.failure(Exception("Gagal mengganti nama file: ${e.localizedMessage}"))
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

    suspend fun unzip(zipFile: File, destinationDir: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destinationDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        file.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
