package fr.martinrocca.resto.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.Visit
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.add.VisitFormFields
import fr.martinrocca.resto.ui.add.buildVisitDraft
import fr.martinrocca.resto.ui.components.BackHeader
import fr.martinrocca.resto.ui.components.LocalPhoto
import kotlinx.coroutines.launch

@Composable
fun EditVisitScreen(
    restaurant: Restaurant?,
    visit: Visit?,
    viewModel: RestoViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var date by rememberSaveable(visit?.id) { mutableStateOf(visit?.date?.toString().orEmpty()) }
    var overallRating by rememberSaveable(visit?.id) { mutableIntStateOf(visit?.overallRating ?: 0) }
    var comment by rememberSaveable(visit?.id) { mutableStateOf(visit?.comment.orEmpty()) }
    var keptPhotoIds by rememberSaveable(visit?.id) {
        mutableStateOf(visit?.photos?.map { it.id }.orEmpty())
    }
    var photoUris by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        val available = (PhotoManager.MAX_PHOTOS_PER_VISIT - keptPhotoIds.size).coerceAtLeast(0)
        photoUris = (photoUris + uris.map { it.toString() }).distinct().take(available)
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackHeader(title = "Modifier la visite", onBack = onBack) },
    ) { padding ->
        if (restaurant == null || visit == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            ) {
                Text("Visite introuvable.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = restaurant.name,
                style = MaterialTheme.typography.headlineMedium,
            )
            val keptPhotos = visit.photos.filter { it.id in keptPhotoIds }
            if (keptPhotos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Photos actuelles", style = MaterialTheme.typography.titleMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(keptPhotos, key = { it.id }) { photo ->
                            Column {
                                LocalPhoto(
                                    relativePath = photo.relativePath,
                                    contentDescription = "Photo de la visite",
                                    modifier = Modifier.size(112.dp),
                                )
                                TextButton(
                                    onClick = {
                                        keptPhotoIds = keptPhotoIds.filterNot { it == photo.id }
                                    },
                                ) { Text("Retirer") }
                            }
                        }
                    }
                }
            }
            VisitFormFields(
                date = date,
                onDateChange = { date = it },
                overallRating = overallRating.takeIf { it > 0 },
                onOverallRatingChange = { overallRating = it ?: 0 },
                comment = comment,
                onCommentChange = { comment = it },
                photoCount = photoUris.size,
                onPickPhotos = { photoPicker.launch("image/*") },
                onClearPhotos = { photoUris = emptyList() },
                canPickPhotos =
                    keptPhotoIds.size + photoUris.size < PhotoManager.MAX_PHOTOS_PER_VISIT,
            )
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val result = runCatching {
                                buildVisitDraft(
                                    date = date,
                                    overallRating = overallRating.takeIf { it > 0 },
                                    comment = comment,
                                    photoUris = photoUris,
                                )
                            }.fold(
                                onSuccess = { draft ->
                                    viewModel.updateVisit(visit.id, draft, keptPhotoIds.toSet())
                                },
                                onFailure = { Result.failure(it) },
                            )
                            result.onSuccess { onSaved() }.onFailure {
                                errorMessage = it.message
                                    ?: "Impossible de modifier cette visite."
                            }
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 10.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("Enregistrer les modifications")
            }
        }
    }
}
