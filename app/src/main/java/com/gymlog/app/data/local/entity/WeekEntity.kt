@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
@Entity(
    tableName = "weeks",
    foreignKeys = [
        ForeignKey(
            entity = MonthEntity::class,
            parentColumns = ["id"],
            childColumns = ["monthId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("monthId"), Index("weekNumber")]
)
@Serializable
data class WeekEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val monthId: String,
    val weekNumber: Int  // 1, 2, 3, 4
)
