package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus

@Composable
fun RatingPicker(
    selected: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Note", style = MaterialTheme.typography.titleMedium)
        listOf(1..5, 6..10).forEach { ratings ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ratings.forEach { rating ->
                    val isSelected = selected == rating
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) ratingColor(rating)
                                else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) Color.Transparent
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable { onSelected(rating) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = rating.toString(),
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        selected?.let {
            Text(
                text = "$it/10 · ${ratingLabel(it)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ratingLabel(rating: Int): String = when (rating) {
    in 1..3 -> "Décevant"
    in 4..5 -> "Correct"
    in 6..7 -> "Bon"
    in 8..9 -> "Très bon"
    else -> "Exceptionnel"
}

@Composable
fun MichelinPicker(
    selected: MichelinStatus,
    onSelected: (MichelinStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "Guide Michelin", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MichelinStatus.entries.forEach { status ->
                if (status == MichelinStatus.ABSENT) {
                    FilterChip(selected == status, { onSelected(status) }, { Text("Absent") })
                } else {
                    MichelinFilterChip(status, selected == status) { onSelected(status) }
                }
            }
        }
    }
}
