package com.example.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.example.R
import com.example.model.WallpaperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SystemWallpaperSync {

    fun signatureFor(config: WallpaperConfig, isLandscape: Boolean): String {
        val path = config.customFilePath ?: "builtin_${if (isLandscape) "land" else "port"}"
        return "$path#v${config.version}"
    }

    suspend fun applyToSystemWallpaper(
        context: Context,
        config: WallpaperConfig,
        isLandscape: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val wm = WallpaperManager.getInstance(context)
            if (!wm.isWallpaperSupported) return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !wm.isSetWallpaperAllowed) {
                return@withContext false
            }

            val customFile = config.customFilePath?.let { File(it) }?.takeIf { it.exists() }
            bitmap = if (customFile != null) {
                decodeSampledFile(customFile, maxDim = 2160)
            } else {
                val resId = if (isLandscape) {
                    R.drawable.img_wallpaper_landscape_owl
                } else {
                    R.drawable.img_wallpaper_portrait_owl
                }
                decodeSampledResource(context, resId, maxDim = 2160)
            }

            val finalBitmap = bitmap ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wm.setBitmap(finalBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            } else {
                wm.setBitmap(finalBitmap)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            bitmap?.recycle()
        }
    }

    private fun decodeSampledFile(file: File, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun decodeSampledResource(context: Context, resId: Int, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, bounds)
        val sample = computeInSampleSize(bounds.outWidth, bounds.outHeight, maxDim)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeResource(context.resources, resId, opts)
    }

    private fun computeInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (width <= 0 || height <= 0) return 1
        while (width / inSampleSize > maxDim || height / inSampleSize > maxDim) {
            inSampleSize *= 2
        }
        return inSampleSize
    }
}
