package fr.martinrocca.resto.data.repository

import androidx.room.withTransaction
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.local.entity.PhotoEntity
import fr.martinrocca.resto.data.local.entity.RestaurantEntity
import fr.martinrocca.resto.data.local.entity.RestaurantTagEntity
import fr.martinrocca.resto.data.local.entity.TagEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity
import fr.martinrocca.resto.data.local.entity.WishlistEntryEntity
import fr.martinrocca.resto.data.local.relation.RestaurantAggregate
import fr.martinrocca.resto.data.local.relation.VisitAggregate
import fr.martinrocca.resto.data.photo.ImportedPhoto
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.domain.model.Dish
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.RestaurantDraft
import fr.martinrocca.resto.domain.model.Tag
import fr.martinrocca.resto.domain.model.Visit
import fr.martinrocca.resto.domain.model.VisitDraft
import fr.martinrocca.resto.domain.model.WishlistEntry
import fr.martinrocca.resto.domain.model.canonicalTagName
import fr.martinrocca.resto.domain.model.normalizeTagName
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RestaurantRepository(
    private val database: RestoDatabase,
    private val photoManager: PhotoManager,
) {
    private val dao = database.restoDao()

    fun observeRestaurants(): Flow<List<Restaurant>> = dao.observeRestaurants().map { rows ->
        rows.map(RestaurantAggregate::toDomain)
    }

    fun observeRestaurant(restaurantId: String): Flow<Restaurant?> =
        dao.observeRestaurant(restaurantId).map { it?.toDomain() }

    suspend fun createWishlistRestaurant(
        draft: RestaurantDraft,
        note: String?,
    ): String = database.withTransaction {
        validateRestaurant(draft)
        ensureGeoapifyIdIsAvailable(draft.geoapifyPlaceId)

        val now = System.currentTimeMillis()
        val restaurantId = UUID.randomUUID().toString()
        dao.insertRestaurant(draft.toEntity(restaurantId, now))
        replaceTags(restaurantId, draft.tags, now)
        dao.upsertWishlistEntry(
            WishlistEntryEntity(
                restaurantId = restaurantId,
                addedAt = now,
                note = note.cleanOrNull(),
            ),
        )
        restaurantId
    }

    suspend fun createVisitedRestaurant(
        draft: RestaurantDraft,
        visit: VisitDraft,
    ): String {
        validateRestaurant(draft)
        validateVisit(visit)
        ensureGeoapifyIdIsAvailable(draft.geoapifyPlaceId)
        val importedPhotos = photoManager.importPhotos(visit.photoUris)
        return try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                val restaurantId = UUID.randomUUID().toString()
                dao.insertRestaurant(draft.toEntity(restaurantId, now))
                replaceTags(restaurantId, draft.tags, now)
                insertVisit(restaurantId, visit, importedPhotos, now)
                restaurantId
            }
        } catch (error: Throwable) {
            photoManager.deletePhotos(importedPhotos.map(ImportedPhoto::relativePath))
            throw error
        }
    }

    suspend fun addVisit(
        restaurantId: String,
        visit: VisitDraft,
    ) {
        validateVisit(visit)
        requireNotNull(dao.findRestaurant(restaurantId)) { "Restaurant introuvable." }
        val importedPhotos = photoManager.importPhotos(visit.photoUris)
        try {
            database.withTransaction {
                insertVisit(restaurantId, visit, importedPhotos, System.currentTimeMillis())
                dao.deleteWishlistEntry(restaurantId)
            }
        } catch (error: Throwable) {
            photoManager.deletePhotos(importedPhotos.map(ImportedPhoto::relativePath))
            throw error
        }
    }

    suspend fun updateRestaurant(
        restaurantId: String,
        draft: RestaurantDraft,
        wishlistNote: String?,
    ) {
        validateRestaurant(draft)
        database.withTransaction {
            val current = requireNotNull(dao.findRestaurant(restaurantId)) {
                "Restaurant introuvable."
            }
            ensureGeoapifyIdIsAvailable(draft.geoapifyPlaceId, restaurantId)
            val now = System.currentTimeMillis()
            dao.updateRestaurant(
                current.copy(
                    name = draft.name.trim(),
                    address = draft.address.trim(),
                    latitude = draft.latitude,
                    longitude = draft.longitude,
                    geoapifyPlaceId = draft.geoapifyPlaceId,
                    michelinStatus = draft.michelinStatus,
                    updatedAt = now,
                ),
            )
            replaceTags(restaurantId, draft.tags, now)
            dao.findWishlistEntry(restaurantId)?.let { wishlist ->
                dao.upsertWishlistEntry(wishlist.copy(note = wishlistNote.cleanOrNull()))
            }
        }
    }

    suspend fun updateVisit(
        visitId: String,
        draft: VisitDraft,
        keptPhotoIds: Set<String>,
    ) {
        validateVisit(draft)
        val currentVisit = requireNotNull(dao.findVisit(visitId)) { "Visite introuvable." }
        val currentPhotos = dao.getPhotosForVisit(visitId)
        require(keptPhotoIds.all { id -> currentPhotos.any { it.id == id } }) {
            "La sélection de photos est invalide."
        }
        val importedPhotos = photoManager.importPhotos(draft.photoUris)
        val photosToKeep = currentPhotos
            .filter { it.id in keptPhotoIds }
            .sortedBy(PhotoEntity::sortOrder)
        val removedPaths = currentPhotos
            .filterNot { it.id in keptPhotoIds }
            .map(PhotoEntity::relativePath)
        try {
            database.withTransaction {
                val now = System.currentTimeMillis()
                dao.updateVisit(
                    currentVisit.copy(
                        date = draft.date.toString(),
                        overallRating = draft.overallRating,
                        foodRating = null,
                        serviceRating = null,
                        settingRating = null,
                        priceRating = null,
                        comment = draft.comment.cleanOrNull(),
                        updatedAt = now,
                    ),
                )
                dao.deleteDishesForVisit(visitId)

                dao.deletePhotosForVisit(visitId)
                val photos = photosToKeep.mapIndexed { index, photo ->
                    photo.copy(sortOrder = index)
                } + importedPhotos.mapIndexed { index, photo ->
                    PhotoEntity(
                        id = UUID.randomUUID().toString(),
                        visitId = visitId,
                        relativePath = photo.relativePath,
                        mimeType = photo.mimeType,
                        sortOrder = photosToKeep.size + index,
                        createdAt = now,
                    )
                }
                if (photos.isNotEmpty()) dao.insertPhotos(photos)

                dao.findRestaurant(currentVisit.restaurantId)?.let { restaurant ->
                    dao.updateRestaurant(restaurant.copy(updatedAt = now))
                }
            }
        } catch (error: Throwable) {
            photoManager.deletePhotos(importedPhotos.map(ImportedPhoto::relativePath))
            throw error
        }
        photoManager.deletePhotos(removedPaths)
    }

    suspend fun removeFromWishlist(restaurantId: String) {
        dao.deleteWishlistEntry(restaurantId)
    }

    suspend fun deleteVisit(visitId: String) {
        val photoPaths = database.withTransaction {
            val paths = dao.getPhotosForVisit(visitId).map(PhotoEntity::relativePath)
            dao.deleteVisit(visitId)
            paths
        }
        photoManager.deletePhotos(photoPaths)
    }

    suspend fun deleteRestaurant(restaurantId: String) {
        val photoPaths = database.withTransaction {
            val paths = dao.getPhotosForRestaurant(restaurantId).map(PhotoEntity::relativePath)
            val restaurant = dao.findRestaurant(restaurantId)
            if (restaurant != null) dao.deleteRestaurant(restaurant)
            paths
        }
        photoManager.deletePhotos(photoPaths)
    }

    private suspend fun ensureGeoapifyIdIsAvailable(
        placeId: String?,
        excludedRestaurantId: String? = null,
    ) {
        val existing = placeId?.let { dao.findRestaurantByGeoapifyId(it) }
        if (existing != null && existing.id != excludedRestaurantId) {
            error("Ce restaurant existe déjà dans votre journal.")
        }
    }

    private suspend fun replaceTags(
        restaurantId: String,
        rawTags: List<String>,
        now: Long,
    ) {
        dao.deleteRestaurantTags(restaurantId)
        val knownTags = dao.getAllTags().toMutableList()
        val linkedTagIds = mutableSetOf<String>()
        rawTags
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach { name ->
                val canonicalName = canonicalTagName(name, knownTags.map(TagEntity::name))
                val normalizedName = normalizeTagName(canonicalName)
                val storedTag = knownTags.firstOrNull {
                    normalizeTagName(it.name) == normalizedName
                } ?: TagEntity(
                    id = UUID.randomUUID().toString(),
                    name = canonicalName.replaceFirstChar { it.titlecase(Locale.FRENCH) },
                    normalizedName = normalizedName,
                    createdAt = now,
                ).also { candidate ->
                    dao.insertTag(candidate)
                    knownTags += candidate
                }
                if (linkedTagIds.add(storedTag.id)) {
                    dao.insertRestaurantTag(
                        RestaurantTagEntity(
                            restaurantId = restaurantId,
                            tagId = storedTag.id,
                        ),
                    )
                }
            }
    }

    private suspend fun insertVisit(
        restaurantId: String,
        draft: VisitDraft,
        importedPhotos: List<ImportedPhoto>,
        now: Long,
    ) {
        val visitId = UUID.randomUUID().toString()
        dao.insertVisit(
            VisitEntity(
                id = visitId,
                restaurantId = restaurantId,
                date = draft.date.toString(),
                overallRating = draft.overallRating,
                foodRating = null,
                serviceRating = null,
                settingRating = null,
                priceRating = null,
                comment = draft.comment.cleanOrNull(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        val photos = importedPhotos.mapIndexed { index, photo ->
            PhotoEntity(
                id = UUID.randomUUID().toString(),
                visitId = visitId,
                relativePath = photo.relativePath,
                mimeType = photo.mimeType,
                sortOrder = index,
                createdAt = now,
            )
        }
        if (photos.isNotEmpty()) dao.insertPhotos(photos)
    }

    private fun validateRestaurant(draft: RestaurantDraft) {
        require(draft.name.isNotBlank()) { "Le nom du restaurant est obligatoire." }
        require(draft.address.isNotBlank()) { "L’adresse du restaurant est obligatoire." }
        require((draft.latitude == null) == (draft.longitude == null)) {
            "Les coordonnées doivent comporter une latitude et une longitude."
        }
    }

    private fun validateVisit(draft: VisitDraft) {
        require(draft.overallRating in 1..10) { "La note globale doit être comprise entre 1 et 10." }
    }
}

private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun RestaurantDraft.toEntity(id: String, now: Long) = RestaurantEntity(
    id = id,
    name = name.trim(),
    address = address.trim(),
    latitude = latitude,
    longitude = longitude,
    geoapifyPlaceId = geoapifyPlaceId,
    michelinStatus = michelinStatus,
    createdAt = now,
    updatedAt = now,
)

private fun RestaurantAggregate.toDomain() = Restaurant(
    id = restaurant.id,
    name = restaurant.name,
    address = restaurant.address,
    latitude = restaurant.latitude,
    longitude = restaurant.longitude,
    geoapifyPlaceId = restaurant.geoapifyPlaceId,
    michelinStatus = restaurant.michelinStatus,
    tags = tags.sortedBy { it.name }.map { Tag(id = it.id, name = it.name) },
    visits = visits.map(VisitAggregate::toDomain).sortedByDescending { it.date },
    wishlist = wishlistEntries.firstOrNull()?.let {
        WishlistEntry(addedAt = it.addedAt, note = it.note)
    },
    createdAt = restaurant.createdAt,
    updatedAt = restaurant.updatedAt,
)

private fun VisitAggregate.toDomain() = Visit(
    id = visit.id,
    restaurantId = visit.restaurantId,
    date = LocalDate.parse(visit.date),
    overallRating = visit.overallRating,
    foodRating = visit.foodRating,
    serviceRating = visit.serviceRating,
    settingRating = visit.settingRating,
    priceRating = visit.priceRating,
    comment = visit.comment,
    dishes = dishes.sortedBy { it.sortOrder }.map {
        Dish(
            id = it.id,
            name = it.name,
            priceCents = it.priceCents,
            currency = it.currency,
            sortOrder = it.sortOrder,
        )
    },
    photos = photos.sortedBy { it.sortOrder }.map {
        fr.martinrocca.resto.domain.model.Photo(
            id = it.id,
            relativePath = it.relativePath,
            mimeType = it.mimeType,
            sortOrder = it.sortOrder,
        )
    },
    createdAt = visit.createdAt,
    updatedAt = visit.updatedAt,
)
