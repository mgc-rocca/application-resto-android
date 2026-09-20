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
import fr.martinrocca.resto.theme.RatingPalette
import fr.martinrocca.resto.theme.WarmGray

@Composable
fun RatingBadge(
    rating: Int?,
    modifier: Modifier = Modifier,
) {
    val color = ratingColor(rating)
    Box(
        modifier = modifier
            .size(48.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rating?.toString() ?: "–",
            color = ratingContentColor(rating, MaterialTheme.colorScheme.surface),
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

fun ratingColor(rating: Int?): Color = RatingPalette.getOrNull((rating ?: 0) - 1) ?: WarmGray

internal fun ratingContentColor(rating: Int?, background: Color): Color {
    val luminance = ratingColor(rating).compositeOver(background).luminance()
    val whiteContrast = 1.05f / (luminance + 0.05f)
    val blackContrast = (luminance + 0.05f) / 0.05f
    return if (whiteContrast >= blackContrast) Color.White else Color.Black
}
