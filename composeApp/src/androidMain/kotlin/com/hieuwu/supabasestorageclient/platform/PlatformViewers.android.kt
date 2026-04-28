package com.hieuwu.supabasestorageclient.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.net.Uri
import android.webkit.WebView
import android.widget.MediaController

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
