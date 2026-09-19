package fr.martinrocca.resto.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.martinrocca.resto.domain.model.MichelinStatus

@Entity(
    tableName = "restaurants",
    indices = [Index(value = ["geoapifyPlaceId"], unique = true)],
)
data class RestaurantEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val geoapifyPlaceId: String?,
    val michelinStatus: MichelinStatus,
    val createdAt: Long,
    val updatedAt: Long,
)
