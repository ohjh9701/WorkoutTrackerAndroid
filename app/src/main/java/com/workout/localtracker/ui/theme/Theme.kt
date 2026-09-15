package com.workout.localtracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF17191D),
    onPrimary = Color.White,
    secondary = Color(0xFF5F6670),
    background = Color(0xFFF5F6F8),
    surface = Color.White,
    onSurface = Color(0xFF15171A),
    surfaceVariant = Color(0xFFEEF0F3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE8E9EC),
    onPrimary = Color(0xFF17191D),
    background = Color(0xFF111214),
    surface = Color(0xFF1A1C20),
    onSurface = Color(0xFFF4F5F7),
    surfaceVariant = Color(0xFF292C31)
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
