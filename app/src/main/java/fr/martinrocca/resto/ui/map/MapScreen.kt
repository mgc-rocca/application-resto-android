package fr.martinrocca.resto.ui.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.GeoapifySearchField

private enum class MapFilter(val label: String) {
    ALL("Tous"),
    VISITED("Visités"),
    WISHLIST("Envies"),
}

@Composable
fun MapScreen(
    restaurants: List<Restaurant>,
    viewModel: RestoViewModel,
    onRestaurantClick: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var addressQuery by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(MapFilter.ALL.name) }
    var cameraTarget by remember { mutableStateOf<MapTarget?>(null) }
    val filter = MapFilter.valueOf(filterName)
    val mappedRestaurants = remember(restaurants, filter) {
        restaurants.filter { restaurant ->
            restaurant.latitude != null && restaurant.longitude != null && when (filter) {
                MapFilter.ALL -> restaurant.isVisited || restaurant.wishlist != null
                MapFilter.VISITED -> restaurant.isVisited
                MapFilter.WISHLIST -> restaurant.wishlist != null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Carte", style = MaterialTheme.typography.displaySmall)
            GeoapifySearchField(
                query = addressQuery,
                onQueryChange = { addressQuery = it },
                isConfigured = viewModel.isGeoapifyConfigured,
                search = viewModel::searchPlaces,
                onSuggestionSelected = { suggestion ->
                    addressQuery = suggestion.formattedAddress
                    cameraTarget = MapTarget(
                        latitude = suggestion.latitude,
                        longitude = suggestion.longitude,
                        zoom = 15.0,
                    )
                },
                label = "Adresse",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MapFilter.entries.forEach { option ->
                    FilterChip(
                        selected = option == filter,
                        onClick = { filterName = option.name },
                        label = { Text(option.label) },
                    )
                }
            }
            Text(
                text = "${mappedRestaurants.size} adresses affichées",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RestaurantMap(
            restaurants = mappedRestaurants,
            cameraTarget = cameraTarget,
            onRestaurantClick = onRestaurantClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .weight(1f),
        )
    }
}

data class MapTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
)
