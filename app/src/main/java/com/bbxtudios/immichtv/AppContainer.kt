package com.bbxtudios.immichtv

import android.content.Context
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.bbxtudios.immichtv.data.repository.ImmichRepository
import com.bbxtudios.immichtv.data.repository.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object AppContainer {

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var immichRepository: ImmichRepository
        private set

    fun init(context: Context) {
        if (!::settingsRepository.isInitialized) {
            settingsRepository = SettingsRepository(context.applicationContext)
            immichRepository = ImmichRepository(settingsRepository)

            val dispatcher = okhttp3.Dispatcher().apply {
                maxRequests = 8
                maxRequestsPerHost = 4
            }

            val okHttpClient = OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build()

            val coilDispatcher = kotlinx.coroutines.Dispatchers.IO.limitedParallelism(2)

            val imageLoader = ImageLoader.Builder(context.applicationContext)
                .okHttpClient(okHttpClient)
                .dispatcher(coilDispatcher)
                .interceptorDispatcher(coilDispatcher)
                .allowHardware(true)
                .allowRgb565(false)
                .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .strongReferencesEnabled(true)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(1024L * 1024L * 1024L) // 1 GB de caché en disco
                        .build()
                }
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .crossfade(false)
                .build()

            Coil.setImageLoader(imageLoader)
        }
    }
}
