package com.gymlog.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.dao.*
import com.gymlog.app.data.local.entity.*
import com.gymlog.app.util.ImageStorageHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GymLogDatabase {
        return Room.databaseBuilder(
            context,
            GymLogDatabase::class.java,
            "gymlevelup_database_v2" // Cambio de nombre para forzar DB nueva limpia
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val database = Room.databaseBuilder(
                            context,
                            GymLogDatabase::class.java,
                            "gymlevelup_database_v2"
                        ).build()
                        prepopulateDatabase(database)
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideExerciseDao(database: GymLogDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideExerciseHistoryDao(database: GymLogDatabase): ExerciseHistoryDao = database.exerciseHistoryDao()

    @Provides
    fun provideCalendarDao(database: GymLogDatabase): CalendarDao = database.calendarDao()

    @Provides
    fun provideMonthDao(database: GymLogDatabase): MonthDao = database.monthDao()

    @Provides
    fun provideWeekDao(database: GymLogDatabase): WeekDao = database.weekDao()

    @Provides
    fun provideDaySlotDao(database: GymLogDatabase): DaySlotDao = database.daySlotDao()

    @Provides
    fun provideSetDao(database: GymLogDatabase): SetDao = database.setDao()

    @Provides
    @Singleton
    fun provideImageStorageHelper(@ApplicationContext context: Context): ImageStorageHelper = ImageStorageHelper(context)

    private suspend fun prepopulateDatabase(database: GymLogDatabase) {
        val count = database.exerciseDao().getExerciseCount()
        if (count > 0) return

        val exerciseDao = database.exerciseDao()
        val setDao = database.setDao()

        // Helper data class para prepoblación
        data class SampleData(
            val id: String, val name: String, val group: MuscleGroup, val desc: String,
            val series: Int, val reps: Int, val weight: Float
        )

        val samples = listOf(
            SampleData("ex_sentadilla_barra", "Sentadilla con barra", MuscleGroup.LEGS, "Compuesto clásico", 3, 10, 60f),
            SampleData("ex_press_banca", "Press banca barra", MuscleGroup.CHEST, "Compuesto pecho", 4, 10, 40f),
            SampleData("ex_jalon_polea", "Jalón al pecho", MuscleGroup.BACK, "Dorsales", 3, 12, 50f)
        )

        database.withTransaction {
            samples.forEach { sample ->
                exerciseDao.insertExercise(
                    ExerciseEntity(
                        id = sample.id,
                        name = sample.name,
                        description = sample.desc,
                        muscleGroup = sample.group,
                        createdAt = System.currentTimeMillis()
                    )
                )

                // Crear Set inicial
                setDao.insertSet(
                    SetEntity(
                        id = UUID.randomUUID().toString(),
                        exerciseId = sample.id,
                        series = sample.series,
                        reps = sample.reps,
                        weightKg = sample.weight
                    )
                )
            }
        }
    }
}