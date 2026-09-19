package fr.martinrocca.resto.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import fr.martinrocca.resto.data.local.entity.DishEntity
import fr.martinrocca.resto.data.local.entity.PhotoEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity

data class VisitAggregate(
    @Embedded val visit: VisitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "visitId",
    )
    val dishes: List<DishEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "visitId",
    )
    val photos: List<PhotoEntity>,
)
