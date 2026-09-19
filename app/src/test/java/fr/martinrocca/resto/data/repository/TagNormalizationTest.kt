package fr.martinrocca.resto.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TagNormalizationTest {
    @Test
    fun `normalization removes case accents and duplicate spaces`() {
        assertEquals("cuisine du marche", normalizeTagName("  Cuisine   du Marché "))
    }

    @Test
    fun `existing tag wins over case accent and gender variants`() {
        val existing = listOf("Française")

        assertEquals("Française", canonicalTagName("française", existing))
        assertEquals("Française", canonicalTagName("Francais", existing))
    }

    @Test
    fun `different cuisine names are not merged`() {
        assertEquals("Javanais", canonicalTagName("Javanais", listOf("Japonais")))
    }
}
