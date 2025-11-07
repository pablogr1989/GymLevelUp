package com.gymlog.app.ui.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import com.gymlog.app.domain.model.ExerciseHistory
import java.util.UUID

enum class SeriesAction {
    CONTINUE,
    CONFIRM_FINISH
}

@HiltViewModel
class TrainingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val daySlotId: String = checkNotNull(savedStateHandle["daySlotId"])

    // 1. Datos básicos
    private val _daySlot = MutableStateFlow<DaySlot?>(null)
    val daySlot: StateFlow<DaySlot?> = _daySlot.asStateFlow()

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    // 2. Estado del entrenamiento
    private val _isTrainingActive = MutableStateFlow(false)
    val isTrainingActive: StateFlow<Boolean> = _isTrainingActive.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    // 3. Estado del ejercicio actual
    private val _currentSeries = MutableStateFlow(1)
    val currentSeries: StateFlow<Int> = _currentSeries.asStateFlow()

    private val _currentWeight = MutableStateFlow(0f)
    val currentWeight: StateFlow<Float> = _currentWeight.asStateFlow()

    private val _currentNotes = MutableStateFlow("")
    val currentNotes: StateFlow<String> = _currentNotes.asStateFlow()

    // 4. Timer
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _restMinutes = MutableStateFlow(2)
    val restMinutes: StateFlow<Int> = _restMinutes.asStateFlow()

    // 5. Botones
    private val _isSeriesButtonEnabled = MutableStateFlow(false)
    val isSeriesButtonEnabled: StateFlow<Boolean> = _isSeriesButtonEnabled.asStateFlow()

    private val _isSeriesRunning = MutableStateFlow(false)
    val isSeriesRunning: StateFlow<Boolean> = _isSeriesRunning.asStateFlow()

    init {
        loadDaySlotAndExercises()
    }

    private fun loadDaySlotAndExercises() {
        viewModelScope.launch {
            // Cargar DaySlot
            _daySlot.value = calendarRepository.getDayById(daySlotId)
            _daySlot.value?.let { daySlot ->
                val exerciseIds = daySlot.selectedExerciseIds
                val allExercises = exerciseRepository.getAllExercises().first()
                val exerciseMap = allExercises.associateBy { it.id }
                _exercises.value = exerciseIds.mapNotNull { id -> exerciseMap[id] }

                // Inicializar datos del primer ejercicio
                _exercises.value.firstOrNull()?.let { firstExercise ->
                    _currentWeight.value = firstExercise.currentWeightKg
                    _currentNotes.value = firstExercise.notes
                }
            }
        }
    }

    fun startTraining() {
        _isTrainingActive.value = true
        _currentExerciseIndex.value = 0
        _currentSeries.value = 1
        _isSeriesButtonEnabled.value = true

        // Cargar datos del primer ejercicio
        _exercises.value.firstOrNull()?.let { exercise ->
            _currentWeight.value = exercise.currentWeightKg
            _currentNotes.value = exercise.notes
        }
    }

    fun endTraining() {
        viewModelScope.launch {
            // Guardar cambios finales si hubo modificación de peso
            saveCurrentExerciseChanges()

            // Marcar DaySlot como completado
            calendarRepository.updateDayCompleted(daySlotId, true)

            // Reset estado
            _isTrainingActive.value = false
            _currentExerciseIndex.value = 0
            _currentSeries.value = 1
            _isSeriesRunning.value = false
            _isSeriesButtonEnabled.value = false
            stopTimer()
        }
    }

    fun updateWeight(weight: Float) {
        _currentWeight.value = weight
    }

    fun updateNotes(notes: String) {
        _currentNotes.value = notes
    }

    fun updateRestMinutes(minutes: Int) {
        _restMinutes.value = minutes.coerceIn(1, 99)
    }

    // ============ FUNCIONES DE SERIES ============

    fun startSeries() {
        _isSeriesRunning.value = true
        _isSeriesButtonEnabled.value = true

        // Resetear timer al valor del number input
        _timerSeconds.value = _restMinutes.value * 60

        // Deshabilitar botones de timer
        _isTimerRunning.value = false
    }

    fun stopSeries(): SeriesAction {
        val currentExercise = _exercises.value.getOrNull(_currentExerciseIndex.value)
        val maxSeries = (currentExercise?.currentSeries ?: 0) + 1 // +1 por calentamiento

        return if (_currentSeries.value + 1 <= maxSeries) {
            // No es la última serie
            _currentSeries.value += 1
            _isSeriesButtonEnabled.value = false
            _isSeriesRunning.value = false

            // Iniciar timer countdown
            startTimer()

            SeriesAction.CONTINUE
        } else {
            // Es la última serie
            SeriesAction.CONFIRM_FINISH
        }
    }

    fun confirmFinishExercise() {
        viewModelScope.launch {
            saveCurrentExerciseChanges()
            moveToNextExercise()
        }
    }

    fun finishExerciseManually() {
        viewModelScope.launch {
            saveCurrentExerciseChanges()
            moveToNextExercise()
        }
    }

    private suspend fun moveToNextExercise() {
        val nextIndex = _currentExerciseIndex.value + 1

        if (nextIndex < _exercises.value.size) {
            // Hay más ejercicios
            _currentExerciseIndex.value = nextIndex
            _currentSeries.value = 1
            _isSeriesRunning.value = false
            _isSeriesButtonEnabled.value = false
            stopTimer()

            // Cargar datos del siguiente ejercicio
            _exercises.value.getOrNull(nextIndex)?.let { exercise ->
                _currentWeight.value = exercise.currentWeightKg
                _currentNotes.value = exercise.notes
            }
        } else {
            // Era el último ejercicio, terminar entrenamiento
            endTraining()
        }
    }

    // ============ FUNCIONES DE TIMER ============

    private var timerJob: Job? = null

    private fun startTimer() {
        _isTimerRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _timerSeconds.value -= 1
            }

            // Timer llegó a 0
            if (_timerSeconds.value == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_timerSeconds.value > 0) {
            startTimer()
        }
    }

    fun restartTimer() {
        _timerSeconds.value = _restMinutes.value * 60
        startTimer()
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        _timerSeconds.value = 0
        timerJob?.cancel()

        // Habilitar botón de serie si estaba deshabilitado
        if (!_isSeriesButtonEnabled.value) {
            _isSeriesButtonEnabled.value = true
        }
    }

    private fun onTimerFinished() {
        _isTimerRunning.value = false

        // Habilitar botón de serie
        _isSeriesButtonEnabled.value = true
    }

    // ============ GUARDAR CAMBIOS ============

    private suspend fun saveCurrentExerciseChanges() {
        val currentExercise = _exercises.value.getOrNull(_currentExerciseIndex.value) ?: return

        val weightChanged = _currentWeight.value != currentExercise.currentWeightKg
        val notesChanged = _currentNotes.value != currentExercise.notes

        // Actualizar notas si cambiaron
        if (notesChanged) {
            exerciseRepository.updateExerciseNotes(
                exerciseId = currentExercise.id,
                notes = _currentNotes.value
            )
        }

        // Actualizar peso y crear historial si cambió
        if (weightChanged) {
            exerciseRepository.updateExerciseStats(
                exerciseId = currentExercise.id,
                series = currentExercise.currentSeries,
                reps = currentExercise.currentReps,
                weight = _currentWeight.value
            )

            // Crear registro en historial
            val history = ExerciseHistory(
                id = UUID.randomUUID().toString(),
                exerciseId = currentExercise.id,
                timestamp = System.currentTimeMillis(),
                series = currentExercise.currentSeries,
                reps = currentExercise.currentReps,
                weightKg = _currentWeight.value
            )
            exerciseRepository.insertHistory(history)
        }
    }
}