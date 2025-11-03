package com.gymlog.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.dao.ExerciseDao
import com.gymlog.app.data.local.dao.ExerciseHistoryDao
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
            "gymlog_database_v3"
        )
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    android.util.Log.d("GymLogDB", "onCreate called - prepopulating database")
                    // Prepopulate when database is created for the first time
                    CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                        val database = Room.databaseBuilder(
                            context,
                            GymLogDatabase::class.java,
                            "gymlog_database_v3"
                        ).build()
                        android.util.Log.d("GymLogDB", "Starting prepopulation")
                        prepopulateDatabase(database)
                        android.util.Log.d("GymLogDB", "Prepopulation completed")
                    }
                }
            })
            .build()
    }

    @Provides
    fun provideExerciseDao(database: GymLogDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideExerciseHistoryDao(database: GymLogDatabase): ExerciseHistoryDao {
        return database.exerciseHistoryDao()
    }

    private suspend fun prepopulateDatabase(database: GymLogDatabase) {
        // Verificar si ya hay datos para evitar prepoblar múltiples veces
        val count = database.exerciseDao().getExerciseCount()
        if (count > 0) {
            android.util.Log.d("GymLogDB", "Database already has $count exercises, skipping prepopulation")
            return
        }
        
        android.util.Log.d("GymLogDB", "Starting database prepopulation")
        val exerciseDao = database.exerciseDao()
        val historyDao = database.exerciseHistoryDao()

        val sampleExercises = listOf(
            ExerciseEntity(
                id = "ex_prensa_inclinada_discos",
                name = "Prensa inclinada discos",
                description = "Ejercicio principal para cuÃ¡driceps y glÃºteos, trabaja tambiÃ©n femorales.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 150f
            ),
            ExerciseEntity(
                id = "ex_sentadilla_multipower",
                name = "Sentadilla multipower",
                description = "Ejercicio guiado para cuÃ¡driceps, glÃºteos y estabilidad del core.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 40f
            ),
            ExerciseEntity(
                id = "ex_sentadilla_barra",
                name = "Sentadilla con barra",
                description = "Ejercicio compuesto para cuÃ¡driceps, glÃºteos y core.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 70f
            ),
            ExerciseEntity(
                id = "ex_sentadilla_bulgara",
                name = "Sentadilla bÃºlgara",
                description = "Ejercicio unilateral para cuÃ¡driceps, glÃºteos y equilibrio.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 10f
            ),
            ExerciseEntity(
                id = "ex_zancada_multipower",
                name = "Zancada multipower",
                description = "Fortalece cuÃ¡driceps, glÃºteos y estabilidad de cadera.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 35f
            ),
            ExerciseEntity(
                id = "ex_zancada_pasos",
                name = "Zancada con pasos",
                description = "Ejercicio funcional que trabaja cuÃ¡driceps y glÃºteos.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 2,
                currentReps = 12,
                currentWeightKg = 20f
            ),
            ExerciseEntity(
                id = "ex_peso_muerto_barra",
                name = "Peso muerto convencional barra",
                description = "Ejercicio compuesto que trabaja glÃºteos, isquiotibiales y espalda baja.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 30f
            ),
            ExerciseEntity(
                id = "ex_peso_muerto_mancuernas",
                name = "Peso muerto mancuernas",
                description = "Variante del peso muerto para isquiotibiales y glÃºteos.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 40f
            ),
            ExerciseEntity(
                id = "ex_peso_muerto_rumano",
                name = "Peso muerto rumano",
                description = "Ejercicio para isquiotibiales y glÃºteos, mejora la cadena posterior.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 30f
            ),
            ExerciseEntity(
                id = "ex_extension_cuadriceps",
                name = "ExtensiÃ³n de cuÃ¡driceps",
                description = "Ejercicio de aislamiento para fortalecer los cuÃ¡driceps.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 60f
            ),
            ExerciseEntity(
                id = "ex_curl_isquio_tumbado",
                name = "Curl isquio tumbado",
                description = "Ejercicio de aislamiento para fortalecer los isquiotibiales.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 31f
            ),
            ExerciseEntity(
                id = "ex_aductor_maquina",
                name = "Aductor mÃ¡quina",
                description = "Trabaja los mÃºsculos aductores internos del muslo.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 60f
            ),
            ExerciseEntity(
                id = "ex_abductor_maquina",
                name = "Abductor mÃ¡quina",
                description = "Fortalece los mÃºsculos abductores, responsables de la apertura de caderas.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 2,
                currentReps = 10,
                currentWeightKg = 60f
            ),
            ExerciseEntity(
                id = "ex_gemelo_mancuerna",
                name = "Gemelo con mancuernas",
                description = "Ejercicio para fortalecer los gemelos y mejorar la estabilidad del tobillo.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 44f
            ),
            ExerciseEntity(
                id = "ex_gemelo_maquina",
                name = "Gemelo mÃ¡quina",
                description = "AÃ­sla los mÃºsculos de los gemelos para mejorar fuerza y volumen.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 25,
                currentWeightKg = 40f
            ),
            ExerciseEntity(
                id = "ex_soleo_maquina",
                name = "SÃ³leo mÃ¡quina",
                description = "Fortalece el sÃ³leo, parte profunda del gemelo.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 25,
                currentWeightKg = 30f
            ),
            ExerciseEntity(
                id = "ex_hack_squat",
                name = "Hack squat",
                description = "Ejercicio guiado para cuÃ¡driceps, glÃºteos y femorales.",
                muscleGroup = MuscleGroup.LEGS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 110f
            ),
            ExerciseEntity(
                id = "ex_hip_thrust_barra",
                name = "Hip Thrust barra",
                description = "Ejercicio clave para activar y fortalecer los glÃºteos.",
                muscleGroup = MuscleGroup.GLUTES,
                currentSeries = 4,
                currentReps = 12,
                currentWeightKg = 30f
            ),
            ExerciseEntity(
                id = "ex_hip_thrust_mancuerna",
                name = "Hip Thrust mancuerna",
                description = "Variante con mancuernas para trabajar glÃºteos e isquiotibiales.",
                muscleGroup = MuscleGroup.GLUTES,
                currentSeries = 3,
                currentReps = 8,
                currentWeightKg = 20f
            ),
            ExerciseEntity(
                id = "ex_puente_gluteo_unilateral",
                name = "Puente glÃºteo unilateral mancuerna",
                description = "Ejercicio unilateral que fortalece los glÃºteos y la estabilidad de cadera.",
                muscleGroup = MuscleGroup.GLUTES,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 14f
            ),
            ExerciseEntity(
                id = "ex_curl_biceps_mancuernas",
                name = "Curl bÃ­ceps mancuernas",
                description = "Ejercicio bÃ¡sico de aislamiento para fortalecer los bÃ­ceps.",
                muscleGroup = MuscleGroup.BICEPS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 25f
            ),
            ExerciseEntity(
                id = "ex_curl_biceps_martillo",
                name = "Curl bÃ­ceps martillo",
                description = "Ejercicio para trabajar braquial y bÃ­ceps con agarre neutro.",
                muscleGroup = MuscleGroup.BICEPS,
                currentSeries = 4,
                currentReps = 12,
                currentWeightKg = 25f
            ),
            ExerciseEntity(
                id = "ex_curl_biceps_scott",
                name = "Curl bÃ­ceps Scott",
                description = "AÃ­sla el bÃ­ceps y mejora la fuerza en la parte baja del movimiento.",
                muscleGroup = MuscleGroup.BICEPS,
                currentSeries = 4,
                currentReps = 10,
                currentWeightKg = 15f
            ),
            ExerciseEntity(
                id = "ex_triceps_cuerda_polea",
                name = "ExtensiÃ³n trÃ­ceps cuerda polea",
                description = "Ejercicio de aislamiento para desarrollar la parte posterior del brazo.",
                muscleGroup = MuscleGroup.TRICEPS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 45f
            ),
            ExerciseEntity(
                id = "ex_triceps_overhead_polea_baja",
                name = "TrÃ­ceps overhead polea baja",
                description = "Ejercicio que estira y activa la cabeza larga del trÃ­ceps.",
                muscleGroup = MuscleGroup.TRICEPS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 15f
            ),
            ExerciseEntity(
                id = "ex_triceps_overhead_mancuernas",
                name = "TrÃ­ceps overhead mancuernas",
                description = "Fortalece la cabeza larga del trÃ­ceps con mancuernas.",
                muscleGroup = MuscleGroup.TRICEPS,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 20f
            ),
            ExerciseEntity(
                id = "ex_press_pecho_plano_maquina",
                name = "Press pecho plano mÃ¡quina",
                description = "Ejercicio principal para pectorales, trÃ­ceps y deltoides frontales.",
                muscleGroup = MuscleGroup.CHEST,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 90f
            ),
            ExerciseEntity(
                id = "ex_press_pecho_inclinado_maquina",
                name = "Press pecho inclinado mÃ¡quina",
                description = "Trabaja la parte superior del pecho y los deltoides frontales.",
                muscleGroup = MuscleGroup.CHEST,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 60f
            ),
            ExerciseEntity(
                id = "ex_press_banca_barra",
                name = "Press banca barra",
                description = "Ejercicio compuesto para pectorales, hombros y trÃ­ceps.",
                muscleGroup = MuscleGroup.CHEST,
                currentSeries = 4,
                currentReps = 10,
                currentWeightKg = 40f
            ),
            ExerciseEntity(
                id = "ex_cruce_poleas",
                name = "Cruce de poleas",
                description = "Ejercicio de aislamiento para definir y contraer los pectorales.",
                muscleGroup = MuscleGroup.CHEST,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 25f
            ),
            ExerciseEntity(
                id = "ex_jalon_polea_prono",
                name = "JalÃ³n polea agarre prono",
                description = "Ejercicio para dorsales y parte media de la espalda.",
                muscleGroup = MuscleGroup.BACK,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 71f
            ),
            ExerciseEntity(
                id = "ex_remo_gironda",
                name = "Remo gironda",
                description = "Desarrolla el grosor y densidad de la espalda media.",
                muscleGroup = MuscleGroup.BACK,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 65f
            ),
            ExerciseEntity(
                id = "ex_remo_pecho_apoyado",
                name = "Remo pecho apoyado mÃ¡quina",
                description = "AÃ­sla la espalda media y reduce el estrÃ©s lumbar.",
                muscleGroup = MuscleGroup.BACK,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 80f
            ),
            ExerciseEntity(
                id = "ex_pull_over_polea",
                name = "Pull over polea alta",
                description = "Ejercicio de aislamiento para dorsales y serrato anterior.",
                muscleGroup = MuscleGroup.BACK,
                currentSeries = 3,
                currentReps = 12,
                currentWeightKg = 45f
            ),
            ExerciseEntity(
                id = "ex_press_militar_maquina",
                name = "Press militar mÃ¡quina",
                description = "Ejercicio compuesto para los deltoides y trÃ­ceps.",
                muscleGroup = MuscleGroup.SHOULDERS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 40f
            ),
            ExerciseEntity(
                id = "ex_elevaciones_laterales_mancuerna",
                name = "Elevaciones laterales mancuernas",
                description = "Ejercicio para desarrollar los deltoides laterales y forma del hombro.",
                muscleGroup = MuscleGroup.SHOULDERS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 15f
            ),
            ExerciseEntity(
                id = "ex_deltoides_posterior_maquina",
                name = "Deltoides posterior mÃ¡quina",
                description = "AÃ­sla la parte posterior del hombro, mejorando postura y equilibrio muscular.",
                muscleGroup = MuscleGroup.SHOULDERS,
                currentSeries = 3,
                currentReps = 10,
                currentWeightKg = 18f
            ),
            ExerciseEntity(
                id = "ex_pajaros_sentado",
                name = "PÃ¡jaros sentado mancuernas",
                description = "Ejercicio de aislamiento para deltoides posteriores.",
                muscleGroup = MuscleGroup.SHOULDERS,
                currentSeries = 4,
                currentReps = 10,
                currentWeightKg = 5f
            ),
        )
        // Usar transacción para inserciones masivas (mucho más rápido)
        database.withTransaction {
            sampleExercises.forEach { exercise ->
                exerciseDao.insertExercise(exercise)

                // Add initial history entry for each exercise
                if (exercise.currentSeries > 0) {
                    historyDao.insertHistory(
                        ExerciseHistoryEntity(
                            exerciseId = exercise.id,
                            series = exercise.currentSeries,
                            reps = exercise.currentReps,
                            weightKg = exercise.currentWeightKg,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        android.util.Log.d("GymLogDB", "Prepopulation completed: ${sampleExercises.size} exercises inserted")
    }
}