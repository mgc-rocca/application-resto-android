package fr.martinrocca.resto.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.martinrocca.resto.theme.DisabledElement

internal val AddButtonOverhang = 26.dp

@Composable
fun RestoBottomBar(
    currentRoute: String?,
    onDestinationClick: (AppDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        NavigationBar(
            modifier = Modifier.padding(top = AddButtonOverhang),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            AppDestination.topLevel.take(2).forEach { destination ->
                DestinationItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onDestinationClick(destination) },
                )
            }
            NavigationBarItem(
                selected = false,
                onClick = {},
                enabled = false,
                icon = {},
            )
            AppDestination.topLevel.drop(2).forEach { destination ->
                DestinationItem(
                    destination = destination,
                    selected = currentRoute == destination.route,
                    onClick = { onDestinationClick(destination) },
                )
            }
        }
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Ajouter un restaurant")
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DestinationItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(destination.icon, contentDescription = null) },
        label = { Text(destination.label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIconColor = DisabledElement,
            disabledTextColor = DisabledElement,
        ),
    )
}
