package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.data.remote.geoapify.GeoapifySuggestion
import kotlinx.coroutines.delay

@Composable
fun GeoapifySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    isConfigured: Boolean,
    search: suspend (String) -> Result<List<GeoapifySuggestion>>,
    onSuggestionSelected: (GeoapifySuggestion) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Rechercher un restaurant ou une adresse",
) {
    var suggestions by remember { mutableStateOf(emptyList<GeoapifySuggestion>()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var dismissedQuery by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(query, isConfigured) {
        suggestions = emptyList()
        error = null
        if (!isConfigured || query.trim().length < 3 || query == dismissedQuery) {
            isLoading = false
            return@LaunchedEffect
        }
        delay(350)
        isLoading = true
        try {
            search(query)
                .onSuccess { suggestions = it }
                .onFailure { error = it.message ?: "Recherche indisponible." }
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                dismissedQuery = null
                onQueryChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (isLoading) CircularProgressIndicator(strokeWidth = 2.dp)
            },
            singleLine = true,
            supportingText = if (!isConfigured) {
                { Text("Clé Geoapify absente : la saisie manuelle reste disponible.") }
            } else {
                null
            },
        )

        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        dismissedQuery = suggestion.formattedAddress
                        suggestions = emptyList()
                        onSuggestionSelected(suggestion)
                    },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = suggestion.formattedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
