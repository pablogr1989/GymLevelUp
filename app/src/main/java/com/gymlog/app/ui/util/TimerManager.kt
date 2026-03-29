package com.gymlog.app.util

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.gymlog.app.ui.screens.timer.AlarmReceiver
import com.gymlog.app.ui.screens.timer.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class TimerState(
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isAlarmRinging: Boolean = false,
    val totalDuration: Int = 0
)

@Singleton
class TimerManager @Inject constructor(
    private val application: Application
) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    fun startTimer(seconds: Int, timerType: String = TrainingConstants.TIMER_TYPE_STANDARD) {
        stopTimer(resetState = false)

        _timerState.update {
            it.copy(
                remainingSeconds = seconds,
                totalDuration = seconds,
                isRunning = true,
                isAlarmRinging = false
            )
        }

        // 1. Programar la Alarma del Sistema a prueba de HyperOS
        scheduleSystemAlarm(seconds, timerType)

        // 2. Iniciar el loop visual (Tick-Tock)
        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            val durationMs = seconds * 1000L

            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = ((durationMs - elapsed) / 1000L).coerceAtLeast(0)

                _timerState.update { it.copy(remainingSeconds = remaining.toInt()) }

                if (remaining > 0) delay(100L)
            }

            if (_timerState.value.remainingSeconds == 0) {
                handleTimerFinished()
            }
        }
    }

    fun pauseTimer() {
        cancelSystemAlarm()
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
    }

    fun resumeTimer(timerType: String = TrainingConstants.TIMER_TYPE_STANDARD) {
        val currentSeconds = _timerState.value.remainingSeconds
        if (currentSeconds > 0) {
            startTimer(currentSeconds, timerType)
        }
    }

    fun stopTimer(resetState: Boolean = true) {
        cancelSystemAlarm()
        stopForegroundService()
        timerJob?.cancel()

        if (resetState) {
            _timerState.update {
                it.copy(
                    remainingSeconds = 0,
                    isRunning = false,
                    isAlarmRinging = false
                )
            }
        }
    }

    fun stopAlarm() {
        stopForegroundService()
        _timerState.update { it.copy(isAlarmRinging = false) }
    }

    // --- Private Helpers ---

    private fun handleTimerFinished() {
        _timerState.update {
            it.copy(isRunning = false, isAlarmRinging = true, remainingSeconds = 0)
        }
    }

    private fun scheduleSystemAlarm(seconds: Int, timerType: String) {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(application, AlarmReceiver::class.java).apply {
            putExtra(TrainingConstants.EXTRA_TIMER_TYPE, timerType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            application,
            TrainingConstants.NOTIFICATION_ID_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (seconds * 1000L)

        try {
            val showIntent = PendingIntent.getActivity(
                application,
                0,
                application.packageManager.getLaunchIntentForPackage(application.packageName) ?: Intent(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        } catch (e: SecurityException) {
            // Fallback por si en algún momento Android restringe el permiso
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelSystemAlarm() {
        val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(application, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            TrainingConstants.NOTIFICATION_ID_TIMER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun stopForegroundService() {
        val stopIntent = Intent(application, TimerForegroundService::class.java).apply {
            action = TrainingConstants.ACTION_STOP
        }
        application.startService(stopIntent)
    }
}