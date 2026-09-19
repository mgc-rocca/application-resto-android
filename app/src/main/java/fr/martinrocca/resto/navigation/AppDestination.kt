package fr.martinrocca.resto.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Map
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Journal : AppDestination("journal", "Journal", Icons.AutoMirrored.Outlined.MenuBook)
    data object Map : AppDestination("map", "Carte", Icons.Outlined.Map)
    data object Wishlist : AppDestination("wishlist", "Envies", Icons.Outlined.Bookmarks)
    data object Stats : AppDestination("stats", "Stats", Icons.Outlined.BarChart)

    companion object {
        val topLevel = listOf(Journal, Map, Wishlist, Stats)
    }
}

object Routes {
    const val ADD_RESTAURANT = "add"
    const val RESTAURANT = "restaurant/{restaurantId}"
    const val EDIT_RESTAURANT = "restaurant/{restaurantId}/edit"
    const val ADD_VISIT = "restaurant/{restaurantId}/visit/new"
    const val EDIT_VISIT = "restaurant/{restaurantId}/visit/{visitId}/edit"

    fun restaurant(restaurantId: String) = "restaurant/$restaurantId"
    fun editRestaurant(restaurantId: String) = "restaurant/$restaurantId/edit"
    fun addVisit(restaurantId: String) = "restaurant/$restaurantId/visit/new"
    fun editVisit(restaurantId: String, visitId: String) =
        "restaurant/$restaurantId/visit/$visitId/edit"
}
