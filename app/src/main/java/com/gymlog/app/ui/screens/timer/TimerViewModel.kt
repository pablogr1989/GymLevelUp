package com.gymlog.app.ui.screens.timer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymlog.app.util.InputValidator.validateInt
import com.gymlog.app.util.TrainingConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val hours: String = "00",
    val minutes: String = "00",
    val seconds: String = "30",
    val isRunning: Boolean = false,
    val totalSeconds: Int = 30,
    val timeFinished: Boolean = false,
    // Variables temporales para guardar la configuración al iniciar
    val savedHours: String = "00",
    val savedMinutes: String = "00",
    val savedSeconds: String = "30"
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    // Fuente de verdad única
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState = _uiState.asStateFlow()

    // --- Legacy StateFlows (Compatibilidad con UI existente) ---
    // Mapean directamente desde el UI State. Si el State cambia, estos emiten.
    val hours = _uiState.map { it.hours }
        .stateIn(viewModelScope, SharingStarted.Lazily, "00")

    val minutes = _uiState.map { it.minutes }
        .stateIn(viewModelScope, SharingStarted.Lazily, "00")

    val seconds = _uiState.map { it.seconds }
        .stateIn(viewModelScope, SharingStarted.Lazily, "30")

    val isRunning = _uiState.map { it.isRunning }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val timeFinished = _uiState.map { it.timeFinished }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)
    // -----------------------------------------------------------

    private var timerJob: Job? = null

    fun updateHours(value: String) {
        if (value.validateInt(0..23, 2)) {
            _uiState.update { it.copy(hours = value.padStart(2, '0')) }
        }
    }

    fun updateMinutes(value: String) {
        if (value.validateInt(0..59, 2)) {
            _uiState.update { it.copy(minutes = value.padStart(2, '0')) }
        }
    }

    fun updateSeconds(value: String) {
        if (value.validateInt(0..59, 2)) {
            _uiState.update { it.copy(seconds = value.padStart(2, '0')) }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        // Guardamos la configuración actual y calculamos total
        val currentState = _uiState.value
        val h = currentState.hours.toIntOrNull() ?: 0
        val m = currentState.minutes.toIntOrNull() ?: 0
        val s = currentState.seconds.toIntOrNull() ?: 0
        val total = h * 3600 + m * 60 + s

        if (total <= 0) return

        _uiState.update {
            it.copy(
                isRunning = true,
                timeFinished = false,
                totalSeconds = total,
                savedHours = it.hours,
                savedMinutes = it.minutes,
                savedSeconds = it.seconds
            )
        }

        timerJob = viewModelScope.launch {
            while (_uiState.value.totalSeconds > 0 && _uiState.value.isRunning) {
                delay(1000)

                _uiState.update { state ->
                    val newTotal = state.totalSeconds - 1
                    // Calculamos h/m/s restantes para mostrarlos
                    val rHours = (newTotal / 3600).toString().padStart(2, '0')
                    val rMinutes = ((newTotal % 3600) / 60).toString().padStart(2, '0')
                    val rSeconds = (newTotal % 60).toString().padStart(2, '0')

                    state.copy(
                        totalSeconds = newTotal,
                        hours = rHours,
                        minutes = rMinutes,
                        seconds = rSeconds
                    )
                }
            }

            if (_uiState.value.totalSeconds == 0) {
                _uiState.update { it.copy(timeFinished = true, isRunning = false) }
                startAlarmService()
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isRunning = false,
                timeFinished = false,
                hours = "00",
                minutes = "00",
                seconds = "30",
                totalSeconds = 30
            )
        }
    }

    fun dismissTimeFinished() {
        stopAlarmService()
        _uiState.update {
            it.copy(
                timeFinished = false,
                // Restauramos los valores originales guardados
                hours = it.savedHours,
                minutes = it.savedMinutes,
                seconds = it.savedSeconds
            )
        }
    }

    private fun startAlarmService() {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java).apply {
            putExtra(TrainingConstants.EXTRA_TIMER_TYPE, TrainingConstants.TIMER_TYPE_STANDARD)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            application,
            TrainingConstants.NOTIFICATION_ID_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // AlarmManager.setExactAndAllowWhileIdle asegura que suene incluso en modo Doze
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
            TrainingConstants.NOTIFICATION_ID_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        val stopIntent = Intent(application, TimerForegroundService::class.java).apply {
            action = TrainingConstants.ACTION_STOP
        }
        application.startService(stopIntent)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopAlarmService()
    }
}