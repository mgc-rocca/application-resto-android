package fr.martinrocca.resto.domain.model

import java.time.LocalDate
import kotlin.math.roundToInt

data class Restaurant(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val geoapifyPlaceId: String?,
    val michelinStatus: MichelinStatus,
    val tags: List<Tag>,
    val visits: List<Visit>,
    val wishlist: WishlistEntry?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val latestVisit: Visit?
        get() = visits.maxWithOrNull(
            compareBy<Visit> { it.date }
                .thenBy { it.createdAt },
        )

    val isVisited: Boolean
        get() = visits.isNotEmpty()

    /** The restaurant score, recalculated from every visit and rounded to one decimal. */
    val averageRating: Double?
        get() = if (visits.isEmpty()) null else
            (visits.map { it.overallRating }.average() * 10).roundToInt() / 10.0

    /** Keep the ten validated palette levels, using the nearest integer to the displayed score. */
    val ratingLevel: Int?
        get() = averageRating?.roundToInt()?.coerceIn(1, 10)
}

data class Tag(
    val id: String,
    val name: String,
)

data class Visit(
    val id: String,
    val restaurantId: String,
    val date: LocalDate,
    val overallRating: Int,
    val foodRating: Int?,
    val serviceRating: Int?,
    val settingRating: Int?,
    val priceRating: Int?,
    val comment: String?,
    val dishes: List<Dish>,
    val photos: List<Photo>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Dish(
    val id: String,
    val name: String,
    val priceCents: Long?,
    val currency: String,
    val sortOrder: Int,
)

data class Photo(
    val id: String,
    val relativePath: String,
    val mimeType: String?,
    val sortOrder: Int,
)

data class WishlistEntry(
    val addedAt: Long,
    val note: String?,
)

data class RestaurantDraft(
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geoapifyPlaceId: String? = null,
    val michelinStatus: MichelinStatus = MichelinStatus.ABSENT,
    val tags: List<String> = emptyList(),
)

data class VisitDraft(
    val date: LocalDate,
    val overallRating: Int,
    val comment: String? = null,
    val photoUris: List<String> = emptyList(),
)
