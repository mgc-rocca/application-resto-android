package fr.martinrocca.resto.data.local

import androidx.room.TypeConverter
import fr.martinrocca.resto.domain.model.MichelinStatus

class RoomConverters {
    @TypeConverter
    fun fromMichelinStatus(value: MichelinStatus): String = value.name

    @TypeConverter
    fun toMichelinStatus(value: String): MichelinStatus = MichelinStatus.valueOf(value)
}
