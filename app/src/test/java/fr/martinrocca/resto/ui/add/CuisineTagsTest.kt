package fr.martinrocca.resto.ui.add

import org.junit.Assert.assertEquals
import org.junit.Test

class CuisineTagsTest {
    @Test
    fun `adding a suggestion preserves every selected tag and removes equivalent duplicates`() {
        val known = listOf("Italien", "Français", "Japonais")
        val first = canonicalizeTags("Italien,Français", known)
        val second = canonicalizeTags("${first.joinToString(",")},Japonais,italien", known)
        assertEquals(listOf("Italien", "Français", "Japonais"), second)
    }
}
