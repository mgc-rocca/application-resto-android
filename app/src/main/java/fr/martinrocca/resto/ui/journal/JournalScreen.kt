package fr.martinrocca.resto.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.cuisineFilters
import fr.martinrocca.resto.domain.model.matchesFilters
import fr.martinrocca.resto.ui.components.EmptyState
import fr.martinrocca.resto.ui.components.RestaurantCard
import fr.martinrocca.resto.ui.components.RestaurantFilters

@Composable
fun JournalScreen(
    restaurants: List<Restaurant>,
    onRestaurantClick: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var michelinFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    var cuisineFilterKey by rememberSaveable { mutableStateOf<String?>(null) }
    val michelinFilter = michelinFilterName?.let(MichelinStatus::valueOf)
    val focusManager = LocalFocusManager.current
    val cuisineFilters = remember(restaurants) {
        cuisineFilters(restaurants.filter(Restaurant::isVisited))
    }

    val filteredRestaurants = remember(restaurants, query, michelinFilter, cuisineFilterKey) {
        restaurants
            .asSequence()
            .filter(Restaurant::isVisited)
            .filter { it.matchesFilters(michelinFilter, cuisineFilterKey) }
            .filter { restaurant ->
                query.isBlank() || restaurant.searchableText().contains(query.trim(), ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<Restaurant> { it.latestVisit?.date }
                    .thenBy { it.name.lowercase() },
            )
            .toList()
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
            Text("Journal", style = MaterialTheme.typography.displaySmall)
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Rechercher dans le journal") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
        }
        item {
            RestaurantFilters(
                michelin = michelinFilter,
                onMichelinChange = { michelinFilterName = it?.name },
                cuisines = cuisineFilters,
                cuisineKey = cuisineFilterKey,
                onCuisineChange = { cuisineFilterKey = it },
            )
        }

        if (filteredRestaurants.isEmpty()) {
            item {
                EmptyState(
                    title = if (restaurants.none(Restaurant::isVisited)) {
                        "Votre carnet est encore vierge"
                    } else {
                        "Aucun résultat"
                    },
                    message = if (restaurants.none(Restaurant::isVisited)) {
                        "Utilisez le bouton + pour enregistrer votre première visite."
                    } else {
                        "Essayez une autre recherche ou retirez les filtres actifs."
                    },
                )
            }
        } else {
            items(
                items = filteredRestaurants,
                key = Restaurant::id,
            ) { restaurant ->
                RestaurantCard(
                    restaurant = restaurant,
                    onClick = { onRestaurantClick(restaurant.id) },
                    showAddress = false,
                )
            }
        }
    }
}

private fun Restaurant.searchableText(): String = buildString {
    append(name)
    append(' ')
    append(address)
    tags.forEach { append(' ').append(it.name) }
    visits.forEach { visit ->
        visit.comment?.let { append(' ').append(it) }
    }
}
