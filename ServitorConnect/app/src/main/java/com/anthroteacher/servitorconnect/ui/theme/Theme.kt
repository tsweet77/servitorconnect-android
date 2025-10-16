package com.anthroteacher.servitorconnect.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Spruce300,
    onPrimary = Earth900,
    secondary = Spruce500,
    onSecondary = Earth900,
    background = Earth900,
    onBackground = Color.White,
    surface = Spruce700,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Spruce700,
    onPrimary = Color.White,
    secondary = Spruce500,
    onSecondary = Color.White,
    background = Moss100,
    onBackground = Color(0xFF0B1510),
    surface = Color.White,
    onSurface = Color(0xFF0B1510)
)

@Composable
fun MyApplicationTheme(
    forceDark: Boolean? = null,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = forceDark ?: isSystemInDarkTheme()
    val colorScheme =
        if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
