package com.hieuwu.supabasestorageclient.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CredentialAvatar(
    name: String,
    modifier: Modifier = Modifier.size(40.dp)
) {
    val initials = remember(name) { extractInitials(name) }
    val baseColor = remember(name) {
        val colors = listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4),
            Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50),
            Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFFFF9800),
            Color(0xFFFF5722)
        )
        colors[kotlin.math.abs(name.hashCode()) % colors.size]
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(baseColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = baseColor,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

private fun extractInitials(name: String): String {
    if (name.isBlank()) return "?"

    val words = name.trim().split(Regex("[^a-zA-Z0-9]+"))
        .filter { it.isNotBlank() }

    return if (words.size >= 2) {
        (words[0].take(1) + words[1].take(1)).uppercase()
    } else if (words.isNotEmpty()) {
        words[0].take(1).uppercase()
    } else {
        "?"
    }
}