package com.gymlog.app.ui.screens.timer

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _hours = MutableStateFlow("00")
    val hours = _hours.asStateFlow()

    private val _minutes = MutableStateFlow("00")
    val minutes = _minutes.asStateFlow()

    private val _seconds = MutableStateFlow("30")
    val seconds = _seconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _totalSeconds = MutableStateFlow(30)
    val totalSeconds = _totalSeconds.asStateFlow()

    private val _timeFinished = MutableStateFlow(false)
    val timeFinished = _timeFinished.asStateFlow()


    private val _saveHours = MutableStateFlow("00")
    val saveHours = _saveHours.asStateFlow()

    private val _saveMinutes = MutableStateFlow("00")
    val saveMinutes = _saveMinutes.asStateFlow()

    private val _saveSeconds = MutableStateFlow("30")
    val saveSeconds = _saveSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun updateHours(value: String) {
        if (value.length <= 2 && (value.isEmpty() || value.all { it.isDigit() })) {
            val intValue = value.toIntOrNull() ?: 0
            if (intValue in 0..23) {
                _hours.value = value.padStart(2, '0')
            }
        }
    }

    fun updateMinutes(value: String) {
        if (value.length <= 2 && (value.isEmpty() || value.all { it.isDigit() })) {
            val intValue = value.toIntOrNull() ?: 0
            if (intValue in 0..59) {
                _minutes.value = value.padStart(2, '0')
            }
        }
    }

    fun updateSeconds(value: String) {
        if (value.length <= 2 && (value.isEmpty() || value.all { it.isDigit() })) {
            val intValue = value.toIntOrNull() ?: 0
            if (intValue in 0..59) {
                _seconds.value = value.padStart(2, '0')
            }
        }
    }

    fun startTimer() {
        if (_isRunning.value) return

        _saveHours.value = _hours.value
        _saveMinutes.value = _minutes.value
        _saveSeconds.value = _seconds.value

        val h = _hours.value.toIntOrNull() ?: 0
        val m = _minutes.value.toIntOrNull() ?: 0
        val s = _seconds.value.toIntOrNull() ?: 0

        _totalSeconds.value = h * 3600 + m * 60 + s

        if (_totalSeconds.value <= 0) return

        _isRunning.value = true
        _timeFinished.value = false

        timerJob = viewModelScope.launch {
            while (_totalSeconds.value > 0 && _isRunning.value) {
                delay(1000)
                _totalSeconds.value -= 1

                val remaining = _totalSeconds.value
                _hours.value = (remaining / 3600).toString().padStart(2, '0')
                _minutes.value = ((remaining % 3600) / 60).toString().padStart(2, '0')
                _seconds.value = (remaining % 60).toString().padStart(2, '0')
            }

            if (_totalSeconds.value == 0) {
                _timeFinished.value = true
                _isRunning.value = false
                startAlarmService()
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        _hours.value = "00"
        _minutes.value = "00"
        _seconds.value = "30"
        _totalSeconds.value = 30
        _timeFinished.value = false
    }

    fun dismissTimeFinished() {
        _timeFinished.value = false
        _hours.value = _saveHours.value
        _minutes.value = _saveMinutes.value
        _seconds.value = _saveSeconds.value
        stopAlarmService()
    }

    private fun startAlarmService() {
        android.util.Log.d("TimerAlarm", "Programando alarma con AlarmManager")

        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java).apply {
            putExtra(TimerForegroundService.EXTRA_TIMER_TYPE, TimerForegroundService.TIMER_TYPE_STANDARD)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            application,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Alarma inmediata que funciona incluso en Doze
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 100, // +100ms para asegurar que dispara
            pendingIntent
        )
    }

    private fun stopAlarmService() {
        // Cancelar alarma pendiente
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Detener servicio si está corriendo
        val stopIntent = Intent(application, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_STOP
        }
        application.startService(stopIntent)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopAlarmService()
    }
}