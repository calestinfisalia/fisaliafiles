package com.fisalia.files.data.network

import android.util.Log
import com.fisalia.files.data.local.FileOperationsHelper
import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.UserMessageException
import com.fisalia.files.R
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.TimeUnit

class SmbService {

    private val client: SMBClient by lazy {
        val config = SmbConfig.builder()
            .withTimeout(30, TimeUnit.SECONDS)
            .withSoTimeout(60, TimeUnit.SECONDS)
            .withBufferSize(1024 * 1024)
            .withReadBufferSize(1024 * 1024)
            .withWriteBufferSize(1024 * 1024)
            .build()
        SMBClient(config)
    }

    private fun connect(connection: NetworkConnection): Pair<Connection, Session> {
        if (connection.shareOrBasePath.isBlank()) {
            throw UserMessageException(R.string.error_smb_share_required)
        }
        if (connection.shareOrBasePath.contains('/') || connection.shareOrBasePath.contains('\\')) {
            throw UserMessageException(R.string.error_smb_share_name_only)
        }
        val conn = client.connect(connection.host, connection.port)
        val auth = if (connection.isAnonymous || connection.username.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(
                connection.username,
                connection.password.toCharArray(),
                connection.domain.ifBlank { null }
            )
        }
        val session = conn.authenticate(auth)
        return Pair(conn, session)
    }

