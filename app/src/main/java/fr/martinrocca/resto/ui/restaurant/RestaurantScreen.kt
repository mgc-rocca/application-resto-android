package fr.martinrocca.resto.ui.restaurant

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.cuisineTags
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.Visit
import fr.martinrocca.resto.domain.model.shareText
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.BackHeader
import fr.martinrocca.resto.ui.components.RatingBadge
import fr.martinrocca.resto.ui.components.RestaurantRatingBadge
import fr.martinrocca.resto.ui.components.LocalPhoto
import fr.martinrocca.resto.ui.components.RestaurantDistinctions
import fr.martinrocca.resto.ui.components.toFrenchDate
import kotlinx.coroutines.launch

@Composable
fun RestaurantScreen(
    restaurant: Restaurant?,
    viewModel: RestoViewModel,
    onBack: () -> Unit,
    onAddVisit: (String) -> Unit,
    onEditRestaurant: (String) -> Unit,
    onEditVisit: (String, String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmRestaurantDeletion by rememberSaveable { mutableStateOf(false) }
    var visitToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BackHeader(
                title = restaurant?.name ?: "Restaurant",
                onBack = onBack,
                actions = {
                    if (restaurant != null) {
                        IconButton(onClick = { onEditRestaurant(restaurant.id) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Modifier le restaurant")
                        }
                        IconButton(onClick = { confirmRestaurantDeletion = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Supprimer le restaurant")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (restaurant == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text("Restaurant introuvable.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = padding.calculateTopPadding() + 12.dp,
                end = 20.dp,
                bottom = padding.calculateBottomPadding() + 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (restaurant.isVisited) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RestaurantRatingBadge(restaurant)
                            Text(
                                text = if (restaurant.visits.size > 1) {
                                    "Note moyenne · ${restaurant.visits.size} visites"
                                } else "1 visite",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    if (restaurant.cuisineTags.isNotEmpty()) {
                        Text(
                            text = restaurant.cuisineTags.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    RestaurantDistinctions(restaurant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = restaurant.address,
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, restaurant.shareText())
                                    putExtra(Intent.EXTRA_TITLE, restaurant.name)
                                    putExtra(Intent.EXTRA_SUBJECT, restaurant.name)
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(intent, "Partager le restaurant"))
                                }.onFailure { errorMessage = "Impossible d’ouvrir le partage." }
                            },
                        ) {
                            Icon(Icons.Outlined.Share, null)
                            Text("Partager", Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            restaurant.wishlist?.let { wishlist ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Dans vos envies", style = MaterialTheme.typography.titleLarge)
                            wishlist.note?.let {
                                Text(it, fontStyle = FontStyle.Italic)
                            }
                            Button(
                                onClick = { onAddVisit(restaurant.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("J’y suis allé")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.removeFromWishlist(restaurant.id)
                                            .onSuccess {
                                                if (!restaurant.isVisited) onDeleted()
                                            }
                                            .onFailure { errorMessage = it.message }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Retirer des envies")
                            }
                        }
                    }
                }
            }

            if (restaurant.isVisited) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Historique", style = MaterialTheme.typography.headlineMedium)
                        Button(onClick = { onAddVisit(restaurant.id) }) {
                            Text("+ Visite")
                        }
                    }
                }
                items(restaurant.visits, key = Visit::id) { visit ->
                    VisitCard(
                        visit = visit,
                        onEdit = { onEditVisit(restaurant.id, visit.id) },
                        onDelete = { visitToDelete = visit.id },
                    )
                }
            }

            errorMessage?.let {
                item { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (confirmRestaurantDeletion && restaurant != null) {
        AlertDialog(
            onDismissRequest = { confirmRestaurantDeletion = false },
            title = { Text("Supprimer ${restaurant.name} ?") },
            text = { Text("Toutes ses visites et photos seront définitivement supprimées.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestaurantDeletion = false
                        scope.launch {
                            viewModel.deleteRestaurant(restaurant.id)
                                .onSuccess { onDeleted() }
                                .onFailure { errorMessage = it.message }
                        }
                    },
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestaurantDeletion = false }) {
                    Text("Annuler")
                }
            },
        )
    }

    visitToDelete?.let { visitId ->
        val deletingLastVisit = restaurant?.visits?.size == 1
        AlertDialog(
            onDismissRequest = { visitToDelete = null },
            title = { Text("Supprimer cette visite ?") },
            text = {
                Text(
                    if (deletingLastVisit) {
                        "Ses photos seront aussi supprimées. Le restaurant sera conservé dans vos envies."
                    } else {
                        "Ses photos seront aussi supprimées."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        visitToDelete = null
                        scope.launch {
                            viewModel.deleteVisit(visitId)
                                .onFailure { errorMessage = it.message }
                        }
                    },
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { visitToDelete = null }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun VisitCard(
    visit: Visit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RatingBadge(visit.overallRating)
                    Text(visit.date.toFrenchDate(), style = MaterialTheme.typography.titleLarge)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifier la visite")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Supprimer la visite")
                    }
                }
            }

            visit.comment?.let {
                HorizontalDivider()
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            if (visit.photos.isNotEmpty()) {
                HorizontalDivider()
                Text("Photos", style = MaterialTheme.typography.titleMedium)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = visit.photos,
                        key = { _, photo -> photo.id },
                    ) { index, photo ->
                        LocalPhoto(
                            relativePath = photo.relativePath,
                            contentDescription = "Photo ${index + 1} de la visite",
                        )
                    }
                }
            }
        }
    }
}
