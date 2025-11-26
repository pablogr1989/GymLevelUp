package com.gymlog.app.ui.screens.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val timerType = intent?.getStringExtra(TimerForegroundService.EXTRA_TIMER_TYPE)
            ?: TimerForegroundService.TIMER_TYPE_STANDARD

        val serviceIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TimerForegroundService.ACTION_START
            putExtra(TimerForegroundService.EXTRA_TIMER_TYPE, timerType)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}