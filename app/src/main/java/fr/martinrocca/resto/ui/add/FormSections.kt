package fr.martinrocca.resto.ui.add

import android.app.DatePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.domain.model.RestaurantDraft
import fr.martinrocca.resto.domain.model.VisitDraft
import fr.martinrocca.resto.domain.model.canonicalTagName
import fr.martinrocca.resto.domain.model.tagEquivalenceKey
import fr.martinrocca.resto.domain.model.tagSuggestionScore
import fr.martinrocca.resto.ui.components.MichelinPicker
import fr.martinrocca.resto.ui.components.RatingPicker
import fr.martinrocca.resto.ui.components.toFrenchDate
import java.time.LocalDate

@Composable
fun RestaurantFormFields(
    name: String,
    onNameChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    tagQuery: String,
    onTagQueryChange: (String) -> Unit,
    knownTags: List<String>,
    michelinStatus: MichelinStatus,
    onMichelinStatusChange: (MichelinStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom du restaurant") },
            singleLine = true,
        )
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Adresse") },
            minLines = 2,
        )
        CuisineTagField(
            value = tags,
            onValueChange = onTagsChange,
            knownTags = knownTags,
            query = tagQuery,
            onQueryChange = onTagQueryChange,
        )
        MichelinPicker(
            selected = michelinStatus,
            onSelected = onMichelinStatusChange,
        )
    }
}

@Composable
fun VisitFormFields(
    date: String,
    onDateChange: (String) -> Unit,
    overallRating: Int?,
    onOverallRatingChange: (Int?) -> Unit,
    comment: String,
    onCommentChange: (String) -> Unit,
    photoCount: Int,
    onPickPhotos: () -> Unit,
    onClearPhotos: () -> Unit,
    canPickPhotos: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("La visite", style = MaterialTheme.typography.headlineMedium)
        VisitDatePicker(
            date = date,
            onDateChange = onDateChange,
        )
        RatingPicker(
            selected = overallRating,
            onSelected = onOverallRatingChange,
        )
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Commentaire") },
            minLines = 4,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Photos", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = onPickPhotos,
                enabled = canPickPhotos,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        !canPickPhotos -> "Limite de ${PhotoManager.MAX_PHOTOS_PER_VISIT} photos atteinte"
                        photoCount == 0 -> "Ajouter des photos (${PhotoManager.MAX_PHOTOS_PER_VISIT} max)"
                        photoCount == 1 -> "1 photo sélectionnée"
                        else -> "$photoCount photos sélectionnées"
                    },
                )
            }
            if (photoCount > 0) {
                TextButton(onClick = onClearPhotos) {
                    Text("Retirer la sélection")
                }
            }
        }
    }
}

fun buildRestaurantDraft(
    name: String,
    address: String,
    tags: String,
    knownTags: List<String> = emptyList(),
    michelinStatus: MichelinStatus,
    latitude: Double? = null,
    longitude: Double? = null,
    geoapifyPlaceId: String? = null,
): RestaurantDraft = RestaurantDraft(
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    geoapifyPlaceId = geoapifyPlaceId,
    tags = canonicalizeTags(tags, knownTags),
    michelinStatus = michelinStatus,
)

fun buildVisitDraft(
    date: String,
    overallRating: Int?,
    comment: String,
    photoUris: List<String> = emptyList(),
): VisitDraft {
    val parsedDate = runCatching { LocalDate.parse(date.trim()) }
        .getOrElse { throw IllegalArgumentException("La date doit respecter le format AAAA-MM-JJ.") }
    val rating = requireNotNull(overallRating) { "La note globale est obligatoire." }
    return VisitDraft(
        date = parsedDate,
        overallRating = rating,
        comment = comment,
        photoUris = photoUris,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CuisineTagField(
    value: String,
    onValueChange: (String) -> Unit,
    knownTags: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val selectedNames = remember(value, knownTags) {
        canonicalizeTags(value, knownTags)
    }
    val suggestions = remember(query, knownTags, selectedNames) {
        knownTags
            .filter(String::isNotBlank)
            .distinctBy(::tagEquivalenceKey)
            .filterNot { name -> selectedNames.any { tagEquivalenceKey(it) == tagEquivalenceKey(name) } }
            .map { it to tagSuggestionScore(query, it) }
            .filter { query.isBlank() || it.second != Int.MAX_VALUE }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
            .take(if (query.isBlank()) 12 else 6)
            .map(Pair<String, Int>::first)
    }
    fun addTags(input: String) {
        onValueChange(canonicalizeTags("$value,$input", knownTags).joinToString(", "))
        onQueryChange("")
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedNames.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedNames.forEach { name ->
                    InputChip(
                        selected = true,
                        onClick = { onValueChange(selectedNames.filterNot { it == name }.joinToString(", ")) },
                        label = { Text(name) },
                        trailingIcon = { Icon(Icons.Outlined.Close, "Retirer $name") },
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Ajouter un tag de cuisine") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { addTags(query) }),
            trailingIcon = {
                IconButton(onClick = { addTags(query) }, enabled = query.isNotBlank()) {
                    Icon(Icons.Outlined.Add, "Ajouter le tag")
                }
            },
        )
        if (suggestions.isNotEmpty()) {
            Text(
                text = if (query.isBlank()) "Tags existants" else "Suggestions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = { addTags(suggestion) },
                        label = { Text(suggestion) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitDatePicker(
    date: String,
    onDateChange: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Date de la visite", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateChange(LocalDate.of(year, month + 1, day).toString())
                    },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth,
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Text(selectedDate.toFrenchDate(), modifier = Modifier.weight(1f))
        }
    }
}

fun canonicalizeTags(value: String, knownTags: List<String>): List<String> {
    val canonicalNames = knownTags.toMutableList()
    val result = mutableListOf<String>()
    value.split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .forEach { rawTag ->
            val canonical = canonicalTagName(rawTag, canonicalNames)
            if (result.none { tagEquivalenceKey(it) == tagEquivalenceKey(canonical) }) {
                result += canonical
                canonicalNames += canonical
            }
        }
    return result
}
