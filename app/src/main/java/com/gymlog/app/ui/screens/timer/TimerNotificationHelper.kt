package com.gymlog.app.ui.screens.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gymlog.app.R
import com.gymlog.app.util.TrainingConstants

object TimerNotificationHelper {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH

            // REFACTORIZACIÓN: Textos obtenidos de resources
            val name = context.getString(R.string.notification_channel_name)
            val desc = context.getString(R.string.notification_channel_description)

            val channel = NotificationChannel(
                TrainingConstants.NOTIFICATION_CHANNEL_ID,
                name,
                importance
            ).apply {
                description = desc
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildTimerFinishedNotification(context: Context, timerTypeRaw: String): android.app.Notification {
        val stopIntent = Intent(context, TimerForegroundService::class.java).apply {
            action = TrainingConstants.ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            context,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mapeo del tipo técnico a texto legible
        val typeReadable = when(timerTypeRaw) {
            TrainingConstants.TIMER_TYPE_TRAINING -> context.getString(R.string.timer_type_training)
            else -> context.getString(R.string.timer_type_standard)
        }

        return NotificationCompat.Builder(context, TrainingConstants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(context.getString(R.string.timer_notification_text, typeReadable))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Sugerencia: Reemplazar con R.drawable.ic_levelup_notification si creas uno
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_delete, context.getString(R.string.timer_notification_action_stop), stopPendingIntent)
            .build()
    }
}