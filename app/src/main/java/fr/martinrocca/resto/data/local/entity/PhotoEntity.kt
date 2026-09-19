package fr.martinrocca.resto.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photos",
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
data class PhotoEntity(
    @PrimaryKey val id: String,
    val visitId: String,
    val relativePath: String,
    val mimeType: String?,
    val sortOrder: Int,
    val createdAt: Long,
)
