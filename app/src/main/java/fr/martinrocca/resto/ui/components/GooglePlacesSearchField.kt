package fr.martinrocca.resto.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import fr.martinrocca.resto.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.widget.BasicPlaceAutocomplete
import com.google.android.libraries.places.widget.model.AutocompleteUiCustomization
import com.google.android.libraries.places.widget.model.AutocompleteListDensity
import fr.martinrocca.resto.R
import fr.martinrocca.resto.data.remote.google.GooglePlacesService
import fr.martinrocca.resto.theme.restoTextFieldColors
import fr.martinrocca.resto.ui.google.GooglePlaceDetailsActivity
import fr.martinrocca.resto.ui.map.MapTarget

@Composable
fun GooglePlacesSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    service: GooglePlacesService,
    onPlaceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    restaurantsOnly: Boolean = true,
    bias: MapTarget? = null,
    selectedPlaceId: String? = null,
    onClearAssociation: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var widerSearch by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        // Cancellation is silent and never changes an existing association or personal fields.
        if (data != null) {
            try {
                val place = BasicPlaceAutocomplete.getPlaceFromIntent(data)
                val status = BasicPlaceAutocomplete.getResultStatusFromIntent(data)
                if (result.resultCode == Activity.RESULT_OK && !place?.id.isNullOrBlank()) {
                    error = null
                    onPlaceSelected(requireNotNull(place.id))
                } else if (status != null && !status.isSuccess && !status.isCanceled) {
                    error = "Recherche Google indisponible. Vérifiez la connexion, la clé et les quotas. La saisie manuelle reste possible."
                }
            } catch (_: Exception) {
                error = "Recherche Google indisponible. La saisie manuelle reste possible."
            }
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            colors = restoTextFieldColors(),
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (restaurantsOnly) "Nom à rechercher sur Google" else "Adresse ou lieu") },
            singleLine = true,
        )
        if (restaurantsOnly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = widerSearch, onCheckedChange = { widerSearch = it })
                Text("Élargir la recherche à tous les établissements", style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedButton(onClick = {
            error = service.preparePlaces()
            if (error == null) {
                try {
                    val builder = BasicPlaceAutocomplete.IntentBuilder()
                        .setInitialQuery(query)
                        .setAutocompleteUiCustomization(
                            AutocompleteUiCustomization.builder()
                                .listDensity(AutocompleteListDensity.MULTI_LINE)
                                .theme(R.style.Theme_Resto_Places)
                                .build(),
                        )
                    // Five primary categories cannot cover every specialist/bistro type.
                    // Explicit wider search removes this filter without extra background calls.
                    if (restaurantsOnly && !widerSearch) builder.setTypesFilter(listOf("restaurant", "cafe", "bar", "bakery", "meal_takeaway"))
                    bias?.let { point ->
                        builder.setOrigin(LatLng(point.latitude, point.longitude))
                        // Bias only, never a country restriction: foreign places remain discoverable.
                        val latitude = point.latitude.coerceIn(-89.5, 89.5)
                        val longitude = point.longitude.coerceIn(-179.5, 179.5)
                        builder.setLocationBias(RectangularBounds.newInstance(
                            LatLng(latitude - 0.35, longitude - 0.35),
                            LatLng(latitude + 0.35, longitude + 0.35),
                        ))
                    }
                    // UI Kit owns suggestions, attribution, throttling and session billing.
                    launcher.launch(builder.build(context))
                } catch (_: Exception) {
                    error = "Google Places n’est pas disponible sur cet appareil. Utilisez la saisie manuelle."
                }
            }
        }) { Text("Rechercher avec Google") }
        Row {
            listOf("Conditions" to "CONDITIONS.md", "Confidentialité" to "CONFIDENTIALITE.md").forEach { (label, file) ->
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PUBLIC_DOCUMENTS_BASE_URL + file))) }
                        .onFailure { error = "Impossible d’ouvrir le document. Consultez les documents publics du dépôt Resto." }
                }) { Text(label, style = MaterialTheme.typography.labelSmall) }
            }
        }
        selectedPlaceId?.let { id ->
            Row {
                GooglePlaceDetailsButton(id)
                onClearAssociation?.let { clear -> TextButton(onClick = clear) { Text("Dissocier") } }
            }
            if (restaurantsOnly) {
                Text(
                    "Établissement Google associé. Complétez ci-dessous le nom et l’adresse de votre carnet.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
fun GooglePlaceDetailsButton(placeId: String) {
    val context = LocalContext.current
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column {
        TextButton(onClick = {
            runCatching { context.startActivity(GooglePlaceDetailsActivity.intent(context, placeId)) }
                .onFailure { error = "Informations Google indisponibles. Votre fiche reste accessible." }
        }) { Text("Informations Google Maps") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Optional durable coordinates: only independent user input, never a copied Google response. */
@Composable
fun PersonalCoordinatesFields(latitude: String, longitude: String, onChange: (String, String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }) { Text("Coordonnées personnelles (facultatif)") }
        if (expanded) {
            OutlinedTextField(value = latitude, onValueChange = { onChange(it, longitude) }, colors = restoTextFieldColors(),
                label = { Text("Latitude personnelle") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = longitude, onValueChange = { onChange(latitude, it) }, colors = restoTextFieldColors(),
                label = { Text("Longitude personnelle") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
    }
}

fun parsePersonalCoordinate(value: String): Double? {
    if (value.isBlank()) return null
    return requireNotNull(value.trim().replace(',', '.').toDoubleOrNull()) { "Coordonnée invalide." }
}
