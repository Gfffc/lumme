package com.univesp.lumme.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightGreen = Color(0xFF2E7D32)
private val LightGreenContainer = Color(0xFFA5D6A7)
private val DarkGreen = Color(0xFF81C784)

private val LightColors = lightColorScheme(
    primary = LightGreen,
    primaryContainer = LightGreenContainer,
    secondary = Color(0xFF00796B),
    tertiary = Color(0xFFFFA000),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF)
)

private val DarkColors = darkColorScheme(
    primary = DarkGreen,
    primaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFFFCA28)
)

@Composable
fun LummeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
