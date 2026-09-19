package fr.martinrocca.resto.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.martinrocca.resto.data.local.entity.DishEntity
import fr.martinrocca.resto.data.local.entity.PhotoEntity
import fr.martinrocca.resto.data.local.entity.RestaurantEntity
import fr.martinrocca.resto.data.local.entity.RestaurantTagEntity
import fr.martinrocca.resto.data.local.entity.TagEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity
import fr.martinrocca.resto.data.local.entity.WishlistEntryEntity
import fr.martinrocca.resto.data.local.relation.RestaurantAggregate
import kotlinx.coroutines.flow.Flow

@Dao
interface RestoDao {
    @Transaction
    @Query("SELECT * FROM restaurants ORDER BY name COLLATE NOCASE")
    fun observeRestaurants(): Flow<List<RestaurantAggregate>>

    @Transaction
    @Query("SELECT * FROM restaurants WHERE id = :restaurantId LIMIT 1")
    fun observeRestaurant(restaurantId: String): Flow<RestaurantAggregate?>

    @Query("SELECT * FROM restaurants WHERE id = :restaurantId LIMIT 1")
    suspend fun findRestaurant(restaurantId: String): RestaurantEntity?

    @Query("SELECT * FROM restaurants WHERE geoapifyPlaceId = :placeId LIMIT 1")
    suspend fun findRestaurantByGeoapifyId(placeId: String): RestaurantEntity?

    @Query("SELECT * FROM visits WHERE id = :visitId LIMIT 1")
    suspend fun findVisit(visitId: String): VisitEntity?

    @Query("SELECT COUNT(*) FROM visits WHERE restaurantId = :restaurantId")
    suspend fun countVisitsForRestaurant(restaurantId: String): Int

    @Query("SELECT * FROM wishlist_entries WHERE restaurantId = :restaurantId LIMIT 1")
    suspend fun findWishlistEntry(restaurantId: String): WishlistEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurant(restaurant: RestaurantEntity)

    @Update
    suspend fun updateRestaurant(restaurant: RestaurantEntity)

    @Delete
    suspend fun deleteRestaurant(restaurant: RestaurantEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisit(visit: VisitEntity)

    @Update
    suspend fun updateVisit(visit: VisitEntity)

    @Query("DELETE FROM visits WHERE id = :visitId")
    suspend fun deleteVisit(visitId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findTag(normalizedName: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRestaurantTag(link: RestaurantTagEntity)

    @Query("DELETE FROM restaurant_tags WHERE restaurantId = :restaurantId")
    suspend fun deleteRestaurantTags(restaurantId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWishlistEntry(entry: WishlistEntryEntity)

    @Query("DELETE FROM wishlist_entries WHERE restaurantId = :restaurantId")
    suspend fun deleteWishlistEntry(restaurantId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDishes(dishes: List<DishEntity>)

    @Query("DELETE FROM dishes WHERE visitId = :visitId")
    suspend fun deleteDishesForVisit(visitId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE visitId = :visitId")
    suspend fun deletePhotosForVisit(visitId: String)

    @Query("SELECT * FROM photos WHERE visitId = :visitId")
    suspend fun getPhotosForVisit(visitId: String): List<PhotoEntity>

    @Query(
        """
        SELECT photos.* FROM photos
        INNER JOIN visits ON visits.id = photos.visitId
        WHERE visits.restaurantId = :restaurantId
        """,
    )
    suspend fun getPhotosForRestaurant(restaurantId: String): List<PhotoEntity>

    @Query("SELECT * FROM restaurants ORDER BY id")
    suspend fun getAllRestaurants(): List<RestaurantEntity>

    @Query("SELECT * FROM visits ORDER BY id")
    suspend fun getAllVisits(): List<VisitEntity>

    @Query("SELECT * FROM tags ORDER BY id")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM restaurant_tags ORDER BY restaurantId, tagId")
    suspend fun getAllRestaurantTags(): List<RestaurantTagEntity>

    @Query("SELECT * FROM wishlist_entries ORDER BY restaurantId")
    suspend fun getAllWishlistEntries(): List<WishlistEntryEntity>

    @Query("SELECT * FROM dishes ORDER BY id")
    suspend fun getAllDishes(): List<DishEntity>

    @Query("SELECT * FROM photos ORDER BY id")
    suspend fun getAllPhotos(): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurants(restaurants: List<RestaurantEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVisits(visits: List<VisitEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRestaurantTags(links: List<RestaurantTagEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWishlistEntries(entries: List<WishlistEntryEntity>)

    @Query("DELETE FROM restaurants")
    suspend fun clearRestaurants()

    @Query("DELETE FROM tags")
    suspend fun clearTags()
}
