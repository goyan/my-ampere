package dev.frx.myampere.ui

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF00897B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00251F),
    secondary = Color(0xFF26A69A),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF1565C0),
    onTertiary = Color(0xFFFFFFFF),
    error = Color(0xFFC62828),
    onError = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80CBC4),
    onPrimary = Color(0xFF00251F),
    primaryContainer = Color(0xFF00695C),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF80DEEA),
    onSecondary = Color(0xFF002022),
    tertiary = Color(0xFF90CAF9),
    onTertiary = Color(0xFF002D6E),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF5C0000),
)

@Composable
fun MyAmpereTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
