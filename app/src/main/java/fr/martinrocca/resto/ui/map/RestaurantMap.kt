package fr.martinrocca.resto.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapColorScheme
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.theme.SagePrimary
import kotlinx.coroutines.delay

/** Personal coordinates and process-only Google coordinates remain separate from Room models. */
data class RestaurantMarker(val restaurant: Restaurant, val latitude: Double, val longitude: Double)

@Composable
fun RestaurantMap(
    restaurants: List<RestaurantMarker>,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialCameraTarget: MapTarget? = null,
    cameraTarget: MapTarget? = null,
    cameraRequestId: Int = 0,
    deviceLocation: MapTarget? = null,
    topContentInset: Int = 0,
    servicesError: String? = null,
    onCameraChanged: (MapTarget) -> Unit = {},
    onMapError: (String?) -> Unit = {},
) {
    if (servicesError != null) {
        Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text(servicesError, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val click by rememberUpdatedState(onRestaurantClick)
    val cameraChanged by rememberUpdatedState(onCameraChanged)
    val reportError by rememberUpdatedState(onMapError)
    val initialTarget = remember { cameraTarget ?: initialCameraTarget ?: PARIS_MAP_TARGET }
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val mapView = remember(context) {
        runCatching {
            MapView(context, GoogleMapOptions().mapType(GoogleMap.MAP_TYPE_NORMAL)
                .mapColorScheme(MapColorScheme.LIGHT)).apply { onCreate(null) }
        }.getOrNull()
    }
    if (mapView == null) {
        LaunchedEffect(Unit) { reportError("La carte Google n’a pas pu démarrer. Le Journal reste disponible.") }
        return
    }
    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        fun sync() {
            runCatching {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !started) {
                    mapView.onStart(); started = true
                }
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !resumed) {
                    mapView.onResume(); resumed = true
                }
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && resumed) {
                    mapView.onPause(); resumed = false
                }
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && started) {
                    mapView.onStop(); started = false
                }
            }.onFailure { reportError("Carte Google indisponible. Le Journal reste disponible.") }
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        val memoryCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { runCatching { mapView.onLowMemory() } }
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory()
            }
        }
        lifecycle.addObserver(observer)
        context.applicationContext.registerComponentCallbacks(memoryCallbacks)
        sync()
        onDispose {
            lifecycle.removeObserver(observer)
            context.applicationContext.unregisterComponentCallbacks(memoryCallbacks)
            runCatching {
                map?.cameraPosition?.toMapTarget()?.let(cameraChanged)
                if (resumed) mapView.onPause()
                if (started) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }
    AndroidView(factory = {
        mapView.apply {
            runCatching {
                getMapAsync { googleMap ->
                    runCatching {
                        googleMap.setMapColorScheme(MapColorScheme.LIGHT)
                        val json = context.assets.open("resto-map-style.json").bufferedReader().use { it.readText() }
                        val styleApplied = googleMap.setMapStyle(MapStyleOptions(json))
                        if (!styleApplied) {
                            reportError("Le style de carte n’a pas pu être appliqué.")
                        }
                        googleMap.isBuildingsEnabled = true
                        googleMap.isIndoorEnabled = false
                        googleMap.uiSettings.isMapToolbarEnabled = false
                        googleMap.uiSettings.isZoomControlsEnabled = false
                        googleMap.uiSettings.isMyLocationButtonEnabled = false
                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(initialTarget.toCameraPosition()))
                        googleMap.setOnMarkerClickListener { marker ->
                            (marker.tag as? String)?.let(click)
                            true
                        }
                        googleMap.setOnCameraIdleListener { cameraChanged(googleMap.cameraPosition.toMapTarget()) }
                        googleMap.setOnMapLoadedCallback {
                            loaded = true
                            reportError(if (styleApplied) null else "Le style de carte n’a pas pu être appliqué.")
                        }
                        map = googleMap
                    }.onFailure { reportError("Carte Google indisponible. Vérifiez sa configuration ; le Journal reste disponible.") }
                }
            }.onFailure { reportError("Carte Google indisponible. Le Journal reste disponible.") }
        }
    }, modifier = modifier.fillMaxSize())

    LaunchedEffect(mapView, loaded) {
        if (!loaded) {
            delay(15_000)
            if (!loaded) reportError("Le fond Google Maps ne s’est pas chargé. Vérifiez le réseau et la clé Google, ou ouvrez le Journal.")
        }
    }
    val restaurantMarkers = remember { mutableListOf<Marker>() }
    LaunchedEffect(map, restaurants) {
        val current = map ?: return@LaunchedEffect
        runCatching {
            restaurantMarkers.forEach(Marker::remove)
            restaurantMarkers.clear()
            val density = context.resources.displayMetrics.density
            val visitedIcon = BitmapDescriptorFactory.fromBitmap(createMarkerBitmap(density))
            val wishIcon = BitmapDescriptorFactory.fromBitmap(createMarkerBitmap(density, isWishlist = true))
            restaurants.forEach { item ->
                current.addMarker(MarkerOptions().position(LatLng(item.latitude, item.longitude))
                    .icon(if (item.restaurant.isVisited) visitedIcon else wishIcon).anchor(0.5f, 1f))?.let { marker ->
                    marker.tag = item.restaurant.id
                    restaurantMarkers += marker
                }
            }
        }.onFailure { reportError("Certains marqueurs n’ont pas pu être affichés. Vos fiches restent disponibles dans le Journal.") }
    }
    var locationMarker by remember { mutableStateOf<Marker?>(null) }
    LaunchedEffect(map, deviceLocation) {
        val current = map ?: return@LaunchedEffect
        runCatching {
            locationMarker?.remove()
            locationMarker = deviceLocation?.let {
                current.addMarker(MarkerOptions().position(LatLng(it.latitude, it.longitude))
                    .icon(BitmapDescriptorFactory.fromBitmap(createLocationBitmap(context.resources.displayMetrics.density)))
                    .anchor(0.5f, 0.5f))
            }
        }
    }
    LaunchedEffect(map, topContentInset) {
        // Google's logo remains visible at the bottom; the overlay only reserves the top area.
        runCatching { map?.setPadding(0, topContentInset, 0, 0) }
    }
    LaunchedEffect(map, cameraRequestId) {
        cameraTarget?.let { target -> runCatching { map?.animateCamera(CameraUpdateFactory.newCameraPosition(target.toCameraPosition())) } }
    }
}

