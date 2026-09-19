package fr.martinrocca.resto.ui.wishlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.cuisineFilters
import fr.martinrocca.resto.ui.components.EmptyState
import fr.martinrocca.resto.ui.components.RestaurantCard
import fr.martinrocca.resto.ui.components.RestaurantFilters
import fr.martinrocca.resto.ui.components.rememberRestaurantFilterState

@Composable
fun WishlistScreen(
    restaurants: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    onAddVisit: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filters = rememberRestaurantFilterState()
    val cuisines = remember(restaurants) { cuisineFilters(restaurants.filter { it.wishlist != null }) }
    val wishlist = remember(restaurants, query, filters.michelin, filters.cuisineKey, filters.categories) {
        restaurants
            .filter { it.wishlist != null }
            .filter(filters::matches)
            .filter {
                query.isBlank() || listOf(
                    it.name,
                    it.address,
                    it.tags.joinToString(" ") { tag -> tag.name },
                ).joinToString(" ").contains(query.trim(), ignoreCase = true)
            }
            .sortedByDescending { it.wishlist?.addedAt }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Envies", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "${restaurants.count { it.wishlist != null }} adresses à découvrir",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rechercher dans les envies") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
            )
        }

        item { RestaurantFilters(state = filters, cuisines = cuisines) }

        if (wishlist.isEmpty()) {
            item {
                EmptyState(
                    title = if (restaurants.none { it.wishlist != null }) {
                        "Aucune envie enregistrée"
                    } else {
                        "Aucun résultat"
                    },
                    message = if (restaurants.none { it.wishlist != null }) {
                        "Ajoutez ici les restaurants que vous souhaitez essayer plus tard."
                    } else "Essayez une autre recherche ou retirez les filtres actifs.",
                )
            }
        } else {
            items(wishlist, key = Restaurant::id) { restaurant ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RestaurantCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.id) },
                    )
                    Button(
                        onClick = { onAddVisit(restaurant.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("J’y suis allé")
                    }
                }
            }
        }
    }
}
