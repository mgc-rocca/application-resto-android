package fr.martinrocca.resto.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Gravity
import android.view.View
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.ui.components.ratingColor
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun RestaurantMap(
    restaurants: List<Restaurant>,
    initialCameraTarget: MapTarget?,
    cameraTarget: MapTarget?,
    cameraRequestId: Int,
    deviceLocation: MapTarget?,
    topContentInset: Int,
    onCameraChanged: (MapTarget) -> Unit,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerStyle by remember { mutableStateOf<Style?>(null) }
    val currentOnRestaurantClick by rememberUpdatedState(onRestaurantClick)
    val currentOnCameraChanged by rememberUpdatedState(onCameraChanged)
    val currentInitialCameraTarget by rememberUpdatedState(initialCameraTarget)

    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        var destroyed = false
        mapView.onCreate(null)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
            started = true
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
            resumed = true
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) {
                    destroyed = true
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            mapView.visibility = View.INVISIBLE
            lifecycle.removeObserver(observer)
            if (!destroyed) {
                if (resumed) mapView.onPause()
                if (started) mapView.onStop()
                destroyed = true
                mapView.onDestroy()
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                visibility = View.VISIBLE
                getMapAsync { mapLibreMap ->
                    map = mapLibreMap
                    mapLibreMap.uiSettings.isCompassEnabled = true
                    mapLibreMap.uiSettings.isAttributionEnabled = true
                    mapLibreMap.uiSettings.compassGravity = Gravity.BOTTOM or Gravity.END
                    val density = context.resources.displayMetrics.density
                    mapLibreMap.uiSettings.setCompassMargins(0, 0, (24 * density).toInt(), (88 * density).toInt())
                    val initialTarget = cameraTarget ?: currentInitialCameraTarget ?: PARIS_MAP_TARGET
                    mapLibreMap.moveCamera(CameraUpdateFactory.newCameraPosition(initialTarget.toCameraPosition()))
                    mapLibreMap.setStyle(
                        Style.Builder().fromUri("asset://resto-map-style.json"),
                    ) { style ->
                        addMarkerAssets(style, context.resources.displayMetrics.density)
                        markerStyle = style
                    }
                }
            }
        },
        modifier = modifier,
    )

    LaunchedEffect(markerStyle, restaurants) {
        markerStyle
            ?.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
            ?.setGeoJson(restaurants.toFeatureCollection())
    }

    LaunchedEffect(map, topContentInset) {
        // Keep camera targets in the visible area while rendering the map behind the overlay.
        map?.moveCamera(CameraUpdateFactory.paddingTo(0.0, topContentInset.toDouble(), 0.0, 0.0))
    }

    DisposableEffect(map) {
        val currentMap = map
        if (currentMap == null) return@DisposableEffect onDispose {}
        val clickListener = MapLibreMap.OnMapClickListener { point ->
            val feature = currentMap.queryRenderedFeatures(
                currentMap.projection.toScreenLocation(point),
                MARKER_LAYER_ID,
            ).firstOrNull()
            feature?.getStringProperty(RESTAURANT_ID_PROPERTY)?.let(currentOnRestaurantClick)
            feature != null
        }
        val cameraListener = MapLibreMap.OnCameraIdleListener {
            currentMap.cameraPosition.toMapTarget()?.let(currentOnCameraChanged)
        }
        currentMap.addOnMapClickListener(clickListener)
        currentMap.addOnCameraIdleListener(cameraListener)
        onDispose {
            currentMap.cameraPosition.toMapTarget()?.let(currentOnCameraChanged)
            currentMap.removeOnMapClickListener(clickListener)
            currentMap.removeOnCameraIdleListener(cameraListener)
        }
    }

    LaunchedEffect(markerStyle, deviceLocation) {
        val features = deviceLocation?.let {
            listOf(Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)))
        }.orEmpty()
        markerStyle?.getSourceAs<GeoJsonSource>(LOCATION_SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    LaunchedEffect(map, markerStyle, cameraTarget, cameraRequestId) {
        val target = cameraTarget ?: return@LaunchedEffect
        if (markerStyle == null) return@LaunchedEffect
        map?.animateCamera(
            CameraUpdateFactory.newCameraPosition(target.toCameraPosition()),
            700,
        )
    }
}

private fun addMarkerAssets(style: Style, density: Float) {
    style.addSource(GeoJsonSource(LOCATION_SOURCE_ID, FeatureCollection.fromFeatures(emptyList<Feature>())))
    style.addLayer(
        CircleLayer("resto-device-location", LOCATION_SOURCE_ID).withProperties(
            circleRadius(7f), circleColor("#3275AC"),
            circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f),
        ),
    )
    (1..10).forEach { rating ->
        style.addImage(
            markerIconId(rating),
            createMarkerBitmap(
                color = ratingColor(rating).toArgb(),
                density = density,
            ),
        )
    }
    style.addImage(
        WISHLIST_ICON_ID,
        createMarkerBitmap(
            color = ratingColor(null).toArgb(),
            density = density,
            isWishlist = true,
        ),
    )
    style.addSource(
        GeoJsonSource(
            MARKER_SOURCE_ID,
            FeatureCollection.fromFeatures(emptyList<Feature>()),
        ),
    )
    style.addLayer(
        SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
            iconImage(get(ICON_ID_PROPERTY)),
            iconAnchor(ICON_ANCHOR_BOTTOM),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
}

private fun List<Restaurant>.toFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    mapNotNull { restaurant ->
        val latitude = restaurant.latitude ?: return@mapNotNull null
        val longitude = restaurant.longitude ?: return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
            addStringProperty(RESTAURANT_ID_PROPERTY, restaurant.id)
            addStringProperty(
                ICON_ID_PROPERTY,
                restaurant.ratingLevel?.let(::markerIconId) ?: WISHLIST_ICON_ID,
            )
        }
    },
)

private fun markerIconId(rating: Int): String = "resto-rating-$rating"

private val PARIS_MAP_TARGET = MapTarget(48.8566, 2.3522, 11.5)

private fun MapTarget.toCameraPosition(): CameraPosition = CameraPosition.Builder()
    .target(LatLng(latitude, longitude))
    .zoom(zoom)
    .bearing(bearing)
    .tilt(tilt)
    .build()

private fun CameraPosition.toMapTarget(): MapTarget? = target?.let { center ->
    MapTarget(
        latitude = center.latitude,
        longitude = center.longitude,
        zoom = zoom,
        bearing = bearing,
        tilt = tilt,
    )
}

private fun createMarkerBitmap(
    color: Int,
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
        // Apply opacity once to the complete pin: overlapping circle/pointer must not darken the join.
        val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), Color.alpha(color))
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.rgb(Color.red(color), Color.green(color), Color.blue(color))
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
        canvas.restoreToCount(layer)
    }
}

private const val MARKER_SOURCE_ID = "resto-restaurants-source"
private const val LOCATION_SOURCE_ID = "resto-device-location-source"
private const val MARKER_LAYER_ID = "resto-restaurants-layer"
private const val RESTAURANT_ID_PROPERTY = "restaurantId"
private const val ICON_ID_PROPERTY = "iconId"
private const val WISHLIST_ICON_ID = "resto-wishlist"
