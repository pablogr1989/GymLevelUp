package com.gymlog.app.ui.screens.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.model.DetailedHistory
import com.gymlog.app.domain.model.Set
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

data class TrainingBlock(
    val exercise: Exercise,
    val sets: List<Set>
)

data class TrainingUiState(
    val daySlot: DaySlot? = null,
    val blocks: List<TrainingBlock> = emptyList(),
    val isTrainingActive: Boolean = false,

    val currentBlockIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val currentSeries: Int = 1,

    // Datos de la plantilla
    val currentWeight: Float = 0f,
    val currentNotes: String = "",

    // Datos EXCLUSIVOS del registro (Detailed History)
    val actualReps: String = "",
    val actualRir: String = "",
    val currentObservations: String = "",

    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val isAlarmRinging: Boolean = false,
    val restMinutes: Int = 2,

    val isSeriesButtonEnabled: Boolean = false,
    val isSeriesRunning: Boolean = false,

    // Diálogos
    val showExitConfirmation: Boolean = false,
    val showWeightChangeConfirmation: Boolean = false,
    val showFinishExercisePrompt: Boolean = false
)

@HiltViewModel
class TrainingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timerManager: TimerManager
) : ViewModel() {

    private val daySlotId: String = checkNotNull(savedStateHandle["daySlotId"])
    private val _localUiState = MutableStateFlow(TrainingUiState())

    val uiState: StateFlow<TrainingUiState> = kotlinx.coroutines.flow.combine(
        _localUiState, timerManager.timerState
    ) { local, timer ->
        local.copy(
            timerSeconds = timer.remainingSeconds,
            isTimerRunning = timer.isRunning,
            isAlarmRinging = timer.isAlarmRinging,
            isSeriesButtonEnabled = local.isSeriesButtonEnabled || timer.isAlarmRinging
        )
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = TrainingUiState())

    init {
        loadDaySlotAndExercises()
    }

    private fun loadDaySlotAndExercises() {
        viewModelScope.launch {
            val daySlotLoaded = calendarRepository.getDayById(daySlotId)
            var blocksLoaded: List<TrainingBlock> = emptyList()

            daySlotLoaded?.let { slot ->
                val allExercises = exerciseRepository.getAllExercises().first()
                val exerciseMap = allExercises.associateBy { it.id }

                blocksLoaded = slot.exercises.mapNotNull { assignment ->
                    val exercise = exerciseMap[assignment.exerciseId]
                    if (exercise != null) {
                        val setIds = assignment.targetSetId?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                        val selectedSets = if (setIds.isNotEmpty()) {
                            setIds.mapNotNull { id -> exercise.sets.find { it.id == id } }
                        } else {
                            listOfNotNull(exercise.sets.firstOrNull())
                        }
                        if (selectedSets.isNotEmpty()) TrainingBlock(exercise, selectedSets) else null
                    } else null
                }
            }

            _localUiState.update { it.copy(
                daySlot = daySlotLoaded,
                blocks = blocksLoaded
            )}
        }
    }

    fun startTraining() {
        _localUiState.update { it.copy(
            isTrainingActive = true,
            currentBlockIndex = 0,
            currentSetIndex = 0,
            currentSeries = 1,
            isSeriesButtonEnabled = true
        )}
        loadCurrentBlockData()
    }

    private fun loadCurrentBlockData() {
        val state = _localUiState.value
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return
        val activeSet = block.sets.getOrNull(state.currentSetIndex)

        _localUiState.update { it.copy(
            currentNotes = block.exercise.notes
        )}
        resetActiveSetInputs(activeSet)
    }

    private fun resetActiveSetInputs(activeSet: Set?) {
        _localUiState.update {
            it.copy(
                currentWeight = activeSet?.weightKg ?: 0f,
                actualReps = activeSet?.minReps?.toString() ?: "",
                actualRir = activeSet?.minRir?.toString() ?: "",
                currentObservations = ""
            )
        }
    }

    fun endTraining() {
        viewModelScope.launch {
            calendarRepository.updateDayCompleted(daySlotId, true)
            stopTimer()
            _localUiState.update { it.copy(
                isTrainingActive = false,
                currentBlockIndex = 0,
                currentSetIndex = 0,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = false
            )}
        }
    }

    fun updateWeight(weight: Float) { _localUiState.update { it.copy(currentWeight = weight) } }
    fun updateNotes(notes: String) { _localUiState.update { it.copy(currentNotes = notes) } }
    fun updateActualReps(reps: String) { _localUiState.update { it.copy(actualReps = reps) } }
    fun updateActualRir(rir: String) { _localUiState.update { it.copy(actualRir = rir) } }
    fun updateObservations(obs: String) { _localUiState.update { it.copy(currentObservations = obs) } }
    fun updateRestMinutes(minutes: Int) { _localUiState.update { it.copy(restMinutes = minutes.coerceIn(1, 99)) } }

    fun startSeries() {
        _localUiState.update { it.copy(isSeriesRunning = true, isSeriesButtonEnabled = true) }
        timerManager.stopTimer(resetState = true)
    }

    // Interceptamos la parada para comprobar el peso
    fun requestStopSeries() {
        val state = _localUiState.value
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return
        val activeSet = block.sets.getOrNull(state.currentSetIndex) ?: return

        if (state.currentWeight != activeSet.weightKg) {
            _localUiState.update { it.copy(showWeightChangeConfirmation = true) }
        } else {
            executeStopSeries()
        }
    }

    fun confirmWeightChangeAndStopSeries() {
        _localUiState.update { it.copy(showWeightChangeConfirmation = false) }
        executeStopSeries()
    }

    fun cancelWeightChange() {
        _localUiState.update { it.copy(showWeightChangeConfirmation = false) }
    }

    private fun executeStopSeries() {
        val state = _localUiState.value
        val block = state.blocks.getOrNull(state.currentBlockIndex) ?: return
        val activeSet = block.sets.getOrNull(state.currentSetIndex) ?: return

        viewModelScope.launch {
            // 1. Guardar siempre el registro detallado de esta serie
            saveDetailedHistoryForCurrentSeries(block.exercise.id, activeSet)

            // 2. Si el peso cambió, sobrescribir la plantilla base
            if (state.currentWeight != activeSet.weightKg) {
                val updatedSet = activeSet.copy(weightKg = state.currentWeight)
                exerciseRepository.updateSet(updatedSet)
            }

            if (state.currentSeries < activeSet.series) {
                // Siguiente serie de la misma variante
                _localUiState.update { it.copy(
                    currentSeries = it.currentSeries + 1,
                    isSeriesButtonEnabled = false,
                    isSeriesRunning = false
                )}
                resetActiveSetInputs(activeSet)
                restartTimer()
            } else {
                // Variante terminada -> Guardamos el historial legacy por compatibilidad
                saveLegacySetHistory(block.exercise.id, activeSet, state.currentWeight)

                if (state.currentSetIndex + 1 < block.sets.size) {
                    // Siguiente variante
                    val nextSet = block.sets[state.currentSetIndex + 1]
                    _localUiState.update { it.copy(
                        currentSetIndex = it.currentSetIndex + 1,
                        currentSeries = 1,
                        isSeriesButtonEnabled = false,
                        isSeriesRunning = false
                    )}
                    resetActiveSetInputs(nextSet)
                    restartTimer()
                } else {
                    // Ejercicio terminado
                    _localUiState.update { it.copy(showFinishExercisePrompt = true) }
                }
            }
        }
    }

    fun confirmFinishExercise() {
        _localUiState.update { it.copy(showFinishExercisePrompt = false) }
        viewModelScope.launch { moveToNextExercise() }
    }

    fun dismissFinishExercise() {
        _localUiState.update { it.copy(showFinishExercisePrompt = false) }
    }

    fun finishExerciseManually() {
        viewModelScope.launch {
            val state = _localUiState.value
            val block = state.blocks.getOrNull(state.currentBlockIndex)
            val activeSet = block?.sets?.getOrNull(state.currentSetIndex)
            if (block != null && activeSet != null && state.isSeriesRunning) {
                // Si la forzó estando la serie iniciada, guarda ese registro incompleto
                saveDetailedHistoryForCurrentSeries(block.exercise.id, activeSet)
                saveLegacySetHistory(block.exercise.id, activeSet, state.currentWeight, state.currentSeries)
            }
            moveToNextExercise()
        }
    }

    private suspend fun moveToNextExercise() {
        val state = _localUiState.value

        // Guardar las notas del ejercicio siempre al salir del bloque
        val block = state.blocks.getOrNull(state.currentBlockIndex)
        if (block != null && state.currentNotes != block.exercise.notes) {
            exerciseRepository.updateExerciseNotes(block.exercise.id, state.currentNotes)
        }

        val nextIndex = state.currentBlockIndex + 1
        if (nextIndex < state.blocks.size) {
            stopTimer()
            _localUiState.update { it.copy(
                currentBlockIndex = nextIndex,
                currentSetIndex = 0,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = true
            )}
            loadCurrentBlockData()
        } else {
            endTraining()
        }
    }

    private suspend fun saveDetailedHistoryForCurrentSeries(exerciseId: String, activeSet: Set) {
        val state = _localUiState.value
        val history = DetailedHistory(
            id = UUID.randomUUID().toString(),
            exerciseId = exerciseId,
            setId = activeSet.id,
            daySlotId = daySlotId,
            timestamp = System.currentTimeMillis(),
            seriesNumber = state.currentSeries,
            reps = state.actualReps.toIntOrNull() ?: activeSet.minReps,
            weightKg = state.currentWeight,
            notes = state.currentObservations,
            rir = state.actualRir.toIntOrNull() // NUEVO: GUARDA EL RIR
        )
        exerciseRepository.insertDetailedHistory(history)
    }

    private suspend fun saveLegacySetHistory(exerciseId: String, set: Set, actualWeight: Float, completedSeries: Int = set.series) {
        exerciseRepository.insertHistory(
            ExerciseHistory(
                id = UUID.randomUUID().toString(),
                exerciseId = exerciseId,
                setId = set.id,
                timestamp = System.currentTimeMillis(),
                series = completedSeries,
                reps = _localUiState.value.actualReps.toIntOrNull() ?: set.minReps,
                weightKg = actualWeight
            )
        )
    }

    fun pauseTimer() { timerManager.pauseTimer() }
    fun resumeTimer() { timerManager.resumeTimer(TrainingConstants.TIMER_TYPE_TRAINING) }
    fun restartTimer() { timerManager.startTimer(_localUiState.value.restMinutes * 60, TrainingConstants.TIMER_TYPE_TRAINING) }
    fun stopTimer() { timerManager.stopTimer(); _localUiState.update { it.copy(isSeriesButtonEnabled = true) } }
    fun stopAlarm() { timerManager.stopAlarm() }
    fun onBackPressed() { if (_localUiState.value.isTrainingActive) _localUiState.update { it.copy(showExitConfirmation = true) } }
    fun confirmExit() { _localUiState.update { it.copy(showExitConfirmation = false, isTrainingActive = false) }; stopTimer() }
    fun dismissExitConfirmation() { _localUiState.update { it.copy(showExitConfirmation = false) } }
}