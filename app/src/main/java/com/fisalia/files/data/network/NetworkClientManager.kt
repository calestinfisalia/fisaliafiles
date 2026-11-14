package com.fisalia.files.data.network

import com.fisalia.files.data.model.FileItem
import com.fisalia.files.data.model.NetworkConnection
import com.fisalia.files.data.model.ProtocolType
import java.io.File

class NetworkClientManager(
    private val smbService: SmbService = SmbService(),
    private val ftpService: FtpService = FtpService()
) {

    suspend fun testConnection(connection: NetworkConnection): Result<Boolean> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.testConnection(connection)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.testConnection(connection)
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
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
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
        }
    }

    suspend fun createFolder(connection: NetworkConnection, relativePath: String): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.createFolder(connection, relativePath)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.createFolder(connection, relativePath)
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
        }
    }

    suspend fun delete(connection: NetworkConnection, relativePath: String, isDirectory: Boolean): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.delete(connection, relativePath, isDirectory)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.delete(connection, relativePath, isDirectory)
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
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
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
        }
    }

    suspend fun rename(connection: NetworkConnection, oldRelativePath: String, newRelativePath: String): Result<Unit> {
        return when (connection.type) {
            ProtocolType.SMB -> smbService.rename(connection, oldRelativePath, newRelativePath)
            ProtocolType.FTP, ProtocolType.FTPS -> ftpService.rename(connection, oldRelativePath, newRelativePath)
            else -> Result.failure(UnsupportedOperationException("Protokol ${connection.type} belum didukung."))
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
            else -> Result.failure(UnsupportedOperationException("Upload belum didukung untuk protokol ${connection.type}."))
        }
    }
}
