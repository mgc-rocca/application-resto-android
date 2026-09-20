package fr.martinrocca.resto.ui.map

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.cuisineFilters
import fr.martinrocca.resto.navigation.AddButtonOverhang
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.GeoapifySearchField
import fr.martinrocca.resto.ui.components.RestaurantFilterSheet
import fr.martinrocca.resto.ui.components.RestaurantFiltersButton
import fr.martinrocca.resto.ui.components.rememberRestaurantFilterState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class MapFilter(val label: String) {
    ALL("Tous"),
    VISITED("Visités"),
    WISHLIST("Envies"),
}

@Composable
fun MapScreen(
    restaurants: List<Restaurant>,
    viewModel: RestoViewModel,
    onRestaurantClick: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var addressQuery by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(MapFilter.ALL.name) }
    val filters = rememberRestaurantFilterState()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val cuisines = remember(restaurants) { cuisineFilters(restaurants) }
    val activeFilters = filters.activeCount + if (filterName == MapFilter.ALL.name) 0 else 1
    var overlayHeight by remember { mutableIntStateOf(0) }
    var cameraTarget by remember { mutableStateOf<MapTarget?>(null) }
    var cameraRequestId by remember { mutableIntStateOf(0) }
    var deviceLocation by remember { mutableStateOf<MapTarget?>(null) }
    var locating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    fun locate() {
        if (locating) return
        locationError = null
        locating = true
        locationJob = scope.launch {
            try {
                val location = currentDeviceLocation(context)
                val target = MapTarget(location.latitude, location.longitude, if (location.accuracy > 500) 12.0 else 15.0)
                deviceLocation = target
                cameraTarget = target
                cameraRequestId++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                locationError = error.message ?: "Impossible d’obtenir votre position."
            } finally {
                locating = false
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions.values.any { it }) locate()
        else locationError = "Autorisez la localisation dans les paramètres de Resto pour vous situer."
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) locationJob?.cancel()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            locationJob?.cancel()
        }
    }
    var savedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedZoom by rememberSaveable { mutableStateOf<Double?>(null) }
    var savedBearing by rememberSaveable { mutableStateOf(0.0) }
    var savedTilt by rememberSaveable { mutableStateOf(0.0) }
    val restoredCameraTarget = savedLatitude?.let { latitude ->
        val longitude = savedLongitude ?: return@let null
        val zoom = savedZoom ?: return@let null
        MapTarget(latitude, longitude, zoom, savedBearing, savedTilt)
    }
    val isNetworkAvailable = rememberIsNetworkAvailable()
    val filter = MapFilter.valueOf(filterName)
    val mappedRestaurants = remember(restaurants, filter, filters.michelin, filters.cuisineKey, filters.categories, filters.minimumRating, filters.priceRange) {
        restaurants.filter { restaurant ->
            restaurant.latitude != null && restaurant.longitude != null &&
                filters.matches(restaurant) && when (filter) {
                MapFilter.ALL -> restaurant.isVisited || restaurant.wishlist != null
                MapFilter.VISITED -> restaurant.isVisited
                MapFilter.WISHLIST -> restaurant.wishlist != null
            }
        }
    }

    if (showFilters) {
        RestaurantFilterSheet(
            state = filters,
            cuisines = cuisines,
            resultCount = mappedRestaurants.size,
            confirmLabel = "Voir la carte",
            onDismiss = { showFilters = false },
            onReset = { filters.reset(); filterName = MapFilter.ALL.name },
            scopeFilters = {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MapFilter.entries.forEach { option ->
                        FilterChip(
                            selected = option == filter,
                            onClick = { filterName = option.name },
                            label = { Text(option.label) },
                        )
                    }
                }
            },
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // The navigation bar reserves an empty strip for the raised + button.
            // Render the map through that strip, stopping at the actual menu surface.
            .padding(bottom = (contentPadding.calculateBottomPadding() - AddButtonOverhang).coerceAtLeast(0.dp)),
    ) {
        RestaurantMap(
            restaurants = mappedRestaurants,
            initialCameraTarget = restoredCameraTarget,
            cameraTarget = cameraTarget,
            cameraRequestId = cameraRequestId,
            deviceLocation = deviceLocation,
            topContentInset = overlayHeight,
            onCameraChanged = { target ->
                savedLatitude = target.latitude
                savedLongitude = target.longitude
                savedZoom = target.zoom
                savedBearing = target.bearing
                savedTilt = target.tilt
            },
            onRestaurantClick = onRestaurantClick,
            modifier = Modifier.fillMaxSize(),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.65f)
                .onSizeChanged { overlayHeight = it.height },
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = contentPadding.calculateTopPadding())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Carte", style = MaterialTheme.typography.headlineMedium)
                    RestaurantFiltersButton(activeFilters) { showFilters = true }
                }
                GeoapifySearchField(
                    query = addressQuery,
                    onQueryChange = { addressQuery = it },
                    isConfigured = viewModel.isGeoapifyConfigured,
                    search = viewModel::searchPlaces,
                    onSuggestionSelected = { suggestion ->
                        addressQuery = suggestion.formattedAddress
                        cameraTarget = MapTarget(suggestion.latitude, suggestion.longitude, 15.0)
                        cameraRequestId++
                    },
                    label = "Adresse",
                )
                Text(
                    text = "${mappedRestaurants.size} adresses affichées",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isNetworkAvailable) {
                    Text(
                        text = "Hors connexion : le fond de carte et la recherche peuvent être incomplets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                locationError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        val intent = if (context.hasLocationPermission()) {
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        }
                        runCatching { context.startActivity(intent) }
                    }) { Text("Ouvrir les réglages") }
                }
            }
        }
        FilledIconButton(
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp, end = 16.dp).size(48.dp),
            enabled = !locating,
            onClick = {
                if (context.hasLocationPermission()) locate()
                else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            },
        ) {
            if (locating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.MyLocation, "Me localiser")
        }
    }
}

data class MapTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double,
    val bearing: Double = 0.0,
    val tilt: Double = 0.0,
)
