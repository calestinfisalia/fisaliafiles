package com.fisalia.files.data.network

import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.UserMessageException
import com.fisalia.files.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.ftp.FTPSClient
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

import java.time.Duration

class FtpService {

    private fun createClient(connection: NetworkConnection): FTPClient {
        val client = if (connection.type == ProtocolType.FTPS) {
            FTPSClient("TLS", false)
        } else {
            FTPClient()
        }
        client.controlEncoding = "UTF-8"
        client.connectTimeout = 15000
        client.dataTimeout = Duration.ofMillis(20000)
        return client
    }

    private fun connect(client: FTPClient, connection: NetworkConnection) {
        client.connect(connection.host, connection.port)
        val reply = client.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            client.disconnect()
            throw UserMessageException(R.string.error_ftp_server_rejected, listOf(reply))
        }

        val username = if (connection.isAnonymous || connection.username.isBlank()) "anonymous" else connection.username
        val password = if (connection.isAnonymous) "anonymous@" else connection.password

        if (!client.login(username, password)) {
            client.disconnect()
            throw UserMessageException(R.string.error_ftp_login_failed, listOf(username))
        }

        client.enterLocalPassiveMode()
        client.setFileType(FTP.BINARY_FILE_TYPE)
    }

    suspend fun testConnection(connection: NetworkConnection): Result<Boolean> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            if (connection.shareOrBasePath.isNotBlank()) {
                client.changeWorkingDirectory(connection.shareOrBasePath)
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try {
                    client.logout()
                    client.disconnect()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun listFiles(connection: NetworkConnection, relativePath: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val fullTarget = when {
                relativePath.isBlank() || relativePath == "/" -> if (basePath.isBlank()) "/" else basePath
                basePath.isBlank() -> "/${relativePath.trimStart('/')}"
                else -> "$basePath/${relativePath.trimStart('/')}"
            }

            val ftpFiles = client.listFiles(fullTarget)
            val items = ftpFiles
                .filter { it.name != "." && it.name != ".." }
                .map { file ->
                    val isDir = file.isDirectory
                    val childPath = if (relativePath.isBlank() || relativePath == "/") file.name else "${relativePath.trimEnd('/')}/${file.name}"
                    val ext = if (!isDir && file.name.contains('.')) file.name.substringAfterLast('.', "") else ""

                    FileItem(
                        id = "${connection.id}:$childPath",
                        name = file.name,
                        path = childPath,
                        size = file.size,
                        isDirectory = isDir,
                        lastModified = file.timestamp?.timeInMillis ?: 0L,
                        extension = ext,
                        protocol = connection.type,
                        connectionId = connection.id,
                        isHidden = file.name.startsWith(".")
                    )
                }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try {
                    client.logout()
                    client.disconnect()
                } catch (_: Exception) {}
            }
        }
    }

    suspend fun createFolder(connection: NetworkConnection, relativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val fullTarget = if (basePath.isBlank()) "/${relativePath.trimStart('/')}" else "$basePath/${relativePath.trimStart('/')}"
            val success = client.makeDirectory(fullTarget)
            if (success) Result.success(Unit) else Result.failure(UserMessageException(R.string.error_ftp_create_folder))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }

    suspend fun delete(connection: NetworkConnection, relativePath: String, isDirectory: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val fullTarget = if (basePath.isBlank()) "/${relativePath.trimStart('/')}" else "$basePath/${relativePath.trimStart('/')}"
            val success = if (isDirectory) {
                client.removeDirectory(fullTarget)
            } else {
                client.deleteFile(fullTarget)
            }
            if (success) Result.success(Unit) else Result.failure(UserMessageException(R.string.error_ftp_delete_item))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }

    suspend fun rename(connection: NetworkConnection, oldRelativePath: String, newRelativePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val oldFull = if (basePath.isBlank()) "/${oldRelativePath.trimStart('/')}" else "$basePath/${oldRelativePath.trimStart('/')}"
            val newFull = if (basePath.isBlank()) "/${newRelativePath.trimStart('/')}" else "$basePath/${newRelativePath.trimStart('/')}"
            val success = client.rename(oldFull, newFull)
            if (success) Result.success(Unit) else Result.failure(UserMessageException(R.string.error_ftp_rename_item))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }

    suspend fun uploadFile(
        connection: NetworkConnection,
        localFile: File,
        remotePath: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val fullTarget = if (basePath.isBlank()) "/${remotePath.trimStart('/')}" else "$basePath/${remotePath.trimStart('/')}"

            localFile.inputStream().use { fis ->
                client.storeFile(fullTarget, fis)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }

    suspend fun downloadFile(
        connection: NetworkConnection,
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        val client = createClient(connection)
        try {
            connect(client, connection)
            val basePath = connection.shareOrBasePath.trimEnd('/')
            val fullTarget = if (basePath.isBlank()) "/${remotePath.trimStart('/')}" else "$basePath/${remotePath.trimStart('/')}"

            val ftpFile = client.mlistFile(fullTarget)
            val totalBytes = ftpFile?.size ?: 0L
            var downloaded = 0L

            destinationFile.outputStream().use { fos ->
                client.retrieveFile(fullTarget, fos)
            }
            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                try { client.disconnect() } catch (_: Exception) {}
            }
        }
    }
}
