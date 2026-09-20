package fr.martinrocca.resto.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

private val SoftSage = SageSecondary.copy(alpha = 0.18f).compositeOver(SurfaceWhite)
private val DarkSurface = SurfaceWhite.copy(alpha = 0.04f).compositeOver(Ink)

internal val LightColors = lightColorScheme(
    primary = SagePrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = SoftSage,
    onPrimaryContainer = Ink,
    inversePrimary = SageSecondary,
    secondary = SageSecondary,
    onSecondary = Ink,
    secondaryContainer = SageSecondary,
    onSecondaryContainer = Ink,
    tertiary = SageSecondary,
    onTertiary = Ink,
    tertiaryContainer = SoftSage,
    onTertiaryContainer = Ink,
    background = AppBackground,
    onBackground = Ink,
    surface = SurfaceWhite,
    onSurface = Ink,
    surfaceVariant = AppBackground,
    onSurfaceVariant = SecondaryText,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = SurfaceWhite,
    outline = OutlineGray,
    outlineVariant = OutlineGray,
    scrim = Ink,
    surfaceBright = SurfaceWhite,
    surfaceDim = AppBackground,
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceWhite,
    surfaceContainer = SurfaceWhite,
    surfaceContainerHigh = SurfaceWhite,
    surfaceContainerHighest = SurfaceWhite,
    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorRed.copy(alpha = 0.12f).compositeOver(SurfaceWhite),
    onErrorContainer = Ink,
)

// Preserve the existing system dark mode, adapting the same sage/ink colors for contrast.
internal val DarkColors = darkColorScheme(
    primary = SageSecondary,
    onPrimary = Ink,
    primaryContainer = SagePrimary,
    onPrimaryContainer = SurfaceWhite,
    inversePrimary = SagePrimary,
    secondary = SageSecondary,
    onSecondary = Ink,
    secondaryContainer = SageSecondary,
    onSecondaryContainer = Ink,
    tertiary = SageSecondary,
    onTertiary = Ink,
    tertiaryContainer = SagePrimary,
    onTertiaryContainer = SurfaceWhite,
    background = Ink,
    onBackground = SurfaceWhite,
    surface = DarkSurface,
    onSurface = SurfaceWhite,
    surfaceVariant = SagePrimary,
    onSurfaceVariant = DisabledElement,
    surfaceTint = Color.Transparent,
    inverseSurface = AppBackground,
    inverseOnSurface = Ink,
    outline = SecondaryText,
    outlineVariant = SecondaryText,
    scrim = Ink,
    surfaceBright = DarkSurface,
    surfaceDim = Ink,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkSurface,
    // Keep the existing Material error hue readable in dark mode, distinct from Michelin.
    error = Color(0xFFF2B8B5),
    onError = Ink,
    errorContainer = ErrorRed.copy(alpha = 0.24f).compositeOver(Ink),
    onErrorContainer = SurfaceWhite,
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
