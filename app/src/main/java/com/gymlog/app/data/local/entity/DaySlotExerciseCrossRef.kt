@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "day_slot_exercise_cross_ref",
    primaryKeys = ["daySlotId", "exerciseId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = DaySlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["daySlotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("daySlotId"),
        Index("exerciseId")
    ]
)
data class DaySlotExerciseCrossRef(
    val daySlotId: String,
    val exerciseId: String,
    val targetSetId: String?,
    val orderIndex: Int
)