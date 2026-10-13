package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoxelDarkColorScheme = darkColorScheme(
    primary = VoxelEmerald,
    onPrimary = Color(0xFF052916),
    primaryContainer = Color(0xFF14492D),
    onPrimaryContainer = Color(0xFFB5F8D3),
    secondary = VoxelDiamondCyan,
    onSecondary = Color(0xFF042B30),
    secondaryContainer = Color(0xFF13484E),
    onSecondaryContainer = Color(0xFFC2F7FC),
    tertiary = VoxelTorchGold,
    onTertiary = Color(0xFF332000),
    background = VoxelObsidian,
    onBackground = VoxelTextPrimary,
    surface = VoxelSlate,
    onSurface = VoxelTextPrimary,
    surfaceVariant = VoxelSurfaceElevated,
    onSurfaceVariant = VoxelTextSecondary,
    error = VoxelCrimsonHeart,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VoxelDarkColorScheme,
        typography = Typography,
        content = content
    )
}
