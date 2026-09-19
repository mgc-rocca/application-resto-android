package fr.martinrocca.resto.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.R
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.theme.MichelinOutline
import fr.martinrocca.resto.theme.MichelinRed

@Composable
fun MichelinLabel(status: MichelinStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = status.label },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.stars == 0) {
            Text(status.compactLabel, color = MichelinRed, style = MaterialTheme.typography.labelLarge)
        } else {
            repeat(status.stars) {
                Icon(
                    painterResource(R.drawable.ic_michelin_star),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MichelinRed,
                )
            }
        }
    }
}

@Composable
fun MichelinBadge(status: MichelinStatus) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MichelinOutline),
    ) {
        MichelinLabel(status, Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
fun MichelinFilterChip(status: MichelinStatus, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { MichelinLabel(status) },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White,
            selectedContainerColor = Color.White,
            labelColor = MichelinRed,
            selectedLabelColor = MichelinRed,
            iconColor = MichelinRed,
            selectedLeadingIconColor = MichelinRed,
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, MichelinOutline),
    )
}
