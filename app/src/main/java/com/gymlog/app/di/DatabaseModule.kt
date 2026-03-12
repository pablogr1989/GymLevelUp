package com.gymlog.app.di

import android.content.Context
import androidx.annotation.StringRes
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymlog.app.R
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
    fun provideDatabase(@ApplicationContext context: Context): GymLogDatabase {
        return Room.databaseBuilder(context, GymLogDatabase::class.java, "gymlevelup_database_v2")
            .addMigrations(GymLogDatabase.MIGRATION_3_4, GymLogDatabase.MIGRATION_4_5, GymLogDatabase.MIGRATION_5_6, GymLogDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val database = Room.databaseBuilder(context, GymLogDatabase::class.java, "gymlevelup_database_v2")
                            .addMigrations(GymLogDatabase.MIGRATION_3_4, GymLogDatabase.MIGRATION_4_5, GymLogDatabase.MIGRATION_5_6, GymLogDatabase.MIGRATION_6_7)
                            .build()
                        prepopulateDatabase(database, context)
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
    fun provideDetailedHistoryDao(database: GymLogDatabase): DetailedHistoryDao = database.detailedHistoryDao()

    @Provides
    @Singleton
    fun provideImageStorageHelper(@ApplicationContext context: Context): ImageStorageHelper = ImageStorageHelper(context)

    private suspend fun prepopulateDatabase(database: GymLogDatabase, context: Context) {
        val count = database.exerciseDao().getExerciseCount()
        if (count > 0) return
        val exerciseDao = database.exerciseDao()
        val setDao = database.setDao()

        data class SampleData(
            val id: String, @StringRes val nameRes: Int, val group: MuscleGroup, @StringRes val descRes: Int,
            val series: Int, val minReps: Int, val maxReps: Int, val weight: Float, val minRir: Int? = null, val maxRir: Int? = null
        )

        val samples = listOf(
            SampleData("ex_sentadilla_barra", R.string.app_name, MuscleGroup.LEGS, R.string.app_name, 3, 10, 10, 60f, 1, 2),
            SampleData("ex_press_banca", R.string.app_name, MuscleGroup.CHEST, R.string.app_name, 4, 10, 10, 40f, 2, 2),
            SampleData("ex_jalon_polea", R.string.app_name, MuscleGroup.BACK, R.string.app_name, 3, 12, 12, 50f, null, null)
        )

        database.withTransaction {
            samples.forEach { sample ->
                val nameResolved = context.getString(sample.nameRes)
                val descResolved = context.getString(sample.descRes)

                exerciseDao.insertExercise(
                    ExerciseEntity(sample.id, if (nameResolved == "GymLevelUp") "Sentadilla (Ejemplo)" else nameResolved, descResolved, sample.group, null, "", "", System.currentTimeMillis())
                )
                setDao.insertSet(
                    SetEntity(UUID.randomUUID().toString(), sample.id, sample.series, sample.minReps, sample.maxReps, sample.weight, sample.minRir, sample.maxRir)
                )
            }
        }
    }
}