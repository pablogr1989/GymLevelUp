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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GymLogDatabase {
        return Room.databaseBuilder(
            context,
            GymLogDatabase::class.java,
            "gymlevelup_database_v2"
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
    @Singleton
    fun provideImageStorageHelper(@ApplicationContext context: Context): ImageStorageHelper = ImageStorageHelper(context)

    private suspend fun prepopulateDatabase(database: GymLogDatabase, context: Context) {
        val count = database.exerciseDao().getExerciseCount()
        if (count > 0) return

        val exerciseDao = database.exerciseDao()
        val setDao = database.setDao()

        // Helper data class para prepoblación usando Recursos
        data class SampleData(
            val id: String,
            @StringRes val nameRes: Int,
            val group: MuscleGroup,
            @StringRes val descRes: Int,
            val series: Int,
            val reps: Int,
            val weight: Float
        )

        // Nota: Asegúrate de tener estos strings en strings.xml o usa strings temporales si prefieres.
        // He usado R.string.app_name como placeholder seguro si alguno falta,
        // pero lo ideal es definir R.string.sample_ex_squat, etc.
        val samples = listOf(
            SampleData(
                "ex_sentadilla_barra",
                R.string.app_name, // Placeholder: Debería ser R.string.sample_squat_name
                MuscleGroup.LEGS,
                R.string.app_name, // Placeholder: Debería ser R.string.sample_squat_desc
                3, 10, 60f
            ),
            SampleData(
                "ex_press_banca",
                R.string.app_name, // Placeholder
                MuscleGroup.CHEST,
                R.string.app_name, // Placeholder
                4, 10, 40f
            ),
            SampleData(
                "ex_jalon_polea",
                R.string.app_name, // Placeholder
                MuscleGroup.BACK,
                R.string.app_name, // Placeholder
                3, 12, 50f
            )
        )

        database.withTransaction {
            samples.forEach { sample ->
                // Resolvemos el string usando el contexto
                // Si aún no has creado los strings específicos en strings.xml,
                // puedes cambiar 'context.getString(sample.nameRes)' por un string literal temporalmente.
                val nameResolved = context.getString(sample.nameRes)
                val descResolved = context.getString(sample.descRes)

                exerciseDao.insertExercise(
                    ExerciseEntity(
                        id = sample.id,
                        name = if (nameResolved == "GymLevelUp") "Sentadilla (Ejemplo)" else nameResolved, // Fallback logic simple
                        description = descResolved,
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