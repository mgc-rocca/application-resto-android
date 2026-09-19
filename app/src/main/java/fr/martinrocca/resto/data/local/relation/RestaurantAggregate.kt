package fr.martinrocca.resto.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import fr.martinrocca.resto.data.local.entity.RestaurantEntity
import fr.martinrocca.resto.data.local.entity.RestaurantTagEntity
import fr.martinrocca.resto.data.local.entity.TagEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity
import fr.martinrocca.resto.data.local.entity.WishlistEntryEntity

data class RestaurantAggregate(
    @Embedded val restaurant: RestaurantEntity,
    @Relation(
        entity = VisitEntity::class,
        parentColumn = "id",
        entityColumn = "restaurantId",
    )
    val visits: List<VisitAggregate>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RestaurantTagEntity::class,
            parentColumn = "restaurantId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "restaurantId",
    )
    val wishlistEntries: List<WishlistEntryEntity>,
)
