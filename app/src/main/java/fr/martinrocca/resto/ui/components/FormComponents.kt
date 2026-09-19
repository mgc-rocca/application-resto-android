package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import kotlin.math.roundToInt

@Composable
fun RatingPicker(
    selected: Int?,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastInteractionRating by remember(selected) { mutableIntStateOf(selected ?: 5) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Note", style = MaterialTheme.typography.titleMedium)
            Text(selected?.let { "$it/10" } ?: "—/10", style = MaterialTheme.typography.titleLarge)
        }
        Slider(
            value = (selected ?: 5).toFloat(),
            onValueChange = {
                lastInteractionRating = it.roundToInt().coerceIn(1, 10)
                onSelected(lastInteractionRating)
            },
            // A tap on the initial thumb also explicitly selects 5, without silently assigning a note.
            onValueChangeFinished = { if (selected == null) onSelected(lastInteractionRating) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = ratingColor(selected),
                activeTrackColor = ratingColor(selected),
            ),
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = "Note sur 10"
                stateDescription = selected?.let { "$it sur 10" } ?: "Non renseignée"
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1", style = MaterialTheme.typography.labelMedium)
            Text("10", style = MaterialTheme.typography.labelMedium)
        }
        selected?.let {
            Text(
                text = ratingLabel(it),
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
