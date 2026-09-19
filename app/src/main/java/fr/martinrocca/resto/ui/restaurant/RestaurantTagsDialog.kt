package fr.martinrocca.resto.ui.restaurant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.ui.add.RestaurantTagsField
import fr.martinrocca.resto.ui.add.canonicalizeTags
import kotlinx.coroutines.launch

@Composable
fun RestaurantTagsDialog(
    restaurant: Restaurant,
    knownTags: List<String>,
    onDismiss: () -> Unit,
    onSave: suspend (List<String>) -> Result<Unit>,
) {
    var tags by rememberSaveable(restaurant.id) {
        mutableStateOf(restaurant.tags.joinToString(", ") { it.name })
    }
    var query by rememberSaveable { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Tags du restaurant") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RestaurantTagsField(tags, { tags = it }, knownTags, query, { query = it })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Annuler") } },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        error = null
                        try {
                            onSave(canonicalizeTags("$tags,$query", knownTags))
                                .onSuccess { onDismiss() }
                                .onFailure { error = it.message ?: "Impossible de modifier les tags." }
                        } finally {
                            saving = false
                        }
                    }
                },
            ) { Text(if (saving) "Enregistrement…" else "Enregistrer") }
        },
    )
}
