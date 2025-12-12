package com.gymlog.app.ui.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.util.InputValidator.validateInt
import com.gymlog.app.util.TimerManager
import com.gymlog.app.util.TrainingConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map // IMPORTANTE: Necesario para usar .map en el StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TimerUiState(
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "30",
    // Variables temporales para edición
    val isRunning: Boolean = false,
    val timeFinished: Boolean = false
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager
) : ViewModel() {

    // Estado local para la configuración (inputs)
    private val _configState = MutableStateFlow(TimerUiState())

    // Fusionamos estado local (configuración) con estado del Manager (ejecución)
    val uiState: StateFlow<TimerUiState> = combine(_configState, timerManager.timerState) { config, timer ->
        config.copy(
            isRunning = timer.isRunning,
            timeFinished = timer.isAlarmRinging,
            // Si está corriendo, mostramos el tiempo del manager. Si no, lo que el usuario editó.
            hours = if(timer.isRunning) formatHours(timer.remainingSeconds) else config.hours,
            minutes = if(timer.isRunning) formatMinutes(timer.remainingSeconds) else config.minutes,
            seconds = if(timer.isRunning) formatSeconds(timer.remainingSeconds) else config.seconds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TimerUiState()
    )

    // --- Legacy Accessors para la UI ---
    // Al importar 'kotlinx.coroutines.flow.map', esto funciona nativamente
    val hours = uiState.map { it.hours }.stateIn(viewModelScope, SharingStarted.Lazily, "00")
    val minutes = uiState.map { it.minutes }.stateIn(viewModelScope, SharingStarted.Lazily, "00")
    val seconds = uiState.map { it.seconds }.stateIn(viewModelScope, SharingStarted.Lazily, "30")
    val isRunning = uiState.map { it.isRunning }.stateIn(viewModelScope, SharingStarted.Lazily, false)
    val timeFinished = uiState.map { it.timeFinished }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun updateHours(value: String) {
        if (value.validateInt(0..23, 2)) {
            _configState.update { it.copy(hours = value.padStart(2, '0')) }
        }
    }

    fun updateMinutes(value: String) {
        if (value.validateInt(0..59, 2)) {
            _configState.update { it.copy(minutes = value.padStart(2, '0')) }
        }
    }

    fun updateSeconds(value: String) {
        if (value.validateInt(0..59, 2)) {
            _configState.update { it.copy(seconds = value.padStart(2, '0')) }
        }
    }

    fun startTimer() {
        val currentState = _configState.value
        val h = currentState.hours.toIntOrNull() ?: 0
        val m = currentState.minutes.toIntOrNull() ?: 0
        val s = currentState.seconds.toIntOrNull() ?: 0
        val total = h * 3600 + m * 60 + s

        if (total > 0) {
            timerManager.startTimer(total, TrainingConstants.TIMER_TYPE_STANDARD)
        }
    }

    fun pauseTimer() {
        timerManager.pauseTimer()
    }

    fun resetTimer() {
        timerManager.stopTimer(resetState = true)
    }

    fun dismissTimeFinished() {
        timerManager.stopAlarm()
        // No reseteamos _configState para que el usuario recuerde el último tiempo puesto
    }

    // Formateadores auxiliares
    private fun formatHours(totalSeconds: Int) = (totalSeconds / 3600).toString().padStart(2, '0')
    private fun formatMinutes(totalSeconds: Int) = ((totalSeconds % 3600) / 60).toString().padStart(2, '0')
    private fun formatSeconds(totalSeconds: Int) = (totalSeconds % 60).toString().padStart(2, '0')
}