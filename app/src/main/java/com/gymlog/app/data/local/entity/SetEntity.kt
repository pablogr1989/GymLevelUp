@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "sets",
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
data class SetEntity(
    @PrimaryKey
    val id: String,
    val exerciseId: String,
    val series: Int,
    val reps: Int,
    val weightKg: Float
)