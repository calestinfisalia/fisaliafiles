package com.fisalia.files.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.fisalia.files.data.local.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = GoogleBlueSecondary,
    onPrimary = GoogleBlueOnContainer,
    primaryContainer = GoogleBlueSecondary.copy(alpha = 0.2f),
    onPrimaryContainer = DarkOnSurface,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = GoogleBluePrimary,
    onPrimary = LightSurface,
    primaryContainer = GoogleBlueContainer,
    onPrimaryContainer = GoogleBlueOnContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun FisaliaFilesTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    darkTheme: Boolean = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    },
    dynamicColor: Boolean = true,
    isTransparentBackground: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = if (isTransparentBackground) {
        val transColor = androidx.compose.ui.graphics.Color.Transparent
        val cardSurface = if (darkTheme) {
            DarkSurface.copy(alpha = 0.55f)
        } else {
            LightSurface.copy(alpha = 0.65f)
        }
        val cardVariant = if (darkTheme) {
            DarkSurfaceVariant.copy(alpha = 0.5f)
        } else {
            LightSurfaceVariant.copy(alpha = 0.6f)
        }
        baseScheme.copy(
            background = transColor,
            surface = cardSurface,
            surfaceVariant = cardVariant
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
