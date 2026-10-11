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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.cuisineFilters
import fr.martinrocca.resto.navigation.AddButtonOverhang
import fr.martinrocca.resto.theme.restoFilledIconButtonColors
import fr.martinrocca.resto.ui.RestoViewModel
import fr.martinrocca.resto.ui.components.GooglePlacesSearchField
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
    onOpenJournal: () -> Unit,
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
    var mapError by remember { mutableStateOf<String?>(null) }
    var searchPlaceId by rememberSaveable { mutableStateOf<String?>(null) }
    var showUnmapped by rememberSaveable { mutableStateOf(false) }
    var locatingPlaceId by remember { mutableStateOf<String?>(null) }
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
    val googleCoordinates by viewModel.googleCoordinates.collectAsStateWithLifecycle()
    val servicesError = remember(context) { viewModel.googlePlacesService.mapError() }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) {
                viewModel.googlePlacesService.expireCoordinates()
                delay(60_000)
            }
        }
    }
    val visibleRestaurants = restaurants.filter { restaurant ->
        filters.matches(restaurant) && when (filter) {
            MapFilter.ALL -> restaurant.isVisited || restaurant.wishlist != null
            MapFilter.VISITED -> restaurant.isVisited
            MapFilter.WISHLIST -> restaurant.wishlist != null
        }
    }
    val mappedRestaurants = visibleRestaurants.mapNotNull { restaurant ->
        val cached = restaurant.googlePlaceId?.let { id ->
            googleCoordinates[id]?.let { viewModel.googlePlacesService.cachedCoordinates(id) }
        }
        val lat = restaurant.latitude ?: cached?.latitude
        val lng = restaurant.longitude ?: cached?.longitude
        if (lat == null || lng == null) null else RestaurantMarker(restaurant, lat, lng)
    }
    val unmappedRestaurants = visibleRestaurants.filter { restaurant -> mappedRestaurants.none { it.restaurant.id == restaurant.id } }
    if (showUnmapped) {
        AlertDialog(
            onDismissRequest = { showUnmapped = false },
            title = { Text("Adresses à localiser") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choisissez une adresse. La position Google est chargée uniquement à votre demande.")
                    unmappedRestaurants.forEach { restaurant ->
                        Text(restaurant.name, style = MaterialTheme.typography.titleSmall)
                        Text(restaurant.address, style = MaterialTheme.typography.bodySmall)
                        Row {
                            TextButton(onClick = { showUnmapped = false; onRestaurantClick(restaurant.id) }) { Text("Ouvrir la fiche") }
                            restaurant.googlePlaceId?.let { id ->
                                TextButton(enabled = locatingPlaceId == null && isNetworkAvailable, onClick = {
                                    locatingPlaceId = id
                                    scope.launch {
                                        try {
                                            viewModel.locateGooglePlace(id).onSuccess { coordinates ->
                                                cameraTarget = MapTarget(coordinates.latitude, coordinates.longitude, 15.0)
                                                cameraRequestId++
                                                showUnmapped = false
                                                locationError = null
                                            }.onFailure { locationError = it.message ?: "Position Google indisponible." }
                                        } finally { locatingPlaceId = null }
                                    }
                                }) { Text(if (locatingPlaceId == id) "Chargement…" else "Localiser") }
                            }
                        }
                    }
                    locationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = { showUnmapped = false }) { Text("Fermer") } },
        )
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
            servicesError = servicesError,
            onMapError = { mapError = it },
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
                GooglePlacesSearchField(
                    query = addressQuery,
                    onQueryChange = { addressQuery = it },
                    service = viewModel.googlePlacesService,
                    restaurantsOnly = false,
                    bias = deviceLocation ?: restoredCameraTarget,
                    selectedPlaceId = searchPlaceId,
                    onClearAssociation = { searchPlaceId = null },
                    onPlaceSelected = { id ->
                        searchPlaceId = id
                        scope.launch {
                            viewModel.locateGooglePlace(id).onSuccess { coordinates ->
                                cameraTarget = MapTarget(coordinates.latitude, coordinates.longitude, 15.0)
                                cameraRequestId++
                                locationError = null
                            }.onFailure { locationError = it.message ?: "Position Google indisponible." }
                        }
                    },
                )
                Text(
                    text = buildString {
                        append("${mappedRestaurants.size} adresses affichées")
                        filters.minimumRating?.let { append(" · Note ≥ $it/10") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (unmappedRestaurants.isNotEmpty()) {
                    TextButton(onClick = { showUnmapped = true }) { Text("${unmappedRestaurants.size} adresses à localiser") }
                }
                (servicesError ?: mapError)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                if (servicesError != null || mapError != null || !isNetworkAvailable) {
                    TextButton(onClick = onOpenJournal) { Text("Ouvrir le Journal") }
                }
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
            colors = restoFilledIconButtonColors(),
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
