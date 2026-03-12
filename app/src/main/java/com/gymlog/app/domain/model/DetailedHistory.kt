package com.gymlog.app.domain.model

import java.util.UUID

data class DetailedHistory(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val setId: String,
    val daySlotId: String,
    val timestamp: Long,
    val seriesNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val notes: String = "",
    val rir: Int? = null
)