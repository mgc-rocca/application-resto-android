package fr.martinrocca.resto.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dishes",
    foreignKeys = [
        ForeignKey(
            entity = VisitEntity::class,
            parentColumns = ["id"],
            childColumns = ["visitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("visitId")],
)
data class DishEntity(
    @PrimaryKey val id: String,
    val visitId: String,
    val name: String,
    val priceCents: Long?,
    val currency: String,
    val sortOrder: Int,
)
