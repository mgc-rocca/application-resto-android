package fr.martinrocca.resto.ui.add

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.BackHeader
import fr.martinrocca.resto.ui.components.GeoapifySearchField
import java.time.LocalDate
import kotlinx.coroutines.launch

private enum class AddMode { WISHLIST, VISIT }

@Composable
fun AddRestaurantScreen(
    viewModel: RestoViewModel,
    knownTags: List<String>,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var geoapifyPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var tags by rememberSaveable { mutableStateOf("") }
    var michelinName by rememberSaveable { mutableStateOf(MichelinStatus.ABSENT.name) }
    val michelinStatus = MichelinStatus.valueOf(michelinName)
    var modeName by rememberSaveable { mutableStateOf<String?>(null) }
    val mode = modeName?.let(AddMode::valueOf)
    var wishlistNote by rememberSaveable { mutableStateOf("") }

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
        topBar = { BackHeader(title = "Ajouter", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nouvelle adresse", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Recherchez l’établissement pour récupérer son adresse et ses coordonnées, ou saisissez-le manuellement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            GeoapifySearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                isConfigured = viewModel.isGeoapifyConfigured,
                search = viewModel::searchPlaces,
                onSuggestionSelected = { suggestion ->
                    searchQuery = suggestion.formattedAddress
                    name = suggestion.name
                    address = suggestion.formattedAddress
                    latitude = suggestion.latitude
                    longitude = suggestion.longitude
                    geoapifyPlaceId = suggestion.placeId
                },
            )

            RestaurantFormFields(
                name = name,
                onNameChange = { name = it },
                address = address,
                onAddressChange = {
                    address = it
                    latitude = null
                    longitude = null
                    geoapifyPlaceId = null
                },
                tags = tags,
                onTagsChange = { tags = it },
                knownTags = knownTags,
                michelinStatus = michelinStatus,
                onMichelinStatusChange = { michelinName = it.name },
            )

            HorizontalDivider()
            Text("Que souhaitez-vous faire ?", style = MaterialTheme.typography.titleLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { modeName = AddMode.WISHLIST.name },
                    modifier = Modifier.weight(1f),
                ) { Text("Ajouter aux envies") }
                Button(
                    onClick = { modeName = AddMode.VISIT.name },
                    modifier = Modifier.weight(1f),
                ) { Text("J’y suis allé") }
            }

            when (mode) {
                AddMode.WISHLIST -> {
                    OutlinedTextField(
                        value = wishlistNote,
                        onValueChange = { wishlistNote = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note personnelle · facultative") },
                        minLines = 3,
                    )
                }

                AddMode.VISIT -> VisitFormFields(
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

                null -> Unit
            }

            errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (mode != null) {
                Button(
                    enabled = !isSaving,
                    onClick = {
                        scope.launch {
                            isSaving = true
                            errorMessage = null
                            val result = runCatching {
                                val selectedMode = requireNotNull(mode) { "Choisissez une action." }
                                val restaurant = buildRestaurantDraft(
                                    name = name,
                                    address = address,
                                    tags = tags,
                                    knownTags = knownTags,
                                    michelinStatus = michelinStatus,
                                    latitude = latitude,
                                    longitude = longitude,
                                    geoapifyPlaceId = geoapifyPlaceId,
                                )
                                when (selectedMode) {
                                    AddMode.WISHLIST -> viewModel
                                        .createWishlistRestaurant(restaurant, wishlistNote)
                                        .getOrThrow()

                                    AddMode.VISIT -> viewModel
                                        .createVisitedRestaurant(
                                            restaurant,
                                            buildVisitDraft(
                                                date = date,
                                                overallRating = overallRating.takeIf { it > 0 },
                                                comment = comment,
                                                photoUris = photoUris,
                                            ),
                                        )
                                        .getOrThrow()
                                }
                            }
                            result.onSuccess(onSaved).onFailure {
                                errorMessage = it.message ?: "Impossible d’enregistrer ce restaurant."
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
                    Text(
                        if (mode == AddMode.WISHLIST) "Enregistrer dans les envies"
                        else "Enregistrer la visite",
                    )
                }
            }
        }
    }
}
