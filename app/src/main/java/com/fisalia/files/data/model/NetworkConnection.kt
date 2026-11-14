package com.fisalia.files.data.model

import java.util.UUID

data class NetworkConnection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: ProtocolType = ProtocolType.SMB,
    val host: String,
    val port: Int = when (type) {
        ProtocolType.SMB -> 445
        ProtocolType.FTP -> 21
        ProtocolType.FTPS -> 990
        ProtocolType.SFTP -> 22
        ProtocolType.WEBDAV -> 80
        else -> 80
    },
    val shareOrBasePath: String = "", // e.g. "SharedFolder" for SMB, or "/home/user"
    val username: String = "",
    val password: String = "",
    val isAnonymous: Boolean = false,
    val domain: String = "", // for Windows Active Directory / NetBIOS domain
    val lastConnected: Long = 0L
)
