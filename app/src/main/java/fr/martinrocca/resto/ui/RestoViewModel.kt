package fr.martinrocca.resto.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.martinrocca.resto.data.backup.BackupRepository
import fr.martinrocca.resto.data.remote.geoapify.GeoapifyService
import fr.martinrocca.resto.data.remote.geoapify.GeoapifySuggestion
import fr.martinrocca.resto.data.repository.RestaurantRepository
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.RestaurantDraft
import fr.martinrocca.resto.domain.model.VisitDraft
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RestoViewModel(
    private val repository: RestaurantRepository,
    private val backupRepository: BackupRepository,
    private val geoapifyService: GeoapifyService,
) : ViewModel() {
    val isGeoapifyConfigured: Boolean = geoapifyService.isConfigured

    val restaurants: StateFlow<List<Restaurant>> = repository
        .observeRestaurants()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    suspend fun createWishlistRestaurant(
        draft: RestaurantDraft,
        note: String?,
    ): Result<String> = runCatching {
        repository.createWishlistRestaurant(draft, note)
    }

    suspend fun createVisitedRestaurant(
        draft: RestaurantDraft,
        visit: VisitDraft,
    ): Result<String> = runCatching {
        repository.createVisitedRestaurant(draft, visit)
    }

    suspend fun addVisit(
        restaurantId: String,
        visit: VisitDraft,
    ): Result<Unit> = runCatching {
        repository.addVisit(restaurantId, visit)
    }

    suspend fun updateRestaurant(
        restaurantId: String,
        draft: RestaurantDraft,
        wishlistNote: String?,
    ): Result<Unit> = runCatching {
        repository.updateRestaurant(restaurantId, draft, wishlistNote)
    }

    suspend fun updateVisit(
        visitId: String,
        visit: VisitDraft,
        keptPhotoIds: Set<String>,
    ): Result<Unit> = runCatching {
        repository.updateVisit(visitId, visit, keptPhotoIds)
    }

    suspend fun removeFromWishlist(restaurantId: String): Result<Unit> = runCatching {
        repository.removeFromWishlist(restaurantId)
    }

    suspend fun deleteVisit(visitId: String): Result<Unit> = runCatching {
        repository.deleteVisit(visitId)
    }

    suspend fun deleteRestaurant(restaurantId: String): Result<Unit> = runCatching {
        repository.deleteRestaurant(restaurantId)
    }

    suspend fun searchPlaces(query: String): Result<List<GeoapifySuggestion>> = runCatching {
        geoapifyService.autocomplete(query)
    }

    suspend fun exportBackup(uri: Uri): Result<Unit> = runCatching {
        backupRepository.exportTo(uri)
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> = runCatching {
        backupRepository.restoreFrom(uri)
    }

    class Factory(
        private val repository: RestaurantRepository,
        private val backupRepository: BackupRepository,
        private val geoapifyService: GeoapifyService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(RestoViewModel::class.java))
            return RestoViewModel(repository, backupRepository, geoapifyService) as T
        }
    }
}
