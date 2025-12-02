@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.gymlog.app.data.backup

import com.gymlog.app.data.local.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val exercises: List<ExerciseEntity>,
    val sets: List<SetEntity>,
    val history: List<ExerciseHistoryEntity>,
    val calendars: List<CalendarEntity>,
    val months: List<MonthEntity>,
    val weeks: List<WeekEntity>,
    val daySlots: List<DaySlotEntity>
)