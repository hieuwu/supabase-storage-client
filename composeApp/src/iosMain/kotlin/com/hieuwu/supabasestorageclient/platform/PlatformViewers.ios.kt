@file:OptIn(ExperimentalForeignApi::class)

package com.hieuwu.supabasestorageclient.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.interop.UIKitView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.AVKit.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.PDFKit.*
import platform.CoreGraphics.*
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode
import kotlinx.cinterop.*

@Composable
actual fun VideoPlayer(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    val player = AVPlayer.playerWithURL(nsUrl)
    
    UIKitView(
        factory = {
            val playerViewController = AVPlayerViewController()
            playerViewController.player = player
            playerViewController.view.apply {
                setFrame(CGRectMake(0.0, 0.0, 0.0, 0.0)) // Framework will handle sizing
            }
            player.play()
            playerViewController.view
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            // Update logic if needed
        }
    )
}

/**
 * Coil has no animated-GIF decoder on iOS (`coil-gif` is Android-only), so decode the frames with
 * ImageIO and let UIKit drive the animation.
 */
@Composable
actual fun GifViewer(url: String) {
    var animatedImage by remember(url) { mutableStateOf<UIImage?>(null) }

    LaunchedEffect(url) {
        animatedImage = withContext(Dispatchers.Default) { decodeAnimatedImage(url) }
    }

    val image = animatedImage
    if (image == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        UIKitView(
            factory = {
                UIImageView().apply {
                    contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                    clipsToBounds = true
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.image = image
                view.startAnimating()
            }
        )
    }
}

private const val DEFAULT_FRAME_DELAY = 0.1

private fun decodeAnimatedImage(url: String): UIImage? {
    val nsUrl = NSURL.URLWithString(url) ?: return null
    val data = NSData.dataWithContentsOfURL(nsUrl) ?: return null
    val cfData: CFDataRef = CFBridgingRetain(data)?.reinterpret() ?: return null
    try {
        val source = CGImageSourceCreateWithData(cfData, null) ?: return null
        try {
            val frameCount = CGImageSourceGetCount(source).toInt()
            if (frameCount <= 1) return UIImage.imageWithData(data)

            val frames = ArrayList<UIImage>(frameCount)
            var duration = 0.0
            for (index in 0 until frameCount) {
                val frame = CGImageSourceCreateImageAtIndex(source, index.convert(), null) ?: continue
                frames.add(UIImage.imageWithCGImage(frame))
                CGImageRelease(frame)
                duration += frameDelayAt(source, index)
            }
            // animatedImageWithImages spreads `duration` evenly, so per-frame delays only shape the total.
            return if (frames.isEmpty()) null else UIImage.animatedImageWithImages(frames, duration)
        } finally {
            CFRelease(source)
        }
    } finally {
        CFBridgingRelease(cfData)
    }
}

private fun frameDelayAt(source: CGImageSourceRef, index: Int): Double {
    val properties = CGImageSourceCopyPropertiesAtIndex(source, index.convert(), null)
        ?: return DEFAULT_FRAME_DELAY
    val gifProperties = (CFBridgingRelease(properties) as? Map<*, *>)?.get("{GIF}") as? Map<*, *>
        ?: return DEFAULT_FRAME_DELAY
    val delay = (gifProperties["UnclampedDelayTime"] as? NSNumber)?.doubleValue
        ?: (gifProperties["DelayTime"] as? NSNumber)?.doubleValue
        ?: return DEFAULT_FRAME_DELAY
    // Browsers clamp near-zero delays; match that so fast GIFs don't play back instantly.
    return if (delay < 0.011) DEFAULT_FRAME_DELAY else delay
}

@Composable
actual fun PdfViewer(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    
    UIKitView(
        factory = {
            val pdfView = PDFView()
            val document = PDFDocument(uRL = nsUrl)
            pdfView.setDocument(document)
            pdfView.setAutoScales(true)
            pdfView
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
             // Update logic if needed
        }
    )
}
