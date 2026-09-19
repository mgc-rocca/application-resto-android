package fr.martinrocca.resto.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.martinrocca.resto.data.local.dao.RestoDao
import fr.martinrocca.resto.data.local.entity.DishEntity
import fr.martinrocca.resto.data.local.entity.PhotoEntity
import fr.martinrocca.resto.data.local.entity.RestaurantEntity
import fr.martinrocca.resto.data.local.entity.RestaurantTagEntity
import fr.martinrocca.resto.data.local.entity.TagEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity
import fr.martinrocca.resto.data.local.entity.WishlistEntryEntity

@Database(
    entities = [
        RestaurantEntity::class,
        VisitEntity::class,
        TagEntity::class,
        RestaurantTagEntity::class,
        DishEntity::class,
        PhotoEntity::class,
        WishlistEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class RestoDatabase : RoomDatabase() {
    abstract fun restoDao(): RestoDao

    companion object {
        fun create(context: Context): RestoDatabase = Room.databaseBuilder(
            context.applicationContext,
            RestoDatabase::class.java,
            "resto.db",
        ).build()
    }
}
