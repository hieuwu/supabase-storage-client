package com.hieuwu.supabasestorageclient.platform

import androidx.compose.runtime.Composable

@Composable
expect fun VideoPlayer(url: String)

@Composable
expect fun PdfViewer(url: String)
