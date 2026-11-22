package com.example.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import java.io.File

object OwlImageLoaderProvider {
    @Volatile
    private var cachedLoader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return cachedLoader ?: synchronized(this) {
            cachedLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .crossfade(true)
                .build()
                .also { cachedLoader = it }
        }
    }
}

@Composable
fun AnimatedGifMedia(
    filePath: String,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { OwlImageLoaderProvider.get(context) }
    val file = remember(filePath) { File(filePath) }
    if (file.exists()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .build(),
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