    suspend fun getStorageCapacity(connection: NetworkConnection): Pair<Long, Long>? = withContext(Dispatchers.IO) {
        if (connection.shareOrBasePath.isBlank()) return@withContext null
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val info = try {
                    share.shareInformation
                } catch (_: Exception) {
                    null
                }
                share.close()
                if (info != null && info.totalSpace > 0L) {
                    Pair(info.totalSpace, info.callerFreeSpace)
                } else null
            } finally {
                session.close()
                conn.close()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun testConnection(connection: NetworkConnection): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                share.close()
                Result.success(true)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listFiles(connection: NetworkConnection, relativePath: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val cleanPath = relativePath.trim('/').replace('/', '\\')
                val fileInfos = share.list(cleanPath)

                val items = fileInfos
                    .filter { it.fileName != "." && it.fileName != ".." }
                    .map { info ->
                        val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                        val isHidden = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_HIDDEN.value) != 0L
                        val childPath = if (relativePath.isBlank() || relativePath == "/") {
                            info.fileName
                        } else {
                            "${relativePath.trimEnd('/')}/${info.fileName}"
                        }
                        val ext = if (!isDir && info.fileName.contains('.')) {
                            info.fileName.substringAfterLast('.', "")
                        } else ""

                        FileItem(
                            id = "${connection.id}:$childPath",
                            name = info.fileName,
                            path = childPath,
                            size = info.endOfFile,
                            isDirectory = isDir,
                            lastModified = info.changeTime.toEpochMillis(),
                            extension = ext,
                            protocol = ProtocolType.SMB,
                            connectionId = connection.id,
                            isHidden = isHidden
                        )
                    }
                share.close()
                Result.success(items)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun countDirectoryChildren(
        connection: NetworkConnection,
        directoryPaths: List<String>
    ): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                try {
                    val counts = mutableMapOf<String, Int>()
                    for (path in directoryPaths) {
                        val cleanPath = path.trim('/').replace('/', '\\')
                        try {
                            counts[path] = share.list(cleanPath)
                                .count { it.fileName != "." && it.fileName != ".." }
                        } catch (error: Exception) {
                            Log.w("SmbService", "Could not read item count for $path", error)
                        }
                    }
                    Result.success(counts)
                } finally {
                    share.close()
                }
            } finally {
                session.close()
                conn.close()
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun createFolder(connection: NetworkConnection, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val cleanPath = relativePath.trim('/').replace('/', '\\')
                share.mkdir(cleanPath)
                share.close()
                Result.success(Unit)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rename(connection: NetworkConnection, oldRelativePath: String, newRelativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val oldClean = oldRelativePath.trim('/').replace('/', '\\')
                val newClean = newRelativePath.trim('/').replace('/', '\\')
                val smbFile = share.openFile(
                    oldClean,
                    EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                smbFile.rename(newClean, true)
                smbFile.close()
                share.close()
                Result.success(Unit)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun delete(connection: NetworkConnection, relativePath: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val cleanPath = relativePath.trim('/').replace('/', '\\')
                if (isDirectory) {
                    share.rmdir(cleanPath, true)
                } else {
                    share.rm(cleanPath)
                }
                share.close()
                Result.success(Unit)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        connection: NetworkConnection,
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val cleanPath = remotePath.trim('/').replace('/', '\\')
                val smbFile = share.openFile(
                    cleanPath,
                    EnumSet.of(AccessMask.FILE_READ_DATA, AccessMask.FILE_READ_ATTRIBUTES),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                val totalBytes = try {
                    smbFile.fileInformation.standardInformation.endOfFile
                } catch (_: Exception) {
                    0L
                }
                var downloaded = 0L

                val buffer = ByteArray(256 * 1024)
                java.io.BufferedInputStream(smbFile.inputStream, 256 * 1024).use { input ->
                    java.io.BufferedOutputStream(destinationFile.outputStream(), 256 * 1024).use { output ->
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            onProgress(downloaded, totalBytes)
                        }
                        output.flush()
                    }
                }
                smbFile.close()
                share.close()
                Result.success(destinationFile)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFile(
        connection: NetworkConnection,
        localFile: File,
        remotePath: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val (conn, session) = connect(connection)
            try {
                val share = session.connectShare(connection.shareOrBasePath) as DiskShare
                val cleanPath = remotePath.trim('/').replace('/', '\\')
                val smbFile = share.openFile(
                    cleanPath,
                    EnumSet.of(AccessMask.FILE_WRITE_DATA, AccessMask.FILE_WRITE_ATTRIBUTES),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OVERWRITE_IF,
                    null
                )
                val totalBytes = localFile.length()
                var written = 0L

                localFile.inputStream().use { input ->
                    smbFile.outputStream.use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(written, totalBytes)
                        }
                    }
                }
                smbFile.close()
                share.close()
                Result.success(Unit)
            } finally {
                session.close()
                conn.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamFile(
        connection: NetworkConnection,
        remotePath: String,
        rangeStart: Long,
        rangeEnd: Long?,
        outputStream: OutputStream,
        isHead: Boolean = false
    ) {
        val (conn, session) = connect(connection)
        try {
            val share = session.connectShare(connection.shareOrBasePath) as DiskShare
            try {
                val cleanPath = remotePath.trim('/').replace('/', '\\')
                val smbFile = share.openFile(
                    cleanPath,
                    EnumSet.of(AccessMask.FILE_READ_DATA, AccessMask.FILE_READ_ATTRIBUTES),
                    null,
                    SMB2ShareAccess.ALL,
                    SMB2CreateDisposition.FILE_OPEN,
                    null
                )
                try {
                    val totalLength = smbFile.fileInformation.standardInformation.endOfFile
                    val actualStart = rangeStart.coerceIn(0L, maxOf(0L, totalLength - 1L))
                    val actualEnd = if (rangeEnd != null && rangeEnd >= actualStart) {
                        rangeEnd.coerceAtMost(totalLength - 1L)
                    } else {
                        maxOf(0L, totalLength - 1L)
                    }
                    val contentLength = if (totalLength == 0L) 0L else (actualEnd - actualStart + 1L)

                    val ext = remotePath.substringAfterLast('.', "").lowercase()
                    val mimeType = FileOperationsHelper.getMimeTypeFromExtension(ext)

                    val isPartial = rangeStart > 0L || rangeEnd != null
                    val statusLine = if (isPartial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n"
                    val contentRange = if (isPartial) "Content-Range: bytes $actualStart-$actualEnd/$totalLength\r\n" else ""

                    val header = statusLine +
                            "Content-Type: $mimeType\r\n" +
                            "Accept-Ranges: bytes\r\n" +
                            contentRange +
                            "Content-Length: $contentLength\r\n" +
                            "Connection: close\r\n\r\n"

                    outputStream.write(header.toByteArray(Charsets.UTF_8))
                    outputStream.flush()

                    if (isHead) return

                    val buffer = ByteArray(1024 * 1024)
                    var currentOffset = actualStart
                    while (currentOffset <= actualEnd) {
                        val toRead = minOf(buffer.size.toLong(), actualEnd - currentOffset + 1L).toInt()
                        val bytesRead = smbFile.read(buffer, currentOffset, 0, toRead)
                        if (bytesRead <= 0) break
                        outputStream.write(buffer, 0, bytesRead)
                        currentOffset += bytesRead
                    }
                    outputStream.flush()
                } finally {
                    smbFile.close()
                }
            } finally {
                share.close()
            }
        } finally {
            session.close()
            conn.close()
        }
    }
}
