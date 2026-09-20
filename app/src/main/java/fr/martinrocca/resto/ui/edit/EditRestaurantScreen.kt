package fr.martinrocca.resto.ui.edit

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.add.RestaurantFormFields
import fr.martinrocca.resto.ui.add.buildRestaurantDraft
import fr.martinrocca.resto.ui.components.BackHeader
import fr.martinrocca.resto.ui.components.GeoapifySearchField
import kotlinx.coroutines.launch

@Composable
fun EditRestaurantScreen(
    restaurant: Restaurant?,
    viewModel: RestoViewModel,
    knownTags: List<String>,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(restaurant?.id) { mutableStateOf(restaurant?.name.orEmpty()) }
    var address by rememberSaveable(restaurant?.id) { mutableStateOf(restaurant?.address.orEmpty()) }
    var searchQuery by rememberSaveable(restaurant?.id) { mutableStateOf("") }
    var latitude by rememberSaveable(restaurant?.id) { mutableStateOf(restaurant?.latitude) }
    var longitude by rememberSaveable(restaurant?.id) { mutableStateOf(restaurant?.longitude) }
    var placeId by rememberSaveable(restaurant?.id) { mutableStateOf(restaurant?.geoapifyPlaceId) }
    var tags by rememberSaveable(restaurant?.id) {
        mutableStateOf(restaurant?.tags?.joinToString(", ") { it.name }.orEmpty())
    }
    var michelinName by rememberSaveable(restaurant?.id) {
        mutableStateOf(restaurant?.michelinStatus?.name ?: MichelinStatus.ABSENT.name)
    }
    var tagQuery by rememberSaveable(restaurant?.id) { mutableStateOf("") }
    var wishlistNote by rememberSaveable(restaurant?.id) {
        mutableStateOf(restaurant?.wishlist?.note.orEmpty())
    }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackHeader(title = "Modifier le restaurant", onBack = onBack) },
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
                    placeId = suggestion.placeId
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
                    placeId = null
                },
                tags = tags,
                onTagsChange = { tags = it },
                tagQuery = tagQuery,
                onTagQueryChange = { tagQuery = it },
                knownTags = knownTags,
                michelinStatus = MichelinStatus.valueOf(michelinName),
                onMichelinStatusChange = { michelinName = it.name },
            )
            if (restaurant.wishlist != null) {
                OutlinedTextField(
                    value = wishlistNote,
                    onValueChange = { wishlistNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Commentaire") },
                    minLines = 3,
                )
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                enabled = !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        errorMessage = null
                        try {
                            val draft = runCatching {
                                buildRestaurantDraft(
                                    name = name,
                                    address = address,
                                    tags = listOf(tags, tagQuery).joinToString(","),
                                    knownTags = knownTags,
                                    michelinStatus = MichelinStatus.valueOf(michelinName),
                                    latitude = latitude,
                                    longitude = longitude,
                                    geoapifyPlaceId = placeId,
                                )
                            }
                            val result = draft.fold(
                                onSuccess = {
                                    viewModel.updateRestaurant(restaurant.id, it, wishlistNote)
                                },
                                onFailure = { Result.failure(it) },
                            )
                            result.onSuccess { onSaved() }.onFailure {
                                errorMessage = it.message
                                    ?: "Impossible de modifier ce restaurant."
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
