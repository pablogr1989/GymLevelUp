package com.gymlog.app.ui.screens.timer

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager

class TimerForegroundService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_TIMER_ALARM"
        const val ACTION_STOP = "ACTION_STOP_TIMER_ALARM"
        const val EXTRA_TIMER_TYPE = "EXTRA_TIMER_TYPE"
        const val TIMER_TYPE_STANDARD = "cronómetro"
        const val TIMER_TYPE_TRAINING = "temporizador de entrenamiento"
    }

    private var soundManager: TimerSoundManager? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        // Crear canal de notificación
        TimerNotificationHelper.createNotificationChannel(this)

        // Adquirir WakeLock para mantener la CPU activa
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GymLog::TimerWakeLock"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 minutos máximo
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val timerType = intent.getStringExtra(EXTRA_TIMER_TYPE) ?: TIMER_TYPE_STANDARD

                // CRÍTICO: Llamar a startForeground INMEDIATAMENTE
                val notification = TimerNotificationHelper.buildTimerFinishedNotification(this, timerType)
                startForeground(TimerNotificationHelper.NOTIFICATION_ID, notification)

                // DESPUÉS iniciar sonido y vibración
                soundManager = TimerSoundManager(this).apply {
                    startAlarm()
                }
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }

        return START_NOT_STICKY
    }

    private fun stopForegroundService() {
        soundManager?.stopAlarm()
        soundManager?.release()
        soundManager = null

        wakeLock?.release()
        wakeLock = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        soundManager?.release()
        wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}