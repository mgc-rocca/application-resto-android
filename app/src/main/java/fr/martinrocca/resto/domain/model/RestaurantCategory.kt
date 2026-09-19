package fr.martinrocca.resto.domain.model

/** Personal categories stored as ordinary tags, so existing journals and backups stay compatible. */
enum class RestaurantCategory(val label: String) {
    QUALITY_PRICE("qualité-prix"),
    GASTRO("gastro"),
}

fun restaurantCategory(name: String): RestaurantCategory? = when (
    normalizeTagName(name).replace("[\\s/\\-–—‑]+".toRegex(), "")
) {
    "qualiteprix" -> RestaurantCategory.QUALITY_PRICE
    "gastro" -> RestaurantCategory.GASTRO
    else -> null
}

fun isCuisineTag(name: String): Boolean = restaurantCategory(name) == null

val Restaurant.cuisineTags: List<Tag>
    get() = tags.filter { isCuisineTag(it.name) }

val Restaurant.categories: Set<RestaurantCategory>
    get() = tags.mapNotNull { restaurantCategory(it.name) }.toSet()
