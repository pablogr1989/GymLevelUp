package com.gymlog.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymlog.app.data.local.dao.*
import com.gymlog.app.data.local.entity.*

@Database(
    entities = [
        ExerciseEntity::class,
        ExerciseHistoryEntity::class,
        CalendarEntity::class,
        MonthEntity::class,
        WeekEntity::class,
        DaySlotEntity::class,
        SetEntity::class // Nueva entidad
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GymLogDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseHistoryDao(): ExerciseHistoryDao
    abstract fun calendarDao(): CalendarDao
    abstract fun monthDao(): MonthDao
    abstract fun weekDao(): WeekDao
    abstract fun daySlotDao(): DaySlotDao
    abstract fun setDao(): SetDao
}