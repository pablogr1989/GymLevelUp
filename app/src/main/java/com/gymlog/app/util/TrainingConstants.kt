package com.gymlog.app.util

object TrainingConstants {
    // Identificadores de Sistema (No requieren traducción)
    const val NOTIFICATION_CHANNEL_ID = "timer_channel"

    // IDs numéricos para notificaciones
    const val NOTIFICATION_ID_TIMER = 1001
    const val NOTIFICATION_ID_TRAINING = 1002

    // Intents y Acciones
    const val ACTION_START = "com.gymlog.app.ACTION_START_TIMER_ALARM" // Buena práctica: usar namespace completo
    const val ACTION_STOP = "com.gymlog.app.ACTION_STOP_TIMER_ALARM"

    // Extras
    const val EXTRA_TIMER_TYPE = "EXTRA_TIMER_TYPE"

    // Valores técnicos para lógica interna (no se muestran al usuario directamente sin procesar)
    const val TIMER_TYPE_STANDARD = "STANDARD"
    const val TIMER_TYPE_TRAINING = "TRAINING"
}