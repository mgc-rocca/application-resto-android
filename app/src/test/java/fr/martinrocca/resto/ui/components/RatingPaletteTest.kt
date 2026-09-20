package fr.martinrocca.resto.ui.components

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import fr.martinrocca.resto.theme.LightColors
import fr.martinrocca.resto.theme.DarkColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingPaletteTest {
    @Test
    fun `every rating uses its specified RGB and opacity`() {
        val specification = listOf(
            0x4B0112 to 14, 0x4B0112 to 21, 0x6C2033 to 29, 0x914656 to 38,
            0xBC8088 to 48, 0xAC9BBC to 58, 0x98AADA to 69, 0x89B1F6 to 80,
            0x5484D3 to 91, 0x315FAF to 100,
        )
        specification.forEachIndexed { index, (rgb, percent) ->
            val alpha = (percent * 255f / 100f).roundToInt()
            assertEquals("Rating ${index + 1}", (alpha shl 24) or rgb, ratingColor(index + 1).toArgb())
        }
    }

    @Test
    fun `digits remain readable on both light and dark cards despite rating opacity`() {
        listOf(LightColors.surface, DarkColors.surface).forEach { surface ->
            (1..10).forEach { rating ->
                val background = ratingColor(rating).compositeOver(surface).luminance()
                val foreground = ratingContentColor(rating, surface).luminance()
                val contrast = (max(background, foreground) + 0.05f) / (min(background, foreground) + 0.05f)
                assertTrue("Rating $rating: contrast $contrast", contrast >= 4.5f)
            }
        }
    }
}
