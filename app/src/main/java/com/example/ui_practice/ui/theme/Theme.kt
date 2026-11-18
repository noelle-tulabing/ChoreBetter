package com.example.ui_practice.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun UI_practiceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF171717), onPrimary = Color.White,
        secondary = Color(0xFF555555), secondaryContainer = Color(0xFFE8E8E8), onSecondaryContainer = Color(0xFF171717),
        background = Color.White, surface = Color.White, onSurface = Color(0xFF171717),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9F9F9),
        surfaceContainer = Color(0xFFF3F3F3), surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE5E5E5),
        surfaceTint = Color.Transparent, surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF595959), outline = Color(0xFF747474)
    ), typography = Typography, content = content)
}
