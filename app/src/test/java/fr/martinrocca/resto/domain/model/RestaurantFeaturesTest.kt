package fr.martinrocca.resto.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantFeaturesTest {
    @Test
    fun `price filters combine with the other filters and prices are never cuisines`() {
        val restaurant = restaurant(MichelinStatus.ONE_STAR).copy(
            tags = listOf(Tag("t1", "Italien"), Tag("t2", "gastro"), Tag("t3", "15 € – 40 €")),
        )
        assertEquals(PriceRange.FROM_15_TO_40, restaurant.priceRange)
        assertEquals(listOf("Italien"), cuisineFilters(listOf(restaurant)).map { it.label })
        assertTrue(restaurant.matchesFilters(
            MichelinStatus.ONE_STAR, tagEquivalenceKey("Italien"), setOf(RestaurantCategory.GASTRO),
            minimumRating = 8, priceRange = PriceRange.FROM_15_TO_40,
        ))
        assertFalse(restaurant.matchesFilters(null, null, priceRange = PriceRange.OVER_80))
        assertFalse(restaurant.matchesFilters(null, null, minimumRating = 9, priceRange = PriceRange.FROM_15_TO_40))
        assertFalse(restaurant(MichelinStatus.ABSENT).matchesFilters(null, null, priceRange = PriceRange.UNDER_15))
        assertTrue(restaurant.shareText().contains("Prix : 15€ - 40€"))
    }

    @Test
    fun `replacing or clearing a price preserves all other tags`() {
        val tags = listOf("Français", "gastro", "<15€", "qualité-prix")
        val changed = withPriceRange(tags, PriceRange.OVER_80)
        assertEquals(listOf("Français", "gastro", "qualité-prix", ">80€"), changed)
        assertEquals(listOf("Français", "gastro", "qualité-prix"), withPriceRange(changed, null))
    }

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
    fun `rating filters use the average and exclude unrated wishes only when active`() {
        val restaurant = restaurant(MichelinStatus.ABSENT)
        val latest = restaurant.visits.single().copy(id = "new", date = LocalDate.of(2026, 9, 20), overallRating = 3)
        val revisited = restaurant.copy(visits = listOf(latest) + restaurant.visits)
        assertEquals(5.5, revisited.averageRating!!, 0.0)
        assertFalse(revisited.matchesFilters(null, null, minimumRating = 8))
        assertTrue(revisited.matchesFilters(null, null, minimumRating = 5))
        assertFalse(revisited.matchesFilters(null, null, minimumRating = 6))
        val wish = restaurant.copy(visits = emptyList())
        assertNull(wish.averageRating)
        assertNull(wish.ratingLevel)
        assertTrue(wish.matchesFilters(null, null))
        assertFalse(wish.matchesFilters(null, null, minimumRating = 1))
    }

    @Test
    fun `restaurant scores round to one decimal and recalculate when visits change`() {
        val restaurant = restaurant(MichelinStatus.ABSENT)
        val visit = restaurant.visits.single()
        val revisited = restaurant.copy(visits = listOf(
            visit, visit.copy(id = "v2"), visit.copy(id = "v3"), visit.copy(id = "v4", overallRating = 9),
        ))
        assertEquals(8.0, restaurant.averageRating!!, 0.0)
        assertEquals(8.3, revisited.averageRating!!, 0.0)
        assertEquals(8, revisited.ratingLevel)
        val corrected = revisited.copy(visits = revisited.visits.dropLast(1) + visit.copy(id = "v4", overallRating = 10))
        assertEquals(8.5, corrected.averageRating!!, 0.0)
        assertEquals(9, corrected.ratingLevel)
        assertEquals(8.0, corrected.copy(visits = corrected.visits.dropLast(1)).averageRating!!, 0.0)
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
    fun `sharing contains key facts and exact map position without personal ratings or comments`() {
        val text = restaurant(MichelinStatus.TWO_STARS).shareText()
        assertTrue(text.contains("Café & Table"))
        assertTrue(text.contains("1 rue du Test, Paris"))
        assertTrue(text.contains("Français · Italien"))
        assertTrue(text.contains("2 étoiles Michelin"))
        assertFalse(text.contains("Ma note"))
        assertFalse(text.contains("8/10"))
        assertTrue(text.endsWith("https://www.google.com/maps/dir/?api=1&destination=48.85%2C2.35"))
        assertTrue(text.contains("1 rue du Test, Paris\n\nCuisine :"))
        assertFalse(text.contains("openstreetmap"))
        assertFalse(text.contains("Commentaire privé"))
        assertFalse(text.contains("Envie privée"))
    }

    @Test
    fun `sharing a manual address uses an encoded directions destination and omits rating`() {
        val text = restaurant(MichelinStatus.ABSENT)
            .copy(latitude = null, longitude = null, visits = emptyList()).shareText()
        assertTrue(text.endsWith("https://www.google.com/maps/dir/?api=1&destination=Caf%C3%A9+%26+Table+1+rue+du+Test%2C+Paris"))
        assertFalse(text.contains("Ma note"))
        assertFalse(text.contains("Michelin"))
    }

    @Test
    fun `sharing removes a repeated name before a multiline or Geoapify address`() {
        val restaurant = restaurant(MichelinStatus.ABSENT).copy(tags = listOf(Tag("p", "<15€")))
        listOf(
            "Café & Table\n1 rue du Test, Paris",
            "Café & Table, 1 rue du Test, Paris",
            "CAFÉ\u00a0 &   TABLE\r\n1 rue du Test, Paris",
        ).forEach { address ->
            val text = restaurant.copy(address = address).shareText()
            assertTrue(text.startsWith("$address\n\nPrix : <15€\n\nhttps://www.google.com/maps/dir/"))
        }
    }

    @Test
    fun `sharing keeps distinct names and separates cuisine and price from the address`() {
        val text = restaurant(MichelinStatus.ABSENT).copy(
            address = "Café & Tableau, 1 rue du Test, Paris",
            tags = listOf(Tag("c", "Français"), Tag("p", "40€-80€")),
        ).shareText()
        assertTrue(text.startsWith("Café & Table\nCafé & Tableau, 1 rue du Test, Paris\n\nCuisine : Français\nPrix : 40€-80€\n\n"))
    }

    @Test
    fun `sharing without tags keeps one blank line before directions and preserves zero or negative coordinates`() {
        val restaurant = restaurant(MichelinStatus.ABSENT).copy(tags = emptyList())
        assertTrue(restaurant.copy(latitude = 0.0, longitude = -1.25).shareText().endsWith(
            "1 rue du Test, Paris\n\nhttps://www.google.com/maps/dir/?api=1&destination=0.0%2C-1.25",
        ))
        assertTrue(restaurant.copy(latitude = null).shareText().contains("destination=Caf%C3%A9+%26+Table"))
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
