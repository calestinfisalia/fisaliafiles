package com.fisalia.files.data.model

import com.fisalia.files.R

enum class FileCategory(val titleRes: Int, val title: String) {
    DOWNLOADS(R.string.category_downloads, "Unduhan"),
    IMAGES(R.string.category_images, "Gambar"),
    VIDEOS(R.string.category_videos, "Video"),
    AUDIO(R.string.category_audio, "Audio"),
    DOCUMENTS(R.string.category_documents, "Dokumen & lainnya"),
    APPS(R.string.category_apps, "Aplikasi"),
    ARCHIVES(R.string.category_documents, "Arsip ZIP");

    companion object {
        fun fromString(value: String): FileCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DOWNLOADS
        }
    }
}
