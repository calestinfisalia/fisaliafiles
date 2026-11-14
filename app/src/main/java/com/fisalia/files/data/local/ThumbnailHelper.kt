package com.fisalia.files.data.local

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import java.io.File
import java.io.FileInputStream

object ThumbnailHelper {
    private val memoryCache = object : LruCache<String, Any>(150) {}

    fun getAudioCover(context: Context? = null, path: String): Bitmap? {
        val cached = memoryCache.get("audio_$path") as? Bitmap
        if (cached != null && !cached.isRecycled) return cached

        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        var bitmap: Bitmap? = null

        // 1. Try MediaMetadataRetriever via FileInputStream descriptor
        try {
            FileInputStream(file).use { fis ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(fis.fd)
                    val picture = retriever.embeddedPicture
                    if (picture != null) {
                        bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    }
                } finally {
                    try { retriever.release() } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        // 2. Fallback: Query MediaStore album art if context is provided
        if (bitmap == null && context != null) {
            try {
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Audio.Media.ALBUM_ID)
                val selection = "${MediaStore.Audio.Media.DATA} = ?"
                val selectionArgs = arrayOf(path)
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                        if (albumIdCol >= 0) {
                            val albumId = cursor.getLong(albumIdCol)
                            if (albumId >= 0) {
                                val albumUri = ContentUris.withAppendedId(
                                    Uri.parse("content://media/external/audio/albumart"),
                                    albumId
                                )
                                context.contentResolver.openInputStream(albumUri)?.use { stream ->
                                    bitmap = BitmapFactory.decodeStream(stream)
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        if (bitmap != null) {
            memoryCache.put("audio_$path", bitmap)
        }
        return bitmap
    }

    fun getVideoThumbnail(context: Context? = null, path: String): Bitmap? {
        val cached = memoryCache.get("video_$path") as? Bitmap
        if (cached != null && !cached.isRecycled) return cached

        val file = File(path)
        if (!file.exists() || !file.canRead()) return null

        var bitmap: Bitmap? = null

        // 1. Try MediaMetadataRetriever via FileInputStream descriptor (most reliable)
        try {
            FileInputStream(file).use { fis ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(fis.fd)
                    bitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                } finally {
                    try { retriever.release() } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}

        // 2. Try ThumbnailUtils on API 29+ if still null
        if (bitmap == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                bitmap = ThumbnailUtils.createVideoThumbnail(file, Size(256, 256), null)
            } catch (_: Throwable) {}
        }

        // 3. Fallback: Query MediaStore Video Thumbnail if context provided
        if (bitmap == null && context != null) {
            try {
                val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(MediaStore.Video.Media._ID)
                val selection = "${MediaStore.Video.Media.DATA} = ?"
                val selectionArgs = arrayOf(path)
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                        if (idCol >= 0) {
                            val id = cursor.getLong(idCol)
                            bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                                context.contentResolver,
                                id,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        if (bitmap != null) {
            memoryCache.put("video_$path", bitmap)
        }
        return bitmap
    }

    fun getPdfThumbnail(path: String): Bitmap? {
        val cached = memoryCache.get("pdf_$path") as? Bitmap
        if (cached != null && !cached.isRecycled) return cached

        return try {
            val file = File(path)
            if (!file.exists() || !file.canRead()) return null
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val width = 240
                val height = (width * (page.height.toFloat() / page.width.toFloat())).toInt().coerceIn(120, 360)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()
                memoryCache.put("pdf_$path", bitmap)
                bitmap
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun getApkIcon(context: Context, path: String): Drawable? {
        val cached = memoryCache.get("apk_$path") as? Drawable
        if (cached != null) return cached

        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(path, 0)
            val appInfo = info?.applicationInfo
            if (appInfo != null) {
                appInfo.sourceDir = path
                appInfo.publicSourceDir = path
                val icon = appInfo.loadIcon(pm)
                if (icon != null) {
                    memoryCache.put("apk_$path", icon)
                }
                icon
            } else null
        } catch (_: Throwable) {
            null
        }
    }
}
