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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RestoViewModel(
    private val repository: RestaurantRepository,
    private val backupRepository: BackupRepository,
    private val geoapifyService: GeoapifyService,
) : ViewModel() {
    val isGeoapifyConfigured: Boolean = geoapifyService.isConfigured

    val restaurants: StateFlow<RestaurantsUiState> = repository
        .observeRestaurants()
        .map<List<Restaurant>, RestaurantsUiState> { RestaurantsUiState.Ready(it) }
        .catch {
            emit(RestaurantsUiState.Error("Impossible de charger le journal."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RestaurantsUiState.Loading,
        )

    suspend fun createWishlistRestaurant(
        draft: RestaurantDraft,
        note: String?,
    ): Result<String> = runSuspendCatching {
        repository.createWishlistRestaurant(draft, note)
    }

    suspend fun createVisitedRestaurant(
        draft: RestaurantDraft,
        visit: VisitDraft,
    ): Result<String> = runSuspendCatching {
        repository.createVisitedRestaurant(draft, visit)
    }

    suspend fun addVisit(
        restaurantId: String,
        visit: VisitDraft,
    ): Result<Unit> = runSuspendCatching {
        repository.addVisit(restaurantId, visit)
    }

    suspend fun updateRestaurant(
        restaurantId: String,
        draft: RestaurantDraft,
        wishlistNote: String?,
    ): Result<Unit> = runSuspendCatching {
        repository.updateRestaurant(restaurantId, draft, wishlistNote)
    }

    suspend fun updateVisit(
        visitId: String,
        visit: VisitDraft,
        keptPhotoIds: Set<String>,
    ): Result<Unit> = runSuspendCatching {
        repository.updateVisit(visitId, visit, keptPhotoIds)
    }

    suspend fun removeFromWishlist(restaurantId: String): Result<Unit> = runSuspendCatching {
        repository.removeFromWishlist(restaurantId)
    }

    suspend fun deleteVisit(visitId: String): Result<Unit> = runSuspendCatching {
        repository.deleteVisit(visitId)
    }

    suspend fun deleteRestaurant(restaurantId: String): Result<Unit> = runSuspendCatching {
        repository.deleteRestaurant(restaurantId)
    }

    suspend fun searchPlaces(query: String): Result<List<GeoapifySuggestion>> = runSuspendCatching {
        geoapifyService.autocomplete(query)
    }

    suspend fun exportBackup(uri: Uri): Result<Unit> = runSuspendCatching {
        backupRepository.exportTo(uri)
    }

    suspend fun restoreBackup(uri: Uri): Result<Unit> = runSuspendCatching {
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

sealed interface RestaurantsUiState {
    data object Loading : RestaurantsUiState
    data class Ready(val restaurants: List<Restaurant>) : RestaurantsUiState
    data class Error(val message: String) : RestaurantsUiState
}

internal suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}
