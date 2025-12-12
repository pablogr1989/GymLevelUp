package com.gymlog.app.ui.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.util.TimerManager
import com.gymlog.app.util.TrainingConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class SeriesAction {
    CONTINUE,
    CONFIRM_FINISH
}

data class TrainingUiState(
    val daySlot: DaySlot? = null,
    val exercises: List<Exercise> = emptyList(),
    val selectedSetIds: List<String?> = emptyList(),
    val isTrainingActive: Boolean = false,

    // Índices
    val currentExerciseIndex: Int = 0,
    val activeSetIndex: Int = 0,

    // Estado del set activo
    val currentSeries: Int = 1,
    val currentWeight: Float = 0f,
    val currentNotes: String = "",

    // Timer (Datos provenientes del Manager)
    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isAlarmRinging: Boolean = false,

    // Configuración local
    val restMinutes: Int = 2,

    // UI Flags
    val isSeriesButtonEnabled: Boolean = false,
    val isSeriesRunning: Boolean = false,
    val showExitConfirmation: Boolean = false
)

@HiltViewModel
class TrainingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timerManager: TimerManager // INYECTADO
) : ViewModel() {

    private val daySlotId: String = checkNotNull(savedStateHandle["daySlotId"])

    private val _localUiState = MutableStateFlow(TrainingUiState())

    // Fusionamos el estado local con el estado del TimerManager
    val uiState: StateFlow<TrainingUiState> = kotlinx.coroutines.flow.combine(
        _localUiState,
        timerManager.timerState
    ) { local, timer ->
        local.copy(
            timerSeconds = timer.remainingSeconds,
            isTimerRunning = timer.isRunning,
            isAlarmRinging = timer.isAlarmRinging,
            // Si el timer termina (suena alarma), habilitamos botones para la siguiente serie
            isSeriesButtonEnabled = local.isSeriesButtonEnabled || timer.isAlarmRinging
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrainingUiState()
    )

    // --- Legacy StateFlows ---
    val daySlot = uiState.map { it.daySlot }.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val exercises = uiState.map { it.exercises }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val isTrainingActive = uiState.map { it.isTrainingActive }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val currentExerciseIndex = uiState.map { it.currentExerciseIndex }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val currentSeries = uiState.map { it.currentSeries }.stateIn(viewModelScope, SharingStarted.Lazily, 1)
    val currentWeight = uiState.map { it.currentWeight }.stateIn(viewModelScope, SharingStarted.Lazily, 0f)
    val currentNotes = uiState.map { it.currentNotes }.stateIn(viewModelScope, SharingStarted.Lazily, "")
    val timerSeconds = uiState.map { it.timerSeconds }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val isTimerRunning = uiState.map { it.isTimerRunning }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val restMinutes = uiState.map { it.restMinutes }.stateIn(viewModelScope, SharingStarted.Lazily, 2)
    val isSeriesButtonEnabled = uiState.map { it.isSeriesButtonEnabled }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val isSeriesRunning = uiState.map { it.isSeriesRunning }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    // -------------------------

    init {
        loadDaySlotAndExercises()
    }

    private fun loadDaySlotAndExercises() {
        viewModelScope.launch {
            val daySlotLoaded = calendarRepository.getDayById(daySlotId)

            var exercisesList: List<Exercise> = emptyList()
            var selectedSetIdsList: List<String?> = emptyList()

            var initialWeight = 0f
            var initialNotes = ""
            var initialSetIndex = 0

            daySlotLoaded?.let { slot ->
                val allExercises = exerciseRepository.getAllExercises().first()
                val exerciseMap = allExercises.associateBy { it.id }

                val loadedData = slot.exercises.mapNotNull { assignment ->
                    val exercise = exerciseMap[assignment.exerciseId]
                    if (exercise != null) {
                        exercise to assignment.targetSetId
                    } else null
                }

                exercisesList = loadedData.map { it.first }
                selectedSetIdsList = loadedData.map { it.second }

                if (exercisesList.isNotEmpty()) {
                    val firstExercise = exercisesList[0]
                    val targetSetId = selectedSetIdsList[0]

                    initialSetIndex = if (targetSetId != null) {
                        firstExercise.sets.indexOfFirst { it.id == targetSetId }.coerceAtLeast(0)
                    } else {
                        0
                    }

                    val activeSet = firstExercise.sets.getOrNull(initialSetIndex)
                    initialWeight = activeSet?.weightKg ?: 0f
                    initialNotes = firstExercise.notes
                }
            }

            _localUiState.update { it.copy(
                daySlot = daySlotLoaded,
                exercises = exercisesList,
                selectedSetIds = selectedSetIdsList,
                currentWeight = initialWeight,
                currentNotes = initialNotes,
                activeSetIndex = initialSetIndex
            )}
        }
    }

    fun startTraining() {
        _localUiState.update { it.copy(
            isTrainingActive = true,
            currentExerciseIndex = 0,
            currentSeries = 1,
            isSeriesButtonEnabled = true
        )}
        loadCurrentExerciseData()
    }

    private fun loadCurrentExerciseData() {
        val state = _localUiState.value
        val exerciseIndex = state.currentExerciseIndex
        val exercise = state.exercises.getOrNull(exerciseIndex) ?: return

        val targetSetId = state.selectedSetIds.getOrNull(exerciseIndex)
        val setIndex = if (targetSetId != null) {
            exercise.sets.indexOfFirst { it.id == targetSetId }.coerceAtLeast(0)
        } else {
            0
        }

        val activeSet = exercise.sets.getOrNull(setIndex)

        _localUiState.update { it.copy(
            activeSetIndex = setIndex,
            currentWeight = activeSet?.weightKg ?: 0f,
            currentNotes = exercise.notes
        )}
    }

    fun endTraining() {
        viewModelScope.launch {
            saveCurrentExerciseChanges()
            calendarRepository.updateDayCompleted(daySlotId, true)
            stopTimer()

            _localUiState.update { it.copy(
                isTrainingActive = false,
                currentExerciseIndex = 0,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = false
            )}
        }
    }

    fun updateWeight(weight: Float) {
        _localUiState.update { it.copy(currentWeight = weight) }
    }

    fun updateNotes(notes: String) {
        _localUiState.update { it.copy(currentNotes = notes) }
    }

    fun updateRestMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, 99)
        _localUiState.update { it.copy(restMinutes = coerced) }
    }

    // ============ FUNCIONES DE SERIES ============

    fun startSeries() {
        _localUiState.update { it.copy(
            isSeriesRunning = true,
            isSeriesButtonEnabled = true
        )}
        // Aseguramos que el timer esté limpio al empezar una serie (estamos trabajando)
        timerManager.stopTimer(resetState = true)
    }

    fun stopSeries(): SeriesAction {
        val state = _localUiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex)
        val activeSet = currentExercise?.sets?.getOrNull(state.activeSetIndex)
        val maxSeries = (activeSet?.series ?: 0)

        return if (state.currentSeries < maxSeries) {
            _localUiState.update { it.copy(
                currentSeries = it.currentSeries + 1,
                isSeriesButtonEnabled = false,
                isSeriesRunning = false
            )}
            // Iniciamos el descanso con el TimerManager
            restartTimer()
            SeriesAction.CONTINUE
        } else {
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

    private fun moveToNextExercise() {
        val nextIndex = _localUiState.value.currentExerciseIndex + 1

        if (nextIndex < _localUiState.value.exercises.size) {
            stopTimer()
            _localUiState.update { it.copy(
                currentExerciseIndex = nextIndex,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = true
            )}
            loadCurrentExerciseData()
        } else {
            endTraining()
        }
    }

    // ============ FUNCIONES DE TIMER (DELEGADAS) ============

    fun pauseTimer() {
        timerManager.pauseTimer()
    }

    fun resumeTimer() {
        timerManager.resumeTimer(TrainingConstants.TIMER_TYPE_TRAINING)
    }

    fun restartTimer() {
        val seconds = _localUiState.value.restMinutes * 60
        timerManager.startTimer(seconds, TrainingConstants.TIMER_TYPE_TRAINING)
    }

    fun stopTimer() {
        timerManager.stopTimer()
        _localUiState.update { it.copy(isSeriesButtonEnabled = true) }
    }

    fun stopAlarm() {
        timerManager.stopAlarm()
    }

    // ============ GUARDAR CAMBIOS ============

    private suspend fun saveCurrentExerciseChanges() {
        val state = _localUiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
        val activeSet = currentExercise.sets.getOrNull(state.activeSetIndex) ?: return

        val weightChanged = state.currentWeight != activeSet.weightKg
        val notesChanged = state.currentNotes != currentExercise.notes

        if (notesChanged) {
            exerciseRepository.updateExerciseNotes(
                exerciseId = currentExercise.id,
                notes = state.currentNotes
            )
        }

        if (weightChanged) {
            val updatedSet = activeSet.copy(weightKg = state.currentWeight)
            exerciseRepository.updateSet(updatedSet)
        }

        exerciseRepository.insertHistory(
            ExerciseHistory(
                id = UUID.randomUUID().toString(),
                exerciseId = currentExercise.id,
                setId = activeSet.id,
                timestamp = System.currentTimeMillis(),
                series = activeSet.series,
                reps = activeSet.reps,
                weightKg = state.currentWeight
            )
        )
    }

    fun onBackPressed() {
        if (_localUiState.value.isTrainingActive) {
            _localUiState.update { it.copy(showExitConfirmation = true) }
        }
    }

    fun confirmExit() {
        _localUiState.update { it.copy(showExitConfirmation = false, isTrainingActive = false) }
        stopTimer()
    }

    fun dismissExitConfirmation() {
        _localUiState.update { it.copy(showExitConfirmation = false) }
    }

    override fun onCleared() {
        super.onCleared()
        // No detenemos el TimerManager aquí porque es Singleton y podría querer seguir sonando
        // aunque cerremos esta pantalla específica, pero si esa es la intención:
        // timerManager.stopTimer()
    }
}