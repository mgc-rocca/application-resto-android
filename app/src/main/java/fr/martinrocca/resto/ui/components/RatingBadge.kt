package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.theme.RatingPalette
import fr.martinrocca.resto.theme.DisabledElement
import java.util.Locale

@Composable
fun RestaurantRatingBadge(restaurant: Restaurant, modifier: Modifier = Modifier) {
    val average = restaurant.averageRating
    RatingBadge(
        rating = restaurant.ratingLevel,
        modifier = modifier,
        label = when {
            average == null -> "–"
            restaurant.visits.size > 1 -> String.format(Locale.FRANCE, "%.1f", average)
            else -> average.toInt().toString()
        },
    )
}

@Composable
fun RatingBadge(
    rating: Int?,
    modifier: Modifier = Modifier,
    label: String = rating?.toString() ?: "–",
) {
    val color = ratingColor(rating)
    Box(
        modifier = modifier
            .size(48.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = ratingContentColor(rating, MaterialTheme.colorScheme.surface),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

fun ratingColor(rating: Int?): Color = RatingPalette.getOrNull((rating ?: 0) - 1) ?: DisabledElement

internal fun ratingContentColor(rating: Int?, background: Color): Color {
    val luminance = ratingColor(rating).compositeOver(background).luminance()
    val whiteContrast = 1.05f / (luminance + 0.05f)
    val blackContrast = (luminance + 0.05f) / 0.05f
    return if (whiteContrast >= blackContrast) Color.White else Color.Black
}
