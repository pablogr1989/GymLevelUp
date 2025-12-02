@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["name"]),
        Index(value = ["muscleGroup"]),
        Index(value = ["createdAt"])
    ]
)
@Serializable
data class ExerciseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val muscleGroup: MuscleGroup,
    val imageUri: String? = null,
    val notes: String = "",
    val changeLogText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

enum class MuscleGroup(val displayName: String) {
    LEGS("Piernas"),
    GLUTES("Gluteos"),
    BACK("Espalda"),
    CHEST("Torso"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    SHOULDERS("Hombros");

    companion object {
        fun fromDisplayName(name: String): MuscleGroup? {
            return values().find { it.displayName == name }
        }
    }
}