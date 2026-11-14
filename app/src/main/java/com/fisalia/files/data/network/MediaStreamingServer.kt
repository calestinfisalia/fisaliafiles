package com.fisalia.files.data.network

import android.util.Log
import com.fisalia.files.FisaliaApplication
import com.fisalia.files.data.local.ConnectionPreferences
import com.fisalia.files.data.model.ProtocolType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.Executors

class MediaStreamingServer private constructor() {

    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newCachedThreadPool()
    private val smbService = SmbService()
    @Volatile
    private var isRunning = false

    val port: Int
        get() = serverSocket?.localPort ?: 0

    @Synchronized
    fun ensureStarted() {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) return
        try {
            serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            isRunning = true
            executor.execute {
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        executor.execute {
                            handleClient(clientSocket)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaStreamingServer", "Failed to start server", e)
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.use { s ->
                val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0].uppercase()
                val pathAndQuery = parts[1]

                // Parse headers
                var rangeStart = 0L
                var rangeEnd: Long? = null
                var headerLine = reader.readLine()
                while (!headerLine.isNullOrBlank()) {
                    if (headerLine.startsWith("Range:", ignoreCase = true)) {
                        val rangeVal = headerLine.substringAfter(':').trim()
                        if (rangeVal.startsWith("bytes=", ignoreCase = true)) {
                            val rangeSpec = rangeVal.substringAfter("bytes=").trim()
                            val dashIdx = rangeSpec.indexOf('-')
                            if (dashIdx != -1) {
                                val startStr = rangeSpec.substring(0, dashIdx).trim()
                                val endStr = rangeSpec.substring(dashIdx + 1).trim()
                                if (startStr.isNotEmpty()) {
                                    rangeStart = startStr.toLongOrNull() ?: 0L
                                }
                                if (endStr.isNotEmpty()) {
                                    rangeEnd = endStr.toLongOrNull()
                                }
                            }
                        }
                    }
                    headerLine = reader.readLine()
                }

                if (!pathAndQuery.startsWith("/stream")) {
                    val notFound = "HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n"
                    s.getOutputStream().write(notFound.toByteArray())
                    s.getOutputStream().flush()
                    return
                }

                val query = pathAndQuery.substringAfter('?', "")
                val queryParams = query.split('&').associate { param ->
                    val eq = param.indexOf('=')
                    if (eq != -1) {
                        val key = URLDecoder.decode(param.substring(0, eq), "UTF-8")
                        val value = URLDecoder.decode(param.substring(eq + 1), "UTF-8")
                        key to value
                    } else {
                        param to ""
                    }
                }

                val connId = queryParams["connId"] ?: ""
                val remotePath = queryParams["path"] ?: ""

                val context = FisaliaApplication.instance.applicationContext
                val connPrefs = ConnectionPreferences(context)
                val connection = connPrefs.getConnectionById(connId)

                if (connection != null && connection.type == ProtocolType.SMB) {
                    smbService.streamFile(
                        connection = connection,
                        remotePath = remotePath,
                        rangeStart = rangeStart,
                        rangeEnd = rangeEnd,
                        outputStream = s.getOutputStream(),
                        isHead = method == "HEAD"
                    )
                } else {
                    val notFound = "HTTP/1.1 404 Connection Not Found\r\nConnection: close\r\n\r\n"
                    s.getOutputStream().write(notFound.toByteArray())
                    s.getOutputStream().flush()
                }
            }
        } catch (_: Exception) {
            // Client closed connection (e.g. video seek or stop), expected behavior
        }
    }

    fun getStreamUrl(connectionId: String, path: String): String {
        ensureStarted()
        val encConn = URLEncoder.encode(connectionId, "UTF-8")
        val encPath = URLEncoder.encode(path, "UTF-8")
        return "http://127.0.0.1:$port/stream?connId=$encConn&path=$encPath"
    }

    fun getThumbnailUrl(connectionId: String, path: String, size: Long, lastModified: Long): String {
        return "${getStreamUrl(connectionId, path)}&thumbnailVersion=$size-$lastModified"
    }

    companion object {
        @Volatile
        private var instance: MediaStreamingServer? = null

        fun getInstance(): MediaStreamingServer {
            return instance ?: synchronized(this) {
                instance ?: MediaStreamingServer().also {
                    it.ensureStarted()
                    instance = it
                }
            }
        }
    }
}
