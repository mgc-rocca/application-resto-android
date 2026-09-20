package fr.martinrocca.resto.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageLight,
    onPrimaryContainer = Anthracite,
    secondary = Olive,
    onSecondary = Color.White,
    tertiary = Color(0xFF52777D),
    background = Cream,
    onBackground = Anthracite,
    surface = WarmWhite,
    onSurface = Anthracite,
    surfaceVariant = Color(0xFFEAE7E0),
    onSurfaceVariant = WarmGray,
    outline = OutlineWarm,
    error = ErrorRed,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC9B5),
    onPrimary = Color(0xFF193526),
    primaryContainer = Color(0xFF354E3E),
    onPrimaryContainer = Color(0xFFD4E8D8),
    secondary = Color(0xFFC4C99F),
    background = Color(0xFF1C1E1C),
    onBackground = Color(0xFFE5E3DE),
    surface = Color(0xFF242724),
    onSurface = Color(0xFFE5E3DE),
    surfaceVariant = Color(0xFF383B38),
    onSurfaceVariant = Color(0xFFC8C6C0),
    outline = Color(0xFF8D918B),
)

@Composable
fun RestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = RestoTypography,
        content = content,
    )
}
