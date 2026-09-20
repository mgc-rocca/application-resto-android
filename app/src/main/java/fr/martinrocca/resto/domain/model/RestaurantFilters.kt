package fr.martinrocca.resto.domain.model

data class CuisineFilter(val key: String, val label: String)

fun cuisineFilters(restaurants: List<Restaurant>): List<CuisineFilter> = restaurants
    .flatMap { it.cuisineTags.map(Tag::name) }
    .groupBy(::tagEquivalenceKey)
    .map { (key, names) ->
        CuisineFilter(key, names.groupingBy { it }.eachCount().maxBy { it.value }.key)
    }
    .sortedBy { it.label.lowercase() }

fun Restaurant.matchesFilters(
    michelin: MichelinStatus?,
    cuisineKey: String?,
    categories: Set<RestaurantCategory> = emptySet(),
    minimumRating: Int? = null,
    priceRange: PriceRange? = null,
): Boolean =
    (michelin == null || michelinStatus == michelin) &&
        (cuisineKey == null || cuisineTags.any { tagEquivalenceKey(it.name) == cuisineKey }) &&
        this.categories.containsAll(categories) &&
        (minimumRating == null || latestVisit?.overallRating?.let { it >= minimumRating } == true) &&
        (priceRange == null || this.priceRange == priceRange)

fun michelinVisitCounts(restaurants: List<Restaurant>): Map<MichelinStatus, Int> =
    MichelinStatus.entries.filterNot { it == MichelinStatus.ABSENT }.associateWith { status ->
        restaurants.count { it.isVisited && it.michelinStatus == status }
    }
