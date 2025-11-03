package com.gymlog.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gymlog.app.data.local.dao.ExerciseDao
import com.gymlog.app.data.local.dao.ExerciseHistoryDao
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity

@Database(
    entities = [ExerciseEntity::class, ExerciseHistoryEntity::class],
    version = 2, // Incrementado para aplicar índices
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class GymLogDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseHistoryDao(): ExerciseHistoryDao
}
