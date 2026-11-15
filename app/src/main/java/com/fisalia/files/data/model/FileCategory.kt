package com.fisalia.files.data.model

import com.fisalia.files.R

enum class FileCategory(val titleRes: Int) {
    DOWNLOADS(R.string.category_downloads),
    IMAGES(R.string.category_images),
    VIDEOS(R.string.category_videos),
    AUDIO(R.string.category_audio),
    DOCUMENTS(R.string.category_documents),
    APPS(R.string.category_apps),
    ARCHIVES(R.string.category_archives);

    companion object {
        fun fromString(value: String): FileCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DOWNLOADS
        }
    }
}
