package fr.martinrocca.resto.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {
    @Test
    fun `sage and ink text remains readable in the light and existing dark themes`() {
        listOf(LightColors, DarkColors).forEach { colors ->
            val pairs = listOf(
                colors.onPrimary to colors.primary,
                colors.onSecondary to colors.secondary,
                colors.onPrimaryContainer to colors.primaryContainer,
                colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onBackground to colors.background,
                colors.onSurface to colors.surface,
                colors.onSurfaceVariant to colors.surface,
                colors.primary to colors.background,
            )
            pairs.forEach { (text, background) ->
                assertTrue("Text $text on $background must reach 4.5:1", contrast(text, background) >= 4.5f)
            }
        }
    }

    private fun contrast(first: Color, second: Color): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
    }
}
