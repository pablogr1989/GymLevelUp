package com.gymlog.app.ui.screens.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.gymlog.app.util.TrainingConstants

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val timerType = intent?.getStringExtra(TrainingConstants.EXTRA_TIMER_TYPE)
            ?: TrainingConstants.TIMER_TYPE_STANDARD

        val serviceIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TrainingConstants.ACTION_START
            putExtra(TrainingConstants.EXTRA_TIMER_TYPE, timerType)
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}