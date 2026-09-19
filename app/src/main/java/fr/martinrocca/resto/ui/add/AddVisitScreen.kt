package fr.martinrocca.resto.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.BackHeader
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun AddVisitScreen(
    restaurant: Restaurant?,
    viewModel: RestoViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var overallRating by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        photoUris = (photoUris + uris.map { it.toString() }).distinct().take(20)
    }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BackHeader(
                title = restaurant?.name ?: "Nouvelle visite",
                onBack = onBack,
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
                text = restaurant.address,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            )
            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            Button(
                enabled = !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        val result = runCatching {
                            val visit = buildVisitDraft(
                                date = date,
                                overallRating = overallRating.takeIf { it > 0 },
                                comment = comment,
                                photoUris = photoUris,
                            )
                            viewModel.addVisit(restaurant.id, visit).getOrThrow()
                        }
                        result.onSuccess { onSaved() }.onFailure {
                            errorMessage = it.message ?: "Impossible d’enregistrer cette visite."
                        }
                        isSaving = false
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
                Text("Enregistrer la visite")
            }
        }
    }
}
