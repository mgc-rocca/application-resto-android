package fr.martinrocca.resto.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingBandTest {
    @Test
    fun `ratings follow the five validated marker levels`() {
        assertEquals(RatingBand.VERY_LOW, ratingBand(1))
        assertEquals(RatingBand.VERY_LOW, ratingBand(3))
        assertEquals(RatingBand.LOW, ratingBand(4))
        assertEquals(RatingBand.GOOD, ratingBand(7))
        assertEquals(RatingBand.VERY_GOOD, ratingBand(9))
        assertEquals(RatingBand.EXCEPTIONAL, ratingBand(10))
        assertEquals(RatingBand.WISHLIST, ratingBand(null))
    }
}
