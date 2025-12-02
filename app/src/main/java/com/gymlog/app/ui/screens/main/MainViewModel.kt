package com.gymlog.app.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.data.local.GymLogDatabase
import com.gymlog.app.data.local.entity.ExerciseEntity
import com.gymlog.app.data.local.entity.ExerciseHistoryEntity
import com.gymlog.app.data.local.entity.MuscleGroup
import com.gymlog.app.data.local.entity.SetEntity
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val database: GymLogDatabase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup = _selectedMuscleGroup.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow<Exercise?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    private val allExercisesFlow = repository.getAllExercises()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val exercises: StateFlow<Map<MuscleGroup, List<Exercise>>> =
        combine(
            allExercisesFlow,
            searchQuery,
            selectedMuscleGroup
        ) { exercises, query, group ->
            when {
                exercises.isEmpty() -> emptyMap()
                query.isEmpty() && group == null -> {
                    exercises.groupBy { it.muscleGroup }
                }
                else -> {
                    exercises.asSequence()
                        .filter { exercise ->
                            query.isEmpty() || exercise.name.contains(query, ignoreCase = true)
                        }
                        .filter { exercise ->
                            group == null || exercise.muscleGroup == group
                        }
                        .groupBy { it.muscleGroup }
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyMap()
        )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectMuscleGroup(group: MuscleGroup?) {
        _selectedMuscleGroup.value = group
    }

    fun showDeleteDialog(exercise: Exercise) {
        _showDeleteDialog.value = exercise
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = null
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
            dismissDeleteDialog()
        }
    }

    fun prepopulateDatabase() {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Starting database prepopulation from UI")
            try {
                prepopulateDatabaseInternal(database)
                android.util.Log.d("MainViewModel", "Database prepopulation completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainViewModel", "Error during database prepopulation", e)
            }
        }
    }

    private suspend fun prepopulateDatabaseInternal(database: GymLogDatabase) {
        android.util.Log.d("MainViewModel", "Starting database cleanup and prepopulation")

        // Limpiar toda la base de datos
        try {
            database.exerciseHistoryDao().deleteAllHistory()
            database.setDao().deleteAllSets() // Nuevo: Limpiar sets
            database.exerciseDao().deleteAllExercises()
            database.daySlotDao().deleteAllDaySlots()
            database.weekDao().deleteAllWeeks()
            database.monthDao().deleteAllMonths()
            database.calendarDao().deleteAllCalendars()
            android.util.Log.d("MainViewModel", "Database cleared successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error clearing database", e)
        }

        android.util.Log.d("MainViewModel", "Starting database prepopulation")
        val exerciseDao = database.exerciseDao()
        val historyDao = database.exerciseHistoryDao()
        val setDao = database.setDao()

        // Helper class para mantener los datos organizados antes de dividirlos
        data class LegacySample(
            val id: String,
            val name: String,
            val description: String,
            val muscleGroup: MuscleGroup,
            val series: Int,
            val reps: Int,
            val weight: Float
        )

        val sampleExercises = listOf(
            LegacySample(
                "ex_prensa_inclinada_discos", "Prensa inclinada discos",
                "Ejercicio principal para cuádriceps y glúteos, trabaja también femorales.", MuscleGroup.LEGS, 3, 12, 150f
            ),
            LegacySample(
                "ex_sentadilla_multipower", "Sentadilla multipower",
                "Ejercicio guiado para cuádriceps, glúteos y estabilidad del core.", MuscleGroup.LEGS, 3, 12, 40f
            ),
            LegacySample(
                "ex_sentadilla_barra", "Sentadilla con barra",
                "Ejercicio compuesto para cuádriceps, glúteos y core.", MuscleGroup.LEGS, 3, 10, 70f
            ),
            LegacySample(
                "ex_sentadilla_bulgara", "Sentadilla búlgara",
                "Ejercicio unilateral para cuádriceps, glúteos y equilibrio.", MuscleGroup.LEGS, 3, 12, 10f
            ),
            LegacySample(
                "ex_zancada_multipower", "Zancada multipower",
                "Fortalece cuádriceps, glúteos y estabilidad de cadera.", MuscleGroup.LEGS, 3, 12, 35f
            ),
            LegacySample(
                "ex_zancada_pasos", "Zancada con pasos",
                "Ejercicio funcional que trabaja cuádriceps y glúteos.", MuscleGroup.LEGS, 2, 12, 20f
            ),
            LegacySample(
                "ex_peso_muerto_barra", "Peso muerto convencional barra",
                "Ejercicio compuesto que trabaja glúteos, isquiotibiales y espalda baja.", MuscleGroup.LEGS, 3, 10, 30f
            ),
            LegacySample(
                "ex_peso_muerto_mancuernas", "Peso muerto mancuernas",
                "Variante del peso muerto para isquiotibiales y glúteos.", MuscleGroup.LEGS, 3, 12, 40f
            ),
            LegacySample(
                "ex_peso_muerto_rumano", "Peso muerto rumano",
                "Ejercicio para isquiotibiales y glúteos, mejora la cadena posterior.", MuscleGroup.LEGS, 3, 10, 30f
            ),
            LegacySample(
                "ex_extension_cuadriceps", "Extensión de cuádriceps",
                "Ejercicio de aislamiento para fortalecer los cuádriceps.", MuscleGroup.LEGS, 3, 12, 60f
            ),
            LegacySample(
                "ex_curl_isquio_tumbado", "Curl isquio tumbado",
                "Ejercicio de aislamiento para fortalecer los isquiotibiales.", MuscleGroup.LEGS, 3, 12, 31f
            ),
            LegacySample(
                "ex_aductor_maquina", "Aductor máquina",
                "Trabaja los músculos aductores internos del muslo.", MuscleGroup.LEGS, 3, 10, 60f
            ),
            LegacySample(
                "ex_abductor_maquina", "Abductor máquina",
                "Fortalece los músculos abductores, responsables de la apertura de caderas.", MuscleGroup.LEGS, 2, 10, 60f
            ),
            LegacySample(
                "ex_gemelo_mancuerna", "Gemelo con mancuernas",
                "Ejercicio para fortalecer los gemelos y mejorar la estabilidad del tobillo.", MuscleGroup.LEGS, 3, 12, 44f
            ),
            LegacySample(
                "ex_gemelo_maquina", "Gemelo máquina",
                "Aísla los músculos de los gemelos para mejorar fuerza y volumen.", MuscleGroup.LEGS, 3, 25, 40f
            ),
            LegacySample(
                "ex_soleo_maquina", "Sóleo máquina",
                "Fortalece el sóleo, parte profunda del gemelo.", MuscleGroup.LEGS, 3, 25, 30f
            ),
            LegacySample(
                "ex_hack_squat", "Hack squat",
                "Ejercicio guiado para cuádriceps, glúteos y femorales.", MuscleGroup.LEGS, 3, 12, 110f
            ),
            LegacySample(
                "ex_hip_thrust_barra", "Hip Thrust barra",
                "Ejercicio clave para activar y fortalecer los glúteos.", MuscleGroup.GLUTES, 4, 12, 30f
            ),
            LegacySample(
                "ex_hip_thrust_mancuerna", "Hip Thrust mancuerna",
                "Variante con mancuernas para trabajar glúteos e isquiotibiales.", MuscleGroup.GLUTES, 3, 8, 20f
            ),
            LegacySample(
                "ex_puente_gluteo_unilateral", "Puente glúteo unilateral mancuerna",
                "Ejercicio unilateral que fortalece los glúteos y la estabilidad de cadera.", MuscleGroup.GLUTES, 3, 12, 14f
            ),
            LegacySample(
                "ex_curl_biceps_mancuernas", "Curl bíceps mancuernas",
                "Ejercicio básico de aislamiento para fortalecer los bíceps.", MuscleGroup.BICEPS, 3, 12, 25f
            ),
            LegacySample(
                "ex_curl_biceps_martillo", "Curl bíceps martillo",
                "Ejercicio para trabajar braquial y bíceps con agarre neutro.", MuscleGroup.BICEPS, 4, 12, 25f
            ),
            LegacySample(
                "ex_curl_biceps_scott", "Curl bíceps Scott",
                "Aísla el bíceps y mejora la fuerza en la parte baja del movimiento.", MuscleGroup.BICEPS, 4, 10, 15f
            ),
            LegacySample(
                "ex_triceps_cuerda_polea", "Extensión tríceps cuerda polea",
                "Ejercicio de aislamiento para desarrollar la parte posterior del brazo.", MuscleGroup.TRICEPS, 3, 12, 45f
            ),
            LegacySample(
                "ex_triceps_overhead_polea_baja", "Tríceps overhead polea baja",
                "Ejercicio que estira y activa la cabeza larga del tríceps.", MuscleGroup.TRICEPS, 3, 12, 15f
            ),
            LegacySample(
                "ex_triceps_overhead_mancuernas", "Tríceps overhead mancuernas",
                "Fortalece la cabeza larga del tríceps con mancuernas.", MuscleGroup.TRICEPS, 3, 12, 20f
            ),
            LegacySample(
                "ex_press_pecho_plano_maquina", "Press pecho plano máquina",
                "Ejercicio principal para pectorales, tríceps y deltoides frontales.", MuscleGroup.CHEST, 3, 10, 90f
            ),
            LegacySample(
                "ex_press_pecho_inclinado_maquina", "Press pecho inclinado máquina",
                "Trabaja la parte superior del pecho y los deltoides frontales.", MuscleGroup.CHEST, 3, 10, 60f
            ),
            LegacySample(
                "ex_press_banca_barra", "Press banca barra",
                "Ejercicio compuesto para pectorales, hombros y tríceps.", MuscleGroup.CHEST, 4, 10, 40f
            ),
            LegacySample(
                "ex_cruce_poleas", "Cruce de poleas",
                "Ejercicio de aislamiento para definir y contraer los pectorales.", MuscleGroup.CHEST, 3, 10, 25f
            ),
            LegacySample(
                "ex_jalon_polea_prono", "Jalón polea agarre prono",
                "Ejercicio para dorsales y parte media de la espalda.", MuscleGroup.BACK, 3, 12, 71f
            ),
            LegacySample(
                "ex_remo_gironda", "Remo gironda",
                "Desarrolla el grosor y densidad de la espalda media.", MuscleGroup.BACK, 3, 10, 65f
            ),
            LegacySample(
                "ex_remo_pecho_apoyado", "Remo pecho apoyado máquina",
                "Aísla la espalda media y reduce el estrés lumbar.", MuscleGroup.BACK, 3, 12, 80f
            ),
            LegacySample(
                "ex_pull_over_polea", "Pull over polea alta",
                "Ejercicio de aislamiento para dorsales y serrato anterior.", MuscleGroup.BACK, 3, 12, 45f
            ),
            LegacySample(
                "ex_press_militar_maquina", "Press militar máquina",
                "Ejercicio compuesto para los deltoides y tríceps.", MuscleGroup.SHOULDERS, 3, 10, 40f
            ),
            LegacySample(
                "ex_elevaciones_laterales_mancuerna", "Elevaciones laterales mancuernas",
                "Ejercicio para desarrollar los deltoides laterales y forma del hombro.", MuscleGroup.SHOULDERS, 3, 10, 15f
            ),
            LegacySample(
                "ex_deltoides_posterior_maquina", "Deltoides posterior máquina",
                "Aísla la parte posterior del hombro, mejorando postura y equilibrio muscular.", MuscleGroup.SHOULDERS, 3, 10, 18f
            ),
            LegacySample(
                "ex_pajaros_sentado", "Pájaros sentado mancuernas",
                "Ejercicio de aislamiento para deltoides posteriores.", MuscleGroup.SHOULDERS, 4, 10, 5f
            )
        )

        // Insertar ejercicios y sets
        sampleExercises.forEach { sample ->
            android.util.Log.d("MainViewModel", "Inserting exercise: ${sample.name}")

            // 1. Insertar Ejercicio (Sin stats)
            exerciseDao.insertExercise(
                ExerciseEntity(
                    id = sample.id,
                    name = sample.name,
                    description = sample.description,
                    muscleGroup = sample.muscleGroup,
                    createdAt = System.currentTimeMillis()
                )
            )

            // 2. Insertar Set Inicial (Con stats)
            if (sample.series > 0) {
                val setId = UUID.randomUUID().toString()

                setDao.insertSet(
                    SetEntity(
                        id = setId,
                        exerciseId = sample.id,
                        series = sample.series,
                        reps = sample.reps,
                        weightKg = sample.weight
                    )
                )

                // 3. Insertar Historial (Vinculado al Set)
                historyDao.insertHistory(
                    ExerciseHistoryEntity(
                        exerciseId = sample.id,
                        setId = setId,
                        series = sample.series,
                        reps = sample.reps,
                        weightKg = sample.weight,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        android.util.Log.d("MainViewModel", "Prepopulation completed: ${sampleExercises.size} exercises inserted")
    }
}