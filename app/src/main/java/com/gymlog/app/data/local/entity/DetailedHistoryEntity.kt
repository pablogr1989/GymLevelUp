package com.gymlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detailed_history")
data class DetailedHistoryEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val setId: String,
    val daySlotId: String,
    val timestamp: Long,
    val seriesNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val notes: String,
    val rir: Int? // NUEVO CAMPO AÑADIDO
)