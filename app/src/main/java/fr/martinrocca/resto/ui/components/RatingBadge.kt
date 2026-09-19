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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.RatingBand
import fr.martinrocca.resto.domain.model.ratingBand
import fr.martinrocca.resto.theme.RatingExceptional
import fr.martinrocca.resto.theme.RatingGood
import fr.martinrocca.resto.theme.RatingLow
import fr.martinrocca.resto.theme.RatingMedium
import fr.martinrocca.resto.theme.RatingVeryGood
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
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

fun ratingColor(rating: Int?): Color = when (ratingBand(rating)) {
    RatingBand.VERY_LOW -> RatingLow
    RatingBand.LOW -> RatingMedium
    RatingBand.GOOD -> RatingGood
    RatingBand.VERY_GOOD -> RatingVeryGood
    RatingBand.EXCEPTIONAL -> RatingExceptional
    RatingBand.WISHLIST -> WarmGray
}