private val PARIS_MAP_TARGET = MapTarget(48.8566, 2.3522, 11.5)
private fun MapTarget.toCameraPosition(): CameraPosition = CameraPosition.Builder()
    .target(LatLng(latitude, longitude)).zoom(zoom.toFloat()).bearing(bearing.toFloat()).tilt(tilt.toFloat()).build()
private fun CameraPosition.toMapTarget() = MapTarget(target.latitude, target.longitude, zoom.toDouble(), bearing.toDouble(), tilt.toDouble())

private fun createLocationBitmap(density: Float): Bitmap {
    val size = (24 * density).toInt().coerceAtLeast(24)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawCircle(size / 2f, size / 2f, 10 * density, paint)
        paint.color = Color.rgb(50, 117, 172) // V1 device-location dot #3275AC.
        canvas.drawCircle(size / 2f, size / 2f, 7 * density, paint)
    }
}

private fun createMarkerBitmap(
    density: Float,
    isWishlist: Boolean = false,
): Bitmap {
    val width = (54 * density).toInt().coerceAtLeast(54)
    val height = (66 * density).toInt().coerceAtLeast(66)
    val centerX = width / 2f
    val circleRadius = width * 0.38f
    val circleCenterY = circleRadius + 3 * density
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Every restaurant and wish uses the same fully opaque V1 color.
            color = SagePrimary.toArgb()
        }
        val pointer = Path().apply {
            moveTo(centerX - circleRadius * 0.52f, circleCenterY + circleRadius * 0.62f)
            lineTo(centerX, height.toFloat())
            lineTo(centerX + circleRadius * 0.52f, circleCenterY + circleRadius * 0.62f)
            close()
        }
        canvas.drawPath(pointer, markerPaint)
        canvas.drawCircle(centerX, circleCenterY, circleRadius, markerPaint)
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            strokeWidth = 3 * density
            strokeCap = Paint.Cap.ROUND
        }
        if (isWishlist) {
            val arm = circleRadius * 0.28f
            canvas.drawLine(centerX - arm, circleCenterY, centerX + arm, circleCenterY, centerPaint)
            canvas.drawLine(centerX, circleCenterY - arm, centerX, circleCenterY + arm, centerPaint)
        } else {
            canvas.drawCircle(centerX, circleCenterY, circleRadius * 0.2f, centerPaint)
        }
    }
}
