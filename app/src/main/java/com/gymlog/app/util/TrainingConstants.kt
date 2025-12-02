package com.gymlog.app.util

object TrainingConstants {
    // Notificaciones
    const val NOTIFICATION_CHANNEL_ID = "timer_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Timer Notifications"

    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_TRAINING = 1002

    // Intents y Acciones
    const val ACTION_START = "ACTION_START_TIMER_ALARM"
    const val ACTION_STOP = "ACTION_STOP_TIMER_ALARM"

    const val EXTRA_TIMER_TYPE = "EXTRA_TIMER_TYPE"
    const val TIMER_TYPE_STANDARD = "cronómetro"
    const val TIMER_TYPE_TRAINING = "temporizador de entrenamiento"
}