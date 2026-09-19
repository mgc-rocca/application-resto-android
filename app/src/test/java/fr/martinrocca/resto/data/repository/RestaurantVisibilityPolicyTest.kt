package fr.martinrocca.resto.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantVisibilityPolicyTest {
    @Test
    fun `removing an unvisited wishlist restaurant deletes the orphan`() {
        assertTrue(RestaurantVisibilityPolicy.deleteAfterWishlistRemoval(visitCount = 0))
        assertFalse(RestaurantVisibilityPolicy.deleteAfterWishlistRemoval(visitCount = 1))
    }

    @Test
    fun `deleting the last visit restores the restaurant to the wishlist`() {
        assertTrue(
            RestaurantVisibilityPolicy.addWishlistAfterLastVisitRemoval(
                remainingVisitCount = 0,
                hasWishlistEntry = false,
            ),
        )
        assertFalse(
            RestaurantVisibilityPolicy.addWishlistAfterLastVisitRemoval(
                remainingVisitCount = 1,
                hasWishlistEntry = false,
            ),
        )
        assertFalse(
            RestaurantVisibilityPolicy.addWishlistAfterLastVisitRemoval(
                remainingVisitCount = 0,
                hasWishlistEntry = true,
            ),
        )
    }
}
