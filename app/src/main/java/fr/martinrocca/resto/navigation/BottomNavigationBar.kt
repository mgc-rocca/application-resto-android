package fr.martinrocca.resto.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RestoBottomBar(
    currentRoute: String?,
    onDestinationClick: (AppDestination) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        NavigationBar(modifier = Modifier.padding(top = 26.dp)) {
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
    )
}
