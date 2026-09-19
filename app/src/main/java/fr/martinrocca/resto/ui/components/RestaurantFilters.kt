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

@Composable
fun RestaurantFilters(
    michelin: MichelinStatus?,
    onMichelinChange: (MichelinStatus?) -> Unit,
    cuisines: List<CuisineFilter>,
    cuisineKey: String?,
    onCuisineChange: (String?) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(michelin == null, { onMichelinChange(null) }, { Text("Tous") })
            MichelinStatus.entries.filterNot { it == MichelinStatus.ABSENT }.forEach { status ->
                MichelinFilterChip(status, michelin == status) {
                    onMichelinChange(status.takeUnless { it == michelin })
                }
            }
        }
        if (cuisines.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(cuisineKey == null, { onCuisineChange(null) }, { Text("Toutes cuisines") })
                cuisines.forEach { cuisine ->
                    FilterChip(
                        selected = cuisineKey == cuisine.key,
                        onClick = { onCuisineChange(cuisine.key.takeUnless { it == cuisineKey }) },
                        label = { Text(cuisine.label) },
                    )
                }
            }
        }
    }
}
