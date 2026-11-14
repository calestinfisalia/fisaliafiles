package com.fisalia.files.data.model

import java.io.File

data class StorageItem(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean = false,
    val nameRes: Int? = null,
    val networkConnection: NetworkConnection? = null
) {
    val isNetwork: Boolean get() = networkConnection != null
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
}
