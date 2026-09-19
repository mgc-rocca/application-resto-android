package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.CuisineFilter
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.RestaurantCategory

@Composable
fun RestaurantFilters(
    state: RestaurantFilterState,
    cuisines: List<CuisineFilter>,
    showRating: Boolean = false,
) {
    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.michelin == null && state.categories.isEmpty(),
                onClick = { state.michelin = null; state.categories = emptySet() },
                label = { Text("Tous") },
            )
            MichelinStatus.entries.filterNot { it == MichelinStatus.ABSENT }.forEach { status ->
                MichelinFilterChip(status, state.michelin == status) {
                    state.michelin = status.takeUnless { it == state.michelin }
                }
            }
            RestaurantCategory.entries.forEach { category ->
                FilterChip(
                    selected = category in state.categories,
                    onClick = { state.toggleCategory(category) },
                    label = { Text(category.label) },
                )
            }
        }
        if (cuisines.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(state.cuisineKey == null, { state.cuisineKey = null }, { Text("Toutes cuisines") })
                cuisines.forEach { cuisine ->
                    FilterChip(
                        selected = state.cuisineKey == cuisine.key,
                        onClick = { state.cuisineKey = cuisine.key.takeUnless { it == state.cuisineKey } },
                        label = { Text(cuisine.label) },
                    )
                }
            }
        }
        if (showRating) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(state.minimumRating == null, { state.minimumRating = null }, { Text("Toutes les notes") })
                (1..10).forEach { rating ->
                    FilterChip(
                        selected = state.minimumRating == rating,
                        onClick = { state.minimumRating = rating.takeUnless { it == state.minimumRating } },
                        label = { Text("≥ $rating/10") },
                    )
                }
            }
        }
    }
}
