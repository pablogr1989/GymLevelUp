@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.backup

import com.gymlog.app.data.local.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val exercises: List<ExerciseEntity> = emptyList(),
    val sets: List<SetEntity> = emptyList(),
    val history: List<ExerciseHistoryEntity> = emptyList(),
    val calendars: List<CalendarEntity> = emptyList(),
    val months: List<MonthEntity> = emptyList(),
    val weeks: List<WeekEntity> = emptyList(),
    val daySlots: List<DaySlotEntity> = emptyList(),
    val daySlotExercises: List<DaySlotExerciseCrossRef> = emptyList()
)