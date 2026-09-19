package fr.martinrocca.resto.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.RestaurantCategory
import fr.martinrocca.resto.domain.model.matchesFilters

@Stable
class RestaurantFilterState {
    var michelin by mutableStateOf<MichelinStatus?>(null)
    var cuisineKey by mutableStateOf<String?>(null)
    var categories by mutableStateOf(emptySet<RestaurantCategory>())
    var minimumRating by mutableStateOf<Int?>(null)

    val activeCount: Int
        get() = listOfNotNull(michelin, cuisineKey, minimumRating).size + categories.size

    fun toggleCategory(category: RestaurantCategory) {
        categories = if (category in categories) categories - category else categories + category
    }

    fun reset(michelin: MichelinStatus? = null) {
        this.michelin = michelin
        cuisineKey = null
        categories = emptySet()
        minimumRating = null
    }

    fun matches(restaurant: Restaurant): Boolean = restaurant.matchesFilters(
        michelin, cuisineKey, categories, minimumRating,
    )

    companion object {
        val Saver = listSaver<RestaurantFilterState, String>(
            save = {
                listOf(
                    it.michelin?.name.orEmpty(), it.cuisineKey.orEmpty(),
                    it.categories.joinToString(",") { category -> category.name },
                    it.minimumRating?.toString().orEmpty(),
                )
            },
            restore = { values ->
                RestaurantFilterState().apply {
                    michelin = values[0].takeIf(String::isNotEmpty)?.let(MichelinStatus::valueOf)
                    cuisineKey = values[1].takeIf(String::isNotEmpty)
                    categories = values[2].split(',').filter(String::isNotEmpty)
                        .map(RestaurantCategory::valueOf).toSet()
                    minimumRating = values[3].toIntOrNull()
                }
            },
        )
    }
}

@Composable
fun rememberRestaurantFilterState(): RestaurantFilterState =
    rememberSaveable(saver = RestaurantFilterState.Saver) { RestaurantFilterState() }
