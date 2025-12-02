package com.gymlog.app.ui.screens.training

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.domain.model.DaySlot
import com.gymlog.app.domain.model.Exercise
import com.gymlog.app.domain.model.ExerciseHistory
import com.gymlog.app.domain.repository.CalendarRepository
import com.gymlog.app.domain.repository.ExerciseRepository
import com.gymlog.app.ui.screens.timer.AlarmReceiver
import com.gymlog.app.ui.screens.timer.TimerForegroundService
import com.gymlog.app.util.TrainingConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val isTrainingActive: Boolean = false,
    val currentExerciseIndex: Int = 0,
    val currentSeries: Int = 1,
    val currentWeight: Float = 0f,
    val currentNotes: String = "",
    val timerSeconds: Int = 0,
    val isTimerRunning: Boolean = false,
    val restMinutes: Int = 2,
    val isSeriesButtonEnabled: Boolean = false,
    val isSeriesRunning: Boolean = false
)

@HiltViewModel
class TrainingModeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val calendarRepository: CalendarRepository,
    private val exerciseRepository: ExerciseRepository,
    private val application: Application
) : ViewModel() {

    private val daySlotId: String = checkNotNull(savedStateHandle["daySlotId"])

    // Fuente de verdad única
    private val _uiState = MutableStateFlow(TrainingUiState())
    val uiState: StateFlow<TrainingUiState> = _uiState.asStateFlow()

    // --- Legacy StateFlows (Compatibilidad con UI existente) ---
    // Estos flujos se actualizan AUTOMÁTICAMENTE cuando cambia _uiState.
    // Usamos stateIn para convertirlos a StateFlow, que es lo que espera Compose.

    val daySlot = _uiState.map { it.daySlot }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val exercises = _uiState.map { it.exercises }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isTrainingActive = _uiState.map { it.isTrainingActive }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val currentExerciseIndex = _uiState.map { it.currentExerciseIndex }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val currentSeries = _uiState.map { it.currentSeries }
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val currentWeight = _uiState.map { it.currentWeight }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val currentNotes = _uiState.map { it.currentNotes }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    val timerSeconds = _uiState.map { it.timerSeconds }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val isTimerRunning = _uiState.map { it.isTimerRunning }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val restMinutes = _uiState.map { it.restMinutes }
        .stateIn(viewModelScope, SharingStarted.Lazily, 2)

    val isSeriesButtonEnabled = _uiState.map { it.isSeriesButtonEnabled }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isSeriesRunning = _uiState.map { it.isSeriesRunning }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    // -----------------------------------------------------------

    init {
        loadDaySlotAndExercises()
    }

    private fun loadDaySlotAndExercises() {
        viewModelScope.launch {
            val daySlotLoaded = calendarRepository.getDayById(daySlotId)

            var exercisesList: List<Exercise> = emptyList()
            var initialWeight = 0f
            var initialNotes = ""

            daySlotLoaded?.let { slot ->
                val exerciseIds = slot.selectedExerciseIds
                // Nota: Esto asume que selectedExerciseIds es una lista de Strings en el objeto DaySlot del dominio.
                // Si DaySlot.selectedExerciseIds es String en DB pero List<String> en Domain, esto funciona.
                // Verificando CalendarRepositoryImpl... sí, hace el parseo.

                val allExercises = exerciseRepository.getAllExercises().first()
                val exerciseMap = allExercises.associateBy { it.id }
                exercisesList = exerciseIds.mapNotNull { id -> exerciseMap[id] }

                exercisesList.firstOrNull()?.let { first ->
                    initialWeight = first.currentWeightKg
                    initialNotes = first.notes
                }
            }

            _uiState.update { it.copy(
                daySlot = daySlotLoaded,
                exercises = exercisesList,
                currentWeight = initialWeight,
                currentNotes = initialNotes
            )}
        }
    }

    fun startTraining() {
        _uiState.update { it.copy(
            isTrainingActive = true,
            currentExerciseIndex = 0,
            currentSeries = 1,
            isSeriesButtonEnabled = true
        )}

        _uiState.value.exercises.firstOrNull()?.let { exercise ->
            _uiState.update { it.copy(
                currentWeight = exercise.currentWeightKg,
                currentNotes = exercise.notes
            )}
        }
    }

    fun endTraining() {
        viewModelScope.launch {
            saveCurrentExerciseChanges()
            calendarRepository.updateDayCompleted(daySlotId, true)
            stopTimer()

            _uiState.update { it.copy(
                isTrainingActive = false,
                currentExerciseIndex = 0,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = false
            )}
        }
    }

    fun updateWeight(weight: Float) {
        _uiState.update { it.copy(currentWeight = weight) }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(currentNotes = notes) }
    }

    fun updateRestMinutes(minutes: Int) {
        val coerced = minutes.coerceIn(1, 99)
        _uiState.update { it.copy(restMinutes = coerced) }
    }

    // ============ FUNCIONES DE SERIES ============

    fun startSeries() {
        _uiState.update { it.copy(
            isSeriesRunning = true,
            isSeriesButtonEnabled = true,
            timerSeconds = it.restMinutes * 60,
            isTimerRunning = false
        )}
    }

    fun stopSeries(): SeriesAction {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex)
        val maxSeries = (currentExercise?.currentSeries ?: 0) + 1

        return if (state.currentSeries + 1 <= maxSeries) {
            _uiState.update { it.copy(
                currentSeries = it.currentSeries + 1,
                isSeriesButtonEnabled = false,
                isSeriesRunning = false
            )}
            startTimer()
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

    private suspend fun moveToNextExercise() {
        val nextIndex = _uiState.value.currentExerciseIndex + 1

        if (nextIndex < _uiState.value.exercises.size) {
            stopTimer()

            val nextExercise = _uiState.value.exercises.getOrNull(nextIndex)

            _uiState.update { it.copy(
                currentExerciseIndex = nextIndex,
                currentSeries = 1,
                isSeriesRunning = false,
                isSeriesButtonEnabled = false,
                currentWeight = nextExercise?.currentWeightKg ?: 0f,
                currentNotes = nextExercise?.notes ?: ""
            )}
        } else {
            endTraining()
        }
    }

    // ============ FUNCIONES DE TIMER ============

    private var timerJob: Job? = null

    private fun startTimer() {
        _uiState.update { it.copy(isTimerRunning = true) }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0 && _uiState.value.isTimerRunning) {
                delay(1000L)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }

            if (_uiState.value.timerSeconds == 0) {
                onTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isTimerRunning = false) }
        timerJob?.cancel()
    }

    fun resumeTimer() {
        if (_uiState.value.timerSeconds > 0) {
            startTimer()
        }
    }

    fun restartTimer() {
        _uiState.update { it.copy(timerSeconds = it.restMinutes * 60) }
        startTimer()
    }

    fun stopTimer() {
        timerJob?.cancel()
        stopAlarmService()

        // Habilitar botón de serie si estaba deshabilitado
        val shouldEnable = !_uiState.value.isSeriesButtonEnabled

        _uiState.update { it.copy(
            isTimerRunning = false,
            timerSeconds = 0,
            isSeriesButtonEnabled = if (shouldEnable) true else it.isSeriesButtonEnabled
        )}
    }

    private fun onTimerFinished() {
        _uiState.update { it.copy(
            isTimerRunning = false,
            isSeriesButtonEnabled = true
        )}
        startAlarmService()
    }

    private fun startAlarmService() {
        android.util.Log.d("TimerAlarm", "Programando alarma con AlarmManager (Training)")

        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java).apply {
            putExtra(TrainingConstants.EXTRA_TIMER_TYPE, TrainingConstants.TIMER_TYPE_TRAINING)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            application,
            TrainingConstants.NOTIFICATION_ID_TRAINING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 100,
            pendingIntent
        )
    }

    private fun stopAlarmService() {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            TrainingConstants.NOTIFICATION_ID_TRAINING,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val stopIntent = Intent(application, TimerForegroundService::class.java).apply {
            action = TrainingConstants.ACTION_STOP
        }
        application.startService(stopIntent)
    }

    // ============ GUARDAR CAMBIOS ============

    private suspend fun saveCurrentExerciseChanges() {
        val state = _uiState.value
        val currentExercise = state.exercises.getOrNull(state.currentExerciseIndex) ?: return

        val weightChanged = state.currentWeight != currentExercise.currentWeightKg
        val notesChanged = state.currentNotes != currentExercise.notes

        if (notesChanged) {
            exerciseRepository.updateExerciseNotes(
                exerciseId = currentExercise.id,
                notes = state.currentNotes
            )
        }

        if (weightChanged) {
            exerciseRepository.updateExerciseStats(
                exerciseId = currentExercise.id,
                series = currentExercise.currentSeries,
                reps = currentExercise.currentReps,
                weight = state.currentWeight
            )

            val history = ExerciseHistory(
                id = UUID.randomUUID().toString(),
                exerciseId = currentExercise.id,
                timestamp = System.currentTimeMillis(),
                series = currentExercise.currentSeries,
                reps = currentExercise.currentReps,
                weightKg = state.currentWeight
            )
            exerciseRepository.insertHistory(history)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopAlarmService()
    }
}