package fr.martinrocca.resto.data.repository

/** Keeps every stored restaurant reachable from either the journal or the wishlist. */
internal object RestaurantVisibilityPolicy {
    fun deleteAfterWishlistRemoval(visitCount: Int): Boolean {
        require(visitCount >= 0)
        return visitCount == 0
    }

    fun addWishlistAfterLastVisitRemoval(
        remainingVisitCount: Int,
        hasWishlistEntry: Boolean,
    ): Boolean {
        require(remainingVisitCount >= 0)
        return remainingVisitCount == 0 && !hasWishlistEntry
    }
}
