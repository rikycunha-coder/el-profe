package com.elprofe.despiece.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFFA64200),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF360F00),
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E3EA),
    onSecondaryContainer = Color(0xFF101D23),
    tertiary = Color(0xFF3B6939),
    onTertiary = Color.White,
    background = Color(0xFFFBF8F6),
    onBackground = Color(0xFF201A17),
    surface = Color(0xFFFBF8F6),
    onSurface = Color(0xFF201A17),
    surfaceVariant = Color(0xFFF1E1D9),
    onSurfaceVariant = Color(0xFF52443C),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB592),
    onPrimary = Color(0xFF581E00),
    primaryContainer = Color(0xFF7E3000),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Color(0xFFB8C8D0),
    onSecondary = Color(0xFF233238),
    secondaryContainer = Color(0xFF3A494F),
    onSecondaryContainer = Color(0xFFD7E3EA),
    tertiary = Color(0xFFA1D39A),
    onTertiary = Color(0xFF0A390F),
    background = Color(0xFF181210),
    onBackground = Color(0xFFEDE0DA),
    surface = Color(0xFF181210),
    onSurface = Color(0xFFEDE0DA),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B8),
)

@Composable
fun TemaDespiece(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        content = content,
    )
}
