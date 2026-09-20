package fr.martinrocca.resto.ui.components

import androidx.compose.runtime.saveable.SaverScope
import fr.martinrocca.resto.domain.model.PriceRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RestaurantFilterStateTest {
    private val saverScope = object : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }

    private fun restored(state: RestaurantFilterState): RestaurantFilterState {
        val saved = with(RestaurantFilterState.Saver) { saverScope.save(state) }
        return requireNotNull(RestaurantFilterState.Saver.restore(requireNotNull(saved)))
    }

    @Test
    fun `map starts with a visible minimum rating while other screens stay unfiltered`() {
        val map = RestaurantFilterState(initialMinimumRating = 5)
        assertEquals(5, map.minimumRating)
        assertEquals(1, map.activeCount)
        assertEquals(0, RestaurantFilterState().activeCount)
    }

    @Test
    fun `clearing the default survives state restoration without resetting other filters`() {
        val map = RestaurantFilterState(initialMinimumRating = 5).apply {
            minimumRating = null
            priceRange = PriceRange.FROM_15_TO_40
        }
        val restored = restored(map)
        assertNull(restored.minimumRating)
        assertEquals(PriceRange.FROM_15_TO_40, restored.priceRange)
        assertEquals(1, restored.activeCount)
    }

    @Test
    fun `modified rating and reset both survive state restoration`() {
        val map = RestaurantFilterState(initialMinimumRating = 5).apply { minimumRating = 8 }
        assertEquals(8, restored(map).minimumRating)
        map.reset()
        assertNull(restored(map).minimumRating)
        assertEquals(0, restored(map).activeCount)
    }
}
