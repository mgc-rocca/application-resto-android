package fr.martinrocca.resto.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visits",
    foreignKeys = [
        ForeignKey(
            entity = RestaurantEntity::class,
            parentColumns = ["id"],
            childColumns = ["restaurantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("restaurantId"), Index("date")],
)
data class VisitEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val date: String,
    val overallRating: Int,
    val foodRating: Int?,
    val serviceRating: Int?,
    val settingRating: Int?,
    val priceRating: Int?,
    val comment: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
