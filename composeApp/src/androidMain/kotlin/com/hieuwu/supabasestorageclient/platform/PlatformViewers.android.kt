package com.hieuwu.supabasestorageclient.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Build
import android.widget.VideoView
import android.net.Uri
import android.webkit.WebView
import android.widget.MediaController
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

@Composable
actual fun VideoPlayer(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            VideoView(context).apply {
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setVideoURI(Uri.parse(url))
                start()
            }
        },
        update = { view ->
            view.setVideoURI(Uri.parse(url))
        }
    )
}

@Composable
actual fun GifViewer(url: String) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    AsyncImage(
        model = url,
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}

@Composable
actual fun PdfViewer(url: String) {
    // For a simple implementation, we can use a WebView or a dedicated PDF library.
    // Given the constraints, let's use a WebView to load the PDF URL.
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                // Google Docs viewer is often used to embed PDFs in WebViews
                loadUrl("https://docs.google.com/viewer?url=$url&embedded=true")
            }
        },
        update = { view ->
            view.loadUrl("https://docs.google.com/viewer?url=$url&embedded=true")
        }
    )
}
