package com.gymlog.app.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.gymlog.app.util.RequestNotificationPermission

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    RequestNotificationPermission()
    val hours by viewModel.hours.collectAsState()
    val minutes by viewModel.minutes.collectAsState()
    val seconds by viewModel.seconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val timeFinished by viewModel.timeFinished.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Cronómetro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Display time
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberPicker(
                value = hours.toIntOrNull() ?: 0,
                onValueChange = { viewModel.updateHours(String.format("%02d", it)) },
                range = 0..23,
                enabled = !isRunning
            )
            Text(":", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            NumberPicker(
                value = minutes.toIntOrNull() ?: 0,
                onValueChange = { viewModel.updateMinutes(String.format("%02d", it)) },
                range = 0..59,
                enabled = !isRunning
            )
            Text(":", fontSize = 72.sp, fontWeight = FontWeight.Bold)
            NumberPicker(
                value = seconds.toIntOrNull() ?: 0,
                onValueChange = { viewModel.updateSeconds(String.format("%02d", it)) },
                range = 0..59,
                enabled = !isRunning
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isRunning) {
                FloatingActionButton(
                    onClick = viewModel::startTimer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Iniciar", modifier = Modifier.size(32.dp))
                }
            } else {
                FloatingActionButton(
                    onClick = viewModel::pauseTimer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.Pause, "Pausar", modifier = Modifier.size(32.dp))
                }
            }
            
            OutlinedButton(
                onClick = viewModel::resetTimer,
                modifier = Modifier.size(80.dp)
            ) {
                Icon(Icons.Default.Refresh, "Reiniciar", modifier = Modifier.size(32.dp))
            }
        }
    }
    
    if (timeFinished) {
        AlertDialog(
            onDismissRequest = viewModel::dismissTimeFinished,
            title = { Text("¡Tiempo terminado!") },
            text = { Text("El cronómetro ha llegado a 0.") },
            confirmButton = {
                TextButton(onClick = viewModel::dismissTimeFinished) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var localValue by remember(value) { mutableIntStateOf(value) }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(100.dp)
            .height(120.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectDragGestures(
                        onDragEnd = {
                            accumulatedDrag = 0f
                        }
                    ) { _, dragAmount ->
                        accumulatedDrag += dragAmount.y

                        // Cambiar cada 50px de arrastre
                        val threshold = 50f
                        if (accumulatedDrag <= -threshold) {
                            // Arrastrando hacia arriba = incrementar
                            val newValue = (localValue + 1).coerceIn(range)
                            onValueChange(newValue)
                            accumulatedDrag = 0f
                            localValue = newValue
                        } else if (accumulatedDrag >= threshold) {
                            // Arrastrando hacia abajo = decrementar
                            val newValue = (localValue - 1).coerceIn(range)
                            onValueChange(newValue)
                            accumulatedDrag = 0f
                            localValue = newValue
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Número anterior (arriba, semitransparente)
        Text(
            text = String.format("%02d", if (localValue > range.first) localValue - 1 else range.last),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.offset(y = (-40).dp),
            textAlign = TextAlign.Center
        )

        // Número actual (centro)
        Text(
            text = String.format("%02d", value),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        // Número siguiente (abajo, semitransparente)
        Text(
            text = String.format("%02d", if (localValue < range.last) localValue + 1 else range.first),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.offset(y = 40.dp),
            textAlign = TextAlign.Center
        )

        // Líneas indicadoras (opcional)
        if (enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Gray,
                    start = Offset(0f, size.height * 0.3f),
                    end = Offset(size.width, size.height * 0.3f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = androidx.compose.ui.graphics.Color.Gray,
                    start = Offset(0f, size.height * 0.7f),
                    end = Offset(size.width, size.height * 0.7f),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}
