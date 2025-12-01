@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
@Entity(
    tableName = "exercise_history",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
@Serializable
data class ExerciseHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val series: Int,
    val reps: Int,
    val weightKg: Float
)
