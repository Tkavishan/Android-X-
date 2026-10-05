package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VaultDarkColorScheme = darkColorScheme(
    primary = VaultGold,
    onPrimary = VaultBlack,
    primaryContainer = VaultCharcoalElevated,
    onPrimaryContainer = VaultGoldLight,
    secondary = VaultGoldLight,
    onSecondary = VaultBlack,
    secondaryContainer = VaultCharcoal,
    onSecondaryContainer = VaultTextPrimary,
    tertiary = VaultGoldDark,
    onTertiary = Color.White,
    background = VaultBlack,
    onBackground = VaultTextPrimary,
    surface = VaultBlackSurface,
    onSurface = VaultTextPrimary,
    surfaceVariant = VaultCharcoal,
    onSurfaceVariant = VaultTextSecondary,
    outline = VaultCharcoalBorder,
    outlineVariant = VaultGoldBorder,
    error = VaultError,
    onError = Color.White
)

@Composable
fun VaultXTheme(
    content: @Composable () -> Unit
) {
    // VaultX is intentionally a dedicated dark matte black & metallic gold luxury security interface
    MaterialTheme(
        colorScheme = VaultDarkColorScheme,
        typography = Typography,
        content = content
    )
}
