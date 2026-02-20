package com.hieuwu.supabasestorageclient.presentation.fileview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.interop.UIKitView
import platform.AVFoundation.*
import platform.AVKit.*
import platform.Foundation.*
import platform.UIKit.*
import platform.PDFKit.*
import platform.CoreGraphics.*
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
