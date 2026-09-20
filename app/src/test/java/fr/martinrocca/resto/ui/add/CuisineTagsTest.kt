package fr.martinrocca.resto.ui.add

import org.junit.Assert.assertEquals
import org.junit.Test

class CuisineTagsTest {
    @Test
    fun `typed price changes keep only the most recent range while preserving cuisines and categories`() {
        assertEquals(
            listOf("Français", "gastro", "40€-80€"),
            canonicalizeTags("Français, <15€, gastro, 15 € - 40 €, 40€-80€", emptyList()),
        )
    }

    @Test
    fun `reserved categories keep their canonical names when editing old tags`() {
        val known = listOf("Qualité prix", "GASTRO", "Français")
        assertEquals(
            listOf("qualité-prix", "gastro", "Français"),
            canonicalizeTags("Qualité/prix,qualité-prix,GASTRO,gastro,Français", known),
        )
    }

    @Test
    fun `adding a suggestion preserves every selected tag and removes equivalent duplicates`() {
        val known = listOf("Italien", "Français", "Japonais")
        val first = canonicalizeTags("Italien,Français", known)
        val second = canonicalizeTags("${first.joinToString(",")},Japonais,italien", known)
        assertEquals(listOf("Italien", "Français", "Japonais"), second)
    }
}
