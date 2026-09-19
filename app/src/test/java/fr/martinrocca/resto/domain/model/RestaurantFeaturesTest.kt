package fr.martinrocca.resto.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantFeaturesTest {
    @Test
    fun `legacy quality price and gastro tags are categories and never cuisines`() {
        val restaurant = restaurant(MichelinStatus.ABSENT).copy(
            tags = listOf(Tag("t1", "Italien"), Tag("t2", "Qualité / prix"), Tag("t3", "GASTRO")),
        )
        assertEquals(listOf("Italien"), cuisineFilters(listOf(restaurant)).map { it.label })
        assertEquals(RestaurantCategory.entries.toSet(), restaurant.categories)
        assertTrue(restaurant.matchesFilters(null, null, setOf(RestaurantCategory.QUALITY_PRICE)))
        assertFalse(restaurant.matchesFilters(null, tagEquivalenceKey("gastro")))
        assertTrue(restaurant.shareText().contains("Cuisine : Italien\n"))
        assertTrue(restaurant.shareText().contains("Tags : qualité-prix · gastro"))
    }

    @Test
    fun `categories cuisine Michelin and minimum rating can all be combined`() {
        val restaurant = restaurant(MichelinStatus.ONE_STAR).copy(
            tags = listOf(Tag("t1", "Italien"), Tag("t2", "gastro")),
        )
        val categories = setOf(RestaurantCategory.GASTRO)
        assertTrue(restaurant.matchesFilters(MichelinStatus.ONE_STAR, tagEquivalenceKey("italien"), categories, 8))
        assertFalse(restaurant.matchesFilters(MichelinStatus.TWO_STARS, null, categories, 8))
        assertFalse(restaurant.matchesFilters(null, null, RestaurantCategory.entries.toSet(), 8))
        assertFalse(restaurant.matchesFilters(null, null, categories, 9))
    }

    @Test
    fun `rating filters use the latest visit and exclude unrated wishes only when active`() {
        val restaurant = restaurant(MichelinStatus.ABSENT)
        val latest = restaurant.visits.single().copy(id = "new", date = LocalDate.of(2026, 9, 20), overallRating = 3)
        val revisited = restaurant.copy(visits = listOf(latest) + restaurant.visits)
        assertFalse(revisited.matchesFilters(null, null, minimumRating = 8))
        assertTrue(revisited.matchesFilters(null, null, minimumRating = 3))
        val wish = restaurant.copy(visits = emptyList())
        assertTrue(wish.matchesFilters(null, null))
        assertFalse(wish.matchesFilters(null, null, minimumRating = 1))
    }

    @Test
    fun `Michelin and cuisine filters are combined and any restaurant tag can match`() {
        val restaurant = restaurant(MichelinStatus.TWO_STARS)
        assertTrue(restaurant.matchesFilters(MichelinStatus.TWO_STARS, tagEquivalenceKey("italien")))
        assertFalse(restaurant.matchesFilters(MichelinStatus.PRESENT, tagEquivalenceKey("italien")))
        assertFalse(restaurant.matchesFilters(MichelinStatus.TWO_STARS, tagEquivalenceKey("japonais")))
        assertTrue(restaurant.matchesFilters(null, null))
    }

    @Test
    fun `Michelin stats use exclusive categories and exclude unvisited wishes`() {
        val restaurants = listOf(
            restaurant(MichelinStatus.PRESENT),
            restaurant(MichelinStatus.ONE_STAR),
            restaurant(MichelinStatus.TWO_STARS),
            restaurant(MichelinStatus.THREE_STARS),
            restaurant(MichelinStatus.ABSENT),
            restaurant(MichelinStatus.THREE_STARS).copy(visits = emptyList()),
        )
        val counts = michelinVisitCounts(restaurants)
        assertEquals(4, counts.size)
        assertEquals(listOf(1, 1, 1, 1), counts.values.toList())
    }

    @Test
    fun `sharing contains key facts and exact map position without private comments`() {
        val text = restaurant(MichelinStatus.TWO_STARS).shareText()
        assertTrue(text.contains("Café & Table"))
        assertTrue(text.contains("1 rue du Test, Paris"))
        assertTrue(text.contains("Français · Italien"))
        assertTrue(text.contains("2 étoiles Michelin"))
        assertTrue(text.contains("Ma note : 8/10"))
        assertTrue(text.contains("?mlat=48.85&mlon=2.35"))
        assertFalse(text.contains("Commentaire privé"))
        assertFalse(text.contains("Envie privée"))
    }

    @Test
    fun `sharing an unvisited manual address encodes the search and omits rating`() {
        val text = restaurant(MichelinStatus.ABSENT)
            .copy(latitude = null, longitude = null, visits = emptyList()).shareText()
        assertTrue(text.contains("Caf%C3%A9+%26+Table"))
        assertFalse(text.contains("Ma note"))
        assertFalse(text.contains("Michelin"))
    }

    private fun restaurant(status: MichelinStatus) = Restaurant(
        id = "r", name = "Café & Table", address = "1 rue du Test, Paris",
        latitude = 48.85, longitude = 2.35, geoapifyPlaceId = null,
        michelinStatus = status,
        tags = listOf(Tag("t1", "Français"), Tag("t2", "Italien")),
        wishlist = WishlistEntry(0, "Envie privée"),
        visits = listOf(
            Visit(
                id = "v", restaurantId = "r", date = LocalDate.of(2026, 9, 19),
                overallRating = 8, foodRating = null, serviceRating = null,
                settingRating = null, priceRating = null, comment = "Commentaire privé",
                dishes = emptyList(), photos = emptyList(), createdAt = 0, updatedAt = 0,
            ),
        ),
        createdAt = 0, updatedAt = 0,
    )
}
