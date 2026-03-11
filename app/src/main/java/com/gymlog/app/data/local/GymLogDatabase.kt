package com.gymlog.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        SetEntity::class,
        DaySlotExerciseCrossRef::class
    ],
    version = 5, // AUMENTAMOS VERSIÓN A 5
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

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sets_new` (
                        `id` TEXT NOT NULL, 
                        `exerciseId` TEXT NOT NULL, 
                        `series` INTEGER NOT NULL, 
                        `minReps` INTEGER NOT NULL, 
                        `maxReps` INTEGER NOT NULL, 
                        `weightKg` REAL NOT NULL, 
                        PRIMARY KEY(`id`), 
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO sets_new (id, exerciseId, series, minReps, maxReps, weightKg)
                    SELECT id, exerciseId, series, reps, reps, weightKg FROM sets
                """.trimIndent())

                db.execSQL("DROP TABLE sets")
                db.execSQL("ALTER TABLE sets_new RENAME TO sets")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sets_exerciseId` ON `sets` (`exerciseId`)")
            }
        }

        // NUEVA MIGRACIÓN: Añadir columnas de RIR permitiendo valores nulos
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sets ADD COLUMN minRir INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE sets ADD COLUMN maxRir INTEGER DEFAULT NULL")
            }
        }
    }
}