package com.hieuwu.supabasestorageclient.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.hieuwu.supabasestorageclient.domain.model.AppTheme

object SupaBucktColors {
    // Core brand
    val Primary          = Color(0xFF34B27B)   // Jungle Green – accents, buttons, selections
    val PrimaryContainer = Color(0xFF2A8E62)   // Darker green for pressed/hovered states
    val OnPrimary        = Color.White

    // Success / positive states (uploads, etc.)
    val Success          = Color(0xFF34B27B)
    val OnSuccess        = Color.White

    // Neutral palette
    val SurfaceLight     = Color.White
    val SurfaceVariantLight = Color(0xFFF8F9FA)  // Athens Gray – cards, lists
    val OutlineLight     = Color(0xFFD0D4D7)
    val OnSurfaceLight   = Color(0xFF11181C)     // Bunker – main text

    val SurfaceDark      = Color(0xFF0F1417)     // Very dark Bunker-inspired
    val SurfaceVariantDark  = Color(0xFF1A2024)
    val OutlineDark      = Color(0xFF4A5A64)
    val OnSurfaceDark    = Color(0xFFEDEEF0)     // Light gray text

    // Common
    val Error            = Color(0xFFEF4444)
    val OnError          = Color.White
    val Warning          = Color(0xFFF59E0B)
    val Secondary        = Color(0xFF6B7280)     // Muted gray for subtitles, icons
    val OnSecondary      = Color.White
}

// Light theme
private val LightColorScheme = lightColorScheme(
    primary = SupaBucktColors.Primary,
    onPrimary = SupaBucktColors.OnPrimary,
    primaryContainer = SupaBucktColors.PrimaryContainer,
    surface = SupaBucktColors.SurfaceLight,
    surfaceVariant = SupaBucktColors.SurfaceVariantLight,
    onSurface = SupaBucktColors.OnSurfaceLight,
    background = SupaBucktColors.SurfaceLight,
    onBackground = SupaBucktColors.OnSurfaceLight,
    outline = SupaBucktColors.OutlineLight,
    error = SupaBucktColors.Error,
    onError = SupaBucktColors.OnError,
    surfaceContainer = SupaBucktColors.SurfaceLight,
    surfaceContainerLow = SupaBucktColors.SurfaceLight,
    surfaceContainerHigh = SupaBucktColors.SurfaceVariantLight,
    surfaceContainerHighest = SupaBucktColors.SurfaceVariantLight,
    surfaceContainerLowest = SupaBucktColors.SurfaceLight,
)

// Dark theme
private val DarkColorScheme = darkColorScheme(
    primary = SupaBucktColors.Primary,
    onPrimary = SupaBucktColors.OnPrimary,
    primaryContainer = SupaBucktColors.PrimaryContainer,
    surface = SupaBucktColors.SurfaceDark,
    surfaceVariant = SupaBucktColors.SurfaceVariantDark,
    onSurface = SupaBucktColors.OnSurfaceDark,
    background = SupaBucktColors.SurfaceDark,
    onBackground = SupaBucktColors.OnSurfaceDark,
    outline = SupaBucktColors.OutlineDark,
    error = SupaBucktColors.Error,
    onError = SupaBucktColors.OnError,
    surfaceContainer = SupaBucktColors.SurfaceDark,
    surfaceContainerLow = SupaBucktColors.SurfaceDark,
    surfaceContainerHigh = SupaBucktColors.SurfaceVariantDark,
    surfaceContainerHighest = SupaBucktColors.SurfaceVariantDark,
    surfaceContainerLowest = SupaBucktColors.SurfaceDark,
)


@Composable
fun SupaBucktTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        content = content
    )
}
