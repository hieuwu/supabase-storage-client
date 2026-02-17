package com.hieuwu.supabasestorageclient.presentation.credentials.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hieuwu.supabasestorageclient.domain.model.Credential

@Composable
fun CredentialItem(
    credential: Credential,
    isExpanded: Boolean,
    isLastUsed: Boolean,
    onToggleVisibility: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isLastUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (isLastUsed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isLastUsed) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = credential.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isLastUsed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Last Used",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                }

                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isExpanded) "Hide" else "Show",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL
            val maskedUrl = if (isExpanded) {
                credential.url
            } else {
                val parts = credential.url.split("//")
                if (parts.size > 1) {
                    val protocol = parts[0]
                    val rest = parts[1]
                    val domain = rest.take(5)
                    "$protocol//$domain..."
                } else {
                    credential.url.take(10) + "..."
                }
            }
            
            CredentialField(label = "URL", value = maskedUrl)

            Spacer(modifier = Modifier.height(4.dp))

            // Key
            val maskedKey = if (isExpanded) credential.key else "•".repeat(20)
            CredentialField(label = "Secret Key", value = maskedKey)
        }
    }
}

@Composable
private fun CredentialField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
