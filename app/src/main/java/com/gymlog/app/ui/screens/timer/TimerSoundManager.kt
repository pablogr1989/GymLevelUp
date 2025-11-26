package com.gymlog.app.ui.screens.timer

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.*

class TimerSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var loopJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun startAlarm() {
        stopAlarm() // Detener cualquier alarma anterior

        loopJob = scope.launch {
            while (isActive) {
                playSound()
                vibratePhone()
                delay(3000) // 1s sonido + 2s pausa = 3s total
            }
        }
    }

    private fun playSound() {
        try {
            mediaPlayer?.release()
            val resId = context.resources.getIdentifier("alarm_sound", "raw", context.packageName)
            if (resId == 0) {
                android.util.Log.e("TimerSound", "No se encontró alarm_sound.mp3 en res/raw/")
                return
            }
            android.util.Log.d("TimerSound", "Reproduciendo sonido...")
            mediaPlayer = MediaPlayer.create(context, resId).apply {
                setOnCompletionListener {
                    it.release()
                }
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerSound", "Error reproduciendo sonido", e)
        }
    }

    private fun vibratePhone() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopAlarm() {
        loopJob?.cancel()
        loopJob = null

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
            }
        } catch (e: Exception) {
            android.util.Log.e("TimerSound", "Error deteniendo MediaPlayer", e)
        } finally {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                android.util.Log.e("TimerSound", "Error liberando MediaPlayer", e)
            }
            mediaPlayer = null
        }

        vibrator?.cancel()
    }

    fun release() {
        stopAlarm()
        scope.cancel()
    }
}