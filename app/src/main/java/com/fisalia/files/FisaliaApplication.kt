package com.fisalia.files

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder

class FisaliaApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        instance = this
        com.fisalia.files.data.network.MediaStreamingServer.getInstance().ensureStarted()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    companion object {
        lateinit var instance: FisaliaApplication
            private set
    }
}
