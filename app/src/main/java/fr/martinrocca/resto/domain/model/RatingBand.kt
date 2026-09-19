package fr.martinrocca.resto.domain.model

enum class RatingBand {
    VERY_LOW,
    LOW,
    GOOD,
    VERY_GOOD,
    EXCEPTIONAL,
    WISHLIST,
}

fun ratingBand(rating: Int?): RatingBand = when (rating) {
    null -> RatingBand.WISHLIST
    in 1..3 -> RatingBand.VERY_LOW
    in 4..5 -> RatingBand.LOW
    in 6..7 -> RatingBand.GOOD
    in 8..9 -> RatingBand.VERY_GOOD
    else -> RatingBand.EXCEPTIONAL
}
