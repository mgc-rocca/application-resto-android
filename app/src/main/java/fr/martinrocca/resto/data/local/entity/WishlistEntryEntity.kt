package fr.martinrocca.resto.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishlist_entries",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WishlistEntryEntity(
    @PrimaryKey val restaurantId: String,
    val addedAt: Long,
    val note: String?,
)
