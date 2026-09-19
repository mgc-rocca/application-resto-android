package fr.martinrocca.resto.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.martinrocca.resto.navigation.AppDestination
import fr.martinrocca.resto.navigation.RestoBottomBar
import fr.martinrocca.resto.navigation.Routes
import fr.martinrocca.resto.domain.model.tagEquivalenceKey
import fr.martinrocca.resto.domain.model.MichelinStatus
import fr.martinrocca.resto.ui.add.AddRestaurantScreen
import fr.martinrocca.resto.ui.add.AddVisitScreen
import fr.martinrocca.resto.ui.edit.EditRestaurantScreen
import fr.martinrocca.resto.ui.edit.EditVisitScreen
import fr.martinrocca.resto.ui.journal.JournalScreen
import fr.martinrocca.resto.ui.map.MapScreen
import fr.martinrocca.resto.ui.restaurant.RestaurantScreen
import fr.martinrocca.resto.ui.stats.StatsScreen
import fr.martinrocca.resto.ui.wishlist.WishlistScreen

@Composable
fun RestoApp(
    viewModel: RestoViewModel,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    var pendingJournalMichelin by rememberSaveable { mutableStateOf<String?>(null) }
    val restaurantsState by viewModel.restaurants.collectAsStateWithLifecycle()
    if (restaurantsState is RestaurantsUiState.Loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (restaurantsState is RestaurantsUiState.Error) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = (restaurantsState as RestaurantsUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }
    val restaurants = (restaurantsState as RestaurantsUiState.Ready).restaurants
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in AppDestination.topLevel.map { it.route }
    val knownTags = remember(restaurants) {
        restaurants
            .flatMap { restaurant -> restaurant.tags.map { it.name } }
            .groupBy(::tagEquivalenceKey)
            .values
            .map { equivalentNames ->
                equivalentNames
                    .groupingBy { it }
                    .eachCount()
                    .maxBy { it.value }
                    .key
            }
            .sortedBy(String::lowercase)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                RestoBottomBar(
                    currentRoute = currentRoute,
                    onDestinationClick = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onAddClick = { navController.navigate(Routes.ADD_RESTAURANT) },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Journal.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(AppDestination.Journal.route) {
                JournalScreen(
                    requestedMichelin = pendingJournalMichelin?.let(MichelinStatus::valueOf),
                    onFilterApplied = { pendingJournalMichelin = null },
                    restaurants = restaurants,
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    contentPadding = contentPadding,
                )
            }
            composable(AppDestination.Map.route) {
                MapScreen(
                    restaurants = restaurants,
                    viewModel = viewModel,
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    contentPadding = contentPadding,
                )
            }
            composable(AppDestination.Wishlist.route) {
                WishlistScreen(
                    restaurants = restaurants,
                    onRestaurantClick = { navController.navigate(Routes.restaurant(it)) },
                    onAddVisit = { navController.navigate(Routes.addVisit(it)) },
                    contentPadding = contentPadding,
                )
            }
            composable(AppDestination.Stats.route) {
                StatsScreen(
                    onMichelinClick = { status ->
                        pendingJournalMichelin = status.name
                        navController.navigate(AppDestination.Journal.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    restaurants = restaurants,
                    viewModel = viewModel,
                    contentPadding = contentPadding,
                )
            }
            composable(Routes.ADD_RESTAURANT) {
                AddRestaurantScreen(
                    viewModel = viewModel,
                    knownTags = knownTags,
                    onBack = { navController.popBackStack() },
                    onSaved = { restaurantId ->
                        navController.navigate(Routes.restaurant(restaurantId)) {
                            popUpTo(Routes.ADD_RESTAURANT) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.RESTAURANT) { entry ->
                val restaurantId = entry.arguments?.getString("restaurantId")
                val restaurant = restaurants.firstOrNull { it.id == restaurantId }
                RestaurantScreen(
                    knownTags = knownTags,
                    restaurant = restaurant,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddVisit = { navController.navigate(Routes.addVisit(it)) },
                    onEditRestaurant = { navController.navigate(Routes.editRestaurant(it)) },
                    onEditVisit = { restaurantId, visitId ->
                        navController.navigate(Routes.editVisit(restaurantId, visitId))
                    },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD_VISIT) { entry ->
                val restaurantId = entry.arguments?.getString("restaurantId")
                val restaurant = restaurants.firstOrNull { it.id == restaurantId }
                AddVisitScreen(
                    restaurant = restaurant,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.EDIT_RESTAURANT) { entry ->
                val restaurantId = entry.arguments?.getString("restaurantId")
                val restaurant = restaurants.firstOrNull { it.id == restaurantId }
                EditRestaurantScreen(
                    restaurant = restaurant,
                    viewModel = viewModel,
                    knownTags = knownTags,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.EDIT_VISIT) { entry ->
                val restaurantId = entry.arguments?.getString("restaurantId")
                val visitId = entry.arguments?.getString("visitId")
                val restaurant = restaurants.firstOrNull { it.id == restaurantId }
                val visit = restaurant?.visits?.firstOrNull { it.id == visitId }
                EditVisitScreen(
                    restaurant = restaurant,
                    visit = visit,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }
    }
}
