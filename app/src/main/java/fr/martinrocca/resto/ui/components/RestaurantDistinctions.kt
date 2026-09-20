package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.RestaurantCategory
import fr.martinrocca.resto.domain.model.categories
import fr.martinrocca.resto.domain.model.priceRange

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RestaurantDistinctions(restaurant: Restaurant) {
    if (restaurant.categories.isEmpty() && restaurant.michelinStatus == MichelinStatus.ABSENT && restaurant.priceRange == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RestaurantCategory.entries.filter { it in restaurant.categories }.forEach { category ->
            RestaurantTagBadge(category.label)
        }
        restaurant.priceRange?.let { RestaurantTagBadge(it.label) }
        if (restaurant.michelinStatus != MichelinStatus.ABSENT) MichelinBadge(restaurant.michelinStatus)
    }
}

@Composable
fun RestaurantTagBadge(label: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
