package fr.martinrocca.resto.ui.stats

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.michelinVisitCounts
import fr.martinrocca.resto.theme.MichelinOutline
import fr.martinrocca.resto.theme.MichelinRed
import fr.martinrocca.resto.ui.components.MichelinLabel
import fr.martinrocca.resto.ui.RestoViewModel
import java.text.DecimalFormat
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun StatsScreen(
    restaurants: List<Restaurant>,
    viewModel: RestoViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var statusIsError by rememberSaveable { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isWorking = true
                statusMessage = null
                try {
                    statusMessage = viewModel.exportBackup(uri).fold(
                        onSuccess = {
                            statusIsError = false
                            "Sauvegarde exportée avec succès."
                        },
                        onFailure = {
                            statusIsError = true
                            it.message ?: "L’export a échoué."
                        },
                    )
                } finally {
                    isWorking = false
                }
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }
    val stats = remember(restaurants) {
        val visits = restaurants.flatMap { it.visits }
        Stats(
            restaurantCount = restaurants.count(Restaurant::isVisited),
            visitCount = visits.size,
            averageRating = visits.map { it.overallRating }.average().takeUnless(Double::isNaN),
            wishlistCount = restaurants.count { it.wishlist != null },
            michelinCounts = michelinVisitCounts(restaurants),
            mostFrequentTags = restaurants
                .filter(Restaurant::isVisited)
                .flatMap { it.tags }
                .groupingBy { it.name }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(5),
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 22.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Stats", style = MaterialTheme.typography.displaySmall) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard("Restaurants", stats.restaurantCount.toString(), Modifier.weight(1f))
                StatCard("Visites", stats.visitCount.toString(), Modifier.weight(1f))
                StatCard(
                    "Note moyenne",
                    stats.averageRating?.let { DecimalFormat("0.0").format(it) } ?: "–",
                    Modifier.weight(1f),
                )
            }
        }
        item {
            StatCard("Envies", stats.wishlistCount.toString(), Modifier.fillMaxWidth())
        }
        item { Text("Guide Michelin", style = MaterialTheme.typography.titleLarge) }
        stats.michelinCounts.entries.toList().chunked(2).forEach { row ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { (status, count) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MichelinOutline),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(count.toString(), color = MichelinRed, style = MaterialTheme.typography.headlineMedium)
                                MichelinLabel(status)
                            }
                        }
                    }
                }
            }
        }
        if (stats.mostFrequentTags.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Cuisines les plus fréquentes", style = MaterialTheme.typography.titleLarge)
                        stats.mostFrequentTags.forEach { (tag, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(tag)
                                Text(count.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Données", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedButton(
                onClick = {
                    exportLauncher.launch("resto-sauvegarde-${LocalDate.now()}.zip")
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                }
                Text("Exporter une sauvegarde", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    restoreLauncher.launch(
                        arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed"),
                    )
                },
                enabled = !isWorking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Restore, contentDescription = null)
                Text("Restaurer une sauvegarde", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            Text(
                text = "Le fichier ZIP contient toutes les données et les photos. Il n’est pas chiffré : conservez-le dans un emplacement privé. Une restauration remplace le journal actuel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        statusMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (statusIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restaurer cette sauvegarde ?") },
            text = {
                Text("Le journal, les envies et les photos actuels seront remplacés. Cette action est irréversible.")
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Annuler") }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        scope.launch {
                            isWorking = true
                            statusMessage = null
                            try {
                                statusMessage = viewModel.restoreBackup(uri).fold(
                                    onSuccess = {
                                        statusIsError = false
                                        "Sauvegarde restaurée avec succès."
                                    },
                                    onFailure = {
                                        statusIsError = true
                                        it.message ?: "La restauration a échoué."
                                    },
                                )
                            } finally {
                                isWorking = false
                            }
                        }
                    },
                ) {
                    Text("Remplacer les données")
                }
            },
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Stats(
    val restaurantCount: Int,
    val visitCount: Int,
    val averageRating: Double?,
    val wishlistCount: Int,
    val michelinCounts: Map<MichelinStatus, Int>,
    val mostFrequentTags: List<Map.Entry<String, Int>>,
)
