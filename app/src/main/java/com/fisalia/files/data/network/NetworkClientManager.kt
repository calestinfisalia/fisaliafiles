package com.fisalia.files.data.network

import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import com.fisalia.files.data.model.UserMessageException
import com.fisalia.files.R
import java.io.File

internal fun Throwable.isSmbShareNotFound(): Boolean {
    var cause: Throwable? = this
    while (cause != null) {
        val message = cause.message.orEmpty()
        if (
            message.contains("STATUS_BAD_NETWORK_NAME", ignoreCase = true) ||
            message.contains("0xc00000cc", ignoreCase = true)
        ) {
            return true
        }
        cause = cause.cause
    }
    return false
}

class NetworkClientManager(
    private val smbService: SmbService = SmbService(),
    private val ftpService: FtpService = FtpService()
) {

    suspend fun testConnection(connection: NetworkConnection): Result<Boolean> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.testConnection(connection)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.testConnection(connection)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun getStorageCapacity(connection: NetworkConnection): Pair<Long, Long>? {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.getStorageCapacity(connection)
            else -> null
        }
    }

    suspend fun listFiles(connection: NetworkConnection, relativePath: String): Result<List<FileItem>> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.listFiles(connection, relativePath)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.listFiles(connection, relativePath)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun countDirectoryChildren(
        connection: NetworkConnection,
        directoryPaths: List<String>
    ): Result<Map<String, Int>> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.countDirectoryChildren(connection, directoryPaths)
            else -> Result.success(emptyMap())
        }
    }

    suspend fun createFolder(connection: NetworkConnection, relativePath: String): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.createFolder(connection, relativePath)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.createFolder(connection, relativePath)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun delete(connection: NetworkConnection, relativePath: String, isDirectory: Boolean): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.delete(connection, relativePath, isDirectory)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.delete(connection, relativePath, isDirectory)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun downloadFile(
        connection: NetworkConnection,
        remotePath: String,
        destinationFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.downloadFile(connection, remotePath, destinationFile, onProgress)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.downloadFile(connection, remotePath, destinationFile, onProgress)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun rename(connection: NetworkConnection, oldRelativePath: String, newRelativePath: String): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.rename(connection, oldRelativePath, newRelativePath)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.rename(connection, oldRelativePath, newRelativePath)
            else -> Result.failure(UserMessageException(R.string.error_network_protocol_unsupported, listOf(connection.type)))
        }
    }

    suspend fun uploadFile(
        connection: NetworkConnection,
        localFile: File,
        remotePath: String,
        onProgress: (bytesWritten: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.uploadFile(connection, localFile, remotePath, onProgress)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.uploadFile(connection, localFile, remotePath, onProgress)
            else -> Result.failure(UserMessageException(R.string.error_network_upload_unsupported, listOf(connection.type)))
        }
    }
}
