package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.CuisineFilter

@Composable
fun RestaurantFiltersButton(activeCount: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Icon(Icons.Outlined.FilterList, null)
        Text(
            if (activeCount == 0) "Filtres" else "Filtres ($activeCount)",
            Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RestaurantFilterSheet(
    state: RestaurantFilterState,
    cuisines: List<CuisineFilter>,
    resultCount: Int,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    scopeFilters: (@Composable () -> Unit)? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filtrer les adresses", style = MaterialTheme.typography.titleLarge)
            scopeFilters?.invoke()
            RestaurantFilters(state, cuisines, showRating = true, showPrice = true)
            if (state.minimumRating != null) {
                Text(
                    "La note est la moyenne des visites, arrondie à une décimale. Les adresses sans note sont masquées.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text("$resultCount adresses affichées")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onReset) { Text("Réinitialiser") }
                OutlinedButton(onClick = onDismiss) { Text(confirmLabel) }
            }
        }
    }
}
