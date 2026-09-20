package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.RestaurantCategory
import fr.martinrocca.resto.domain.model.categories
import fr.martinrocca.resto.domain.model.cuisineTags
import fr.martinrocca.resto.domain.model.priceRange

@Composable
fun JournalRestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.titleLarge
    val tagStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    // Reserve the same two rows for every restaurant, including those without tags.
    // Scale their height with the user's font size rather than clipping large text.
    val titleHeight = with(density) { titleStyle.lineHeight.toDp() }
    val tagHeight = with(density) { tagStyle.lineHeight.toDp() }.coerceAtLeast(20.dp) + 12.dp
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RatingBadge(restaurant.latestVisit?.overallRating)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    restaurant.name,
                    modifier = Modifier.fillMaxWidth().height(titleHeight),
                    style = titleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().height(tagHeight).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (restaurant.michelinStatus != MichelinStatus.ABSENT) MichelinBadge(restaurant.michelinStatus)
                    restaurant.cuisineTags.forEach { RestaurantTagBadge(it.name) }
                    RestaurantCategory.entries.filter { it in restaurant.categories }.forEach {
                        RestaurantTagBadge(it.label)
                    }
                    restaurant.priceRange?.let { RestaurantTagBadge(it.label) }
                }
            }
        }
    }
}
